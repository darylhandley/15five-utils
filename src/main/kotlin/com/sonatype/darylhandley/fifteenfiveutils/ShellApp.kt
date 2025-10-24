package com.sonatype.darylhandley.fifteenfiveutils

import com.sonatype.darylhandley.fifteenfiveutils.commands.Command
import com.sonatype.darylhandley.fifteenfiveutils.commands.objectives.*
import com.sonatype.darylhandley.fifteenfiveutils.commands.teams.*
import com.sonatype.darylhandley.fifteenfiveutils.commands.useralias.*
import com.sonatype.darylhandley.fifteenfiveutils.commands.users.*
import com.sonatype.darylhandley.fifteenfiveutils.service.AliasService
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveCloneService
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveService
import com.sonatype.darylhandley.fifteenfiveutils.service.TeamsService
import com.sonatype.darylhandley.fifteenfiveutils.service.UserService
import com.sonatype.darylhandley.fifteenfiveutils.ui.Colors
import com.sonatype.darylhandley.fifteenfiveutils.ui.ShellUI
import com.sonatype.darylhandley.fifteenfiveutils.util.ConfigLoader
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import java.io.File

class ShellApp {
    private lateinit var userService: UserService
    private lateinit var objectiveService: ObjectiveService
    private lateinit var objectiveCloneService: ObjectiveCloneService
    private lateinit var aliasService: AliasService
    private lateinit var teamsService: TeamsService
    private lateinit var lineReader: LineReader
    private lateinit var terminal: Terminal
    private lateinit var ui: ShellUI
    private lateinit var commands: Map<String, Command>

    private fun tokenizeInput(input: String): List<String> {
        return input.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    }

    fun run() {
        println("${Colors.BOLD}${Colors.CYAN}15Five Utils Shell${Colors.RESET} - Type '${Colors.YELLOW}help${Colors.RESET}' or '${Colors.YELLOW}?${Colors.RESET}' for commands or '${Colors.YELLOW}quit/exit/q${Colors.RESET}' to exit")
        println("${Colors.DIM}${"─".repeat(60)}${Colors.RESET}")

        val sessionId = try {
            ConfigLoader.getSessionId()
        } catch (e: Exception) {
            println("${Colors.RED}Configuration error: ${e.message}${Colors.RESET}")
            return
        }

        val csrfToken = try {
            ConfigLoader.getCsrfMiddlewareToken()
        } catch (e: Exception) {
            println("${Colors.RED}Configuration error: ${e.message}${Colors.RESET}")
            return
        }

        // Initialize services
        userService = UserService(sessionId)
        objectiveService = ObjectiveService(sessionId, csrfToken)
        objectiveCloneService = ObjectiveCloneService(sessionId)
        aliasService = AliasService(userService)
        teamsService = TeamsService(userService, aliasService)

        // Set up circular reference for alias protection
        aliasService.setTeamsService(teamsService)

        // Set up JLine3 terminal and line reader
        terminal = TerminalBuilder.builder()
            .system(true)
            .build()

        // Set up command history file
        val homeDir = System.getProperty("user.home")
        val configDir = File(homeDir, ".15fiveutils")
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
        val historyFile = File(configDir, "history")

        // Set up tab completion for available commands
        val completer = StringsCompleter(
            "help",
            "?",
            "quit",
            "exit",
            "q",
            "users list",
            "users refresh",
            "objectives list",
            "objectives list -verbose",
            "objectives list -v",
            "objectives listbyuser",
            "objectives listbyuser -verbose",
            "objectives listbyuser -v",
            "objectives listbyteam",
            "objectives listbyteam -verbose",
            "objectives listbyteam -v",
            "objectives get",
            "objectives clone",
            "objectives teamclone",
            "objectives updateprogress",
            "objectives updatechildprogress",
            "useralias create",
            "useralias createbysearch",
            "useralias list",
            "useralias delete",
            "teams create",
            "teams get",
            "teams add",
            "teams remove",
            "teams delete",
            "teams list"
        )

        lineReader = LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(completer)
            .history(DefaultHistory())
            .variable(LineReader.HISTORY_FILE, historyFile.absolutePath)
            .build()

        // Initialize UI helper
        ui = ShellUI(terminal, lineReader)

        // Initialize command registry
        commands = initializeCommands()

        var running = true

        while (running) {
            try {
                val prompt = "${Colors.BLUE}15five${Colors.RESET}${Colors.DIM}>${Colors.RESET} "
                val input = lineReader.readLine(prompt).trim()

                if (input.isEmpty()) continue

                val tokens = tokenizeInput(input)
                val commandKey = buildCommandKey(tokens)

                when {
                    tokens.firstOrNull()?.lowercase() in listOf("help", "?") -> handleHelpCommand()
                    tokens.firstOrNull()?.lowercase() in listOf("quit", "exit", "q") -> {
                        println("${Colors.YELLOW}Goodbye!${Colors.RESET}")
                        running = false
                    }
                    commandKey != null && commands.containsKey(commandKey) -> {
                        try {
                            commands[commandKey]!!.execute(tokens)
                        } catch (e: Exception) {
                            ui.printError("Error executing command: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                    else -> {
                        println("${Colors.RED}Unknown command: $input. Type '${Colors.YELLOW}help${Colors.RESET}' or '${Colors.YELLOW}?${Colors.RESET}' for available commands.${Colors.RESET}")
                    }
                }

                if (running) {
                    ui.printSeparator()
                }
            } catch (e: org.jline.reader.UserInterruptException) {
                // Handle Ctrl+C gracefully
                e.printStackTrace()
                println("\n${Colors.YELLOW}Goodbye!${Colors.RESET}")
                break
            } catch (e: org.jline.reader.EndOfFileException) {
                // Handle EOF (Ctrl+D) gracefully
                e.printStackTrace()
                println("\n${Colors.YELLOW}Goodbye!${Colors.RESET}")
                break
            }
        }

        terminal.close()

        // added for fast shutdown since okhttp client wants to hold onto connections
        System.exit(0)
    }

    private fun initializeCommands(): Map<String, Command> {
        return mapOf(
            // Users commands
            "users list" to UsersListCommand(userService, ui),
            "users refresh" to UsersRefreshCommand(userService, ui),

            // Objectives commands
            "objectives list" to ObjectivesListCommand(objectiveService, ui),
            "objectives listbyuser" to ObjectivesListByUserCommand(objectiveService, aliasService, ui),
            "objectives listbyteam" to ObjectivesListByTeamCommand(objectiveService, teamsService, userService, aliasService, ui),
            "objectives get" to ObjectivesGetCommand(objectiveService, ui),
            "objectives clone" to ObjectivesCloneCommand(objectiveService, objectiveCloneService, userService, aliasService, ui),
            "objectives teamclone" to ObjectivesTeamCloneCommand(objectiveService, objectiveCloneService, teamsService, userService, aliasService, ui),
            "objectives updateprogress" to ObjectivesUpdateProgressCommand(objectiveService, ui),
            "objectives updatechildprogress" to ObjectivesUpdateChildProgressCommand(objectiveService, ui),

            // User alias commands
            "useralias create" to UserAliasCreateCommand(aliasService, ui),
            "useralias createbysearch" to UserAliasCreateBySearchCommand(aliasService, ui),
            "useralias list" to UserAliasListCommand(aliasService, ui),
            "useralias delete" to UserAliasDeleteCommand(aliasService, ui),

            // Teams commands
            "teams create" to TeamsCreateCommand(teamsService, ui),
            "teams get" to TeamsGetCommand(teamsService, ui),
            "teams add" to TeamsAddCommand(teamsService, ui),
            "teams remove" to TeamsRemoveCommand(teamsService, ui),
            "teams delete" to TeamsDeleteCommand(teamsService, ui),
            "teams list" to TeamsListCommand(teamsService, ui)
        )
    }

    private fun buildCommandKey(tokens: List<String>): String? {
        if (tokens.isEmpty()) return null

        // Try two-word commands first (e.g., "users list")
        if (tokens.size >= 2) {
            val twoWordKey = "${tokens[0].lowercase()} ${tokens[1].lowercase()}"
            if (commands.containsKey(twoWordKey)) {
                return twoWordKey
            }
        }

        // Try one-word commands
        val oneWordKey = tokens[0].lowercase()
        if (commands.containsKey(oneWordKey)) {
            return oneWordKey
        }

        return null
    }

    private fun handleHelpCommand() {
        println("${Colors.BOLD}${Colors.CYAN}General:${Colors.RESET}")
        println("  ${Colors.YELLOW}help/?${Colors.RESET}${" ".repeat(37)} - Show this help")
        println("  ${Colors.YELLOW}quit/exit/q${Colors.RESET}${" ".repeat(32)} - Exit the shell")
        println("${Colors.BOLD}${Colors.CYAN}Users:${Colors.RESET}")
        println("  ${Colors.YELLOW}users list${Colors.RESET}${" ".repeat(33)} - List all users")
        println("  ${Colors.YELLOW}users list${Colors.RESET} ${Colors.DIM}<search>${Colors.RESET}${" ".repeat(25)} - Search for users by name")
        println("  ${Colors.YELLOW}users refresh${Colors.RESET}${" ".repeat(30)} - Refresh user cache from API")
        println()
        println("${Colors.BOLD}${Colors.CYAN}Objectives:${Colors.RESET}")
        println("  ${Colors.YELLOW}objectives list${Colors.RESET} ${Colors.DIM}[-v]${Colors.RESET}${" ".repeat(25)} - List top 100 objectives")
        println("  ${Colors.YELLOW}objectives list${Colors.RESET} ${Colors.DIM}<limit> [-v]${Colors.RESET}${" ".repeat(17)} - List objectives (custom limit)")
        println("  ${Colors.YELLOW}objectives listbyuser${Colors.RESET} ${Colors.DIM}<id> [-v]${Colors.RESET}${" ".repeat(14)} - List objectives for user ID or alias")
        println("  ${Colors.YELLOW}objectives listbyteam${Colors.RESET} ${Colors.DIM}<team> [-v]${Colors.RESET}${" ".repeat(12)} - List objectives for team members")
        println("    ${Colors.DIM}Use -verbose or -v for detailed view instead of compact table${Colors.RESET}")
        println("  ${Colors.YELLOW}objectives get${Colors.RESET} ${Colors.DIM}<id>${Colors.RESET}${" ".repeat(26)} - Get single objective by ID")
        println("  ${Colors.YELLOW}objectives clone${Colors.RESET} ${Colors.DIM}<id> <user>${Colors.RESET}${" ".repeat(17)} - Clone objective to another user")
        println("  ${Colors.YELLOW}objectives teamclone${Colors.RESET} ${Colors.DIM}<id> <team>${Colors.RESET}${" ".repeat(13)} - Clone objective to all team members")
        println("  ${Colors.YELLOW}objectives updateprogress${Colors.RESET} ${Colors.DIM}<id>${Colors.RESET}${" ".repeat(14)} - Copy key result progress from parent")
        println("  ${Colors.YELLOW}objectives updatechildprogress${Colors.RESET} ${Colors.DIM}<id>${Colors.RESET}${" ".repeat(9)} - Update all children to match parent progress")
        println()
        println("${Colors.BOLD}${Colors.CYAN}User Aliases:${Colors.RESET}")
        println("  ${Colors.YELLOW}useralias create${Colors.RESET} ${Colors.DIM}<alias> <userid>${Colors.RESET}${" ".repeat(12)} - Create user alias")
        println("  ${Colors.YELLOW}useralias createbysearch${Colors.RESET} ${Colors.DIM}<alias> <search>${Colors.RESET}${" ".repeat(4)} - Create alias by searching for user")
        println("  ${Colors.YELLOW}useralias list${Colors.RESET}${" ".repeat(29)} - List all user aliases")
        println("  ${Colors.YELLOW}useralias delete${Colors.RESET} ${Colors.DIM}<alias>${Colors.RESET}${" ".repeat(22)} - Delete user alias")
        println()
        println("${Colors.BOLD}${Colors.CYAN}Teams:${Colors.RESET}")
        println("  ${Colors.YELLOW}teams create${Colors.RESET} ${Colors.DIM}<name>${Colors.RESET}${" ".repeat(27)} - Create a new team")
        println("  ${Colors.YELLOW}teams get${Colors.RESET} ${Colors.DIM}<name>${Colors.RESET}${" ".repeat(30)} - Show team members with details")
        println("  ${Colors.YELLOW}teams add${Colors.RESET} ${Colors.DIM}<team> <alias>${Colors.RESET}${" ".repeat(22)} - Add alias to team")
        println("  ${Colors.YELLOW}teams remove${Colors.RESET} ${Colors.DIM}<team> <alias>${Colors.RESET}${" ".repeat(19)} - Remove alias from team")
        println("  ${Colors.YELLOW}teams delete${Colors.RESET} ${Colors.DIM}<name>${Colors.RESET}${" ".repeat(27)} - Delete team (keeps aliases)")
        println("  ${Colors.YELLOW}teams list${Colors.RESET}${" ".repeat(33)} - List all teams")
        println()
    }
}

fun main() {
    ShellApp().run()
}