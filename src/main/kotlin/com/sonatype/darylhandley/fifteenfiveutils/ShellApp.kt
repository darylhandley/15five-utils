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
        printWelcomeBanner()

        if (!initialize()) {
            return
        }

        runShell()

        cleanup()
    }

    private fun printWelcomeBanner() {
        println("${Colors.BOLD}${Colors.CYAN}15Five Utils Shell${Colors.RESET} - Type '${Colors.YELLOW}help${Colors.RESET}' or '${Colors.YELLOW}?${Colors.RESET}' for commands or '${Colors.YELLOW}quit/exit/q${Colors.RESET}' to exit")
        println("${Colors.DIM}${"─".repeat(60)}${Colors.RESET}")
    }

    private fun initialize(): Boolean {
        // Load configuration
        val sessionId = try {
            ConfigLoader.getSessionId()
        } catch (e: Exception) {
            println("${Colors.RED}Configuration error: ${e.message}${Colors.RESET}")
            return false
        }

        val csrfToken = try {
            ConfigLoader.getCsrfMiddlewareToken()
        } catch (e: Exception) {
            println("${Colors.RED}Configuration error: ${e.message}${Colors.RESET}")
            return false
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

        return true
    }

    private fun runShell() {
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
    }

    private fun cleanup() {
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

    private data class HelpEntry(
        val command: String,
        val params: String = "",
        val description: String,
        val indent: Int = 2
    )

    private fun handleHelpCommand() {
        val sections = mapOf(
            "General" to listOf(
                HelpEntry("help/?", description = "Show this help"),
                HelpEntry("quit/exit/q", description = "Exit the shell")
            ),
            "Users" to listOf(
                HelpEntry("users list", description = "List all users"),
                HelpEntry("users list", "<search>", "Search for users by name"),
                HelpEntry("users refresh", description = "Refresh user cache from API")
            ),
            "Objectives" to listOf(
                HelpEntry("objectives list", "[-v]", "List top 100 objectives"),
                HelpEntry("objectives list", "<limit> [-v]", "List objectives (custom limit)"),
                HelpEntry("objectives listbyuser", "<id> [-v]", "List objectives for user ID or alias"),
                HelpEntry("objectives listbyteam", "<team> [-v]", "List objectives for team members"),
                HelpEntry("", "", "Use -verbose or -v for detailed view instead of compact table", indent = 4),
                HelpEntry("objectives get", "<id>", "Get single objective by ID"),
                HelpEntry("objectives clone", "<id> <user>", "Clone objective to another user"),
                HelpEntry("objectives teamclone", "<id> <team>", "Clone objective to all team members"),
                HelpEntry("objectives updateprogress", "<id>", "Copy key result progress from parent"),
                HelpEntry("objectives updatechildprogress", "<id>", "Update all children to match parent progress")
            ),
            "User Aliases" to listOf(
                HelpEntry("useralias create", "<alias> <userid>", "Create user alias"),
                HelpEntry("useralias createbysearch", "<alias> <search>", "Create alias by searching for user"),
                HelpEntry("useralias list", description = "List all user aliases"),
                HelpEntry("useralias delete", "<alias>", "Delete user alias")
            ),
            "Teams" to listOf(
                HelpEntry("teams create", "<name>", "Create a new team"),
                HelpEntry("teams get", "<name>", "Show team members with details"),
                HelpEntry("teams add", "<team> <alias>", "Add alias to team"),
                HelpEntry("teams remove", "<team> <alias>", "Remove alias from team"),
                HelpEntry("teams delete", "<name>", "Delete team (keeps aliases)"),
                HelpEntry("teams list", description = "List all teams")
            )
        )

        printHelpSections(sections)
    }

    private fun printHelpSections(sections: Map<String, List<HelpEntry>>) {
        // Calculate max width for alignment (excluding indented notes)
        val maxWidth = sections.values.flatten()
            .filter { it.indent == 2 }  // Only regular commands
            .maxOf {
                it.command.length + it.params.length +
                        (if (it.params.isNotEmpty()) 1 else 0)  // space between command and params
            }

        sections.forEach { (section, entries) ->
            println("${Colors.BOLD}${Colors.CYAN}$section:${Colors.RESET}")
            entries.forEach { entry ->
                if (entry.command.isEmpty()) {
                    // Sub-note/indented line
                    println("${" ".repeat(entry.indent)}${Colors.DIM}${entry.description}${Colors.RESET}")
                } else {
                    val commandPart = "${Colors.YELLOW}${entry.command}${Colors.RESET}"
                    val paramsPart = if (entry.params.isNotEmpty()) " ${Colors.DIM}${entry.params}${Colors.RESET}" else ""
                    val fullCommand = entry.command + if (entry.params.isNotEmpty()) " ${entry.params}" else ""
                    val padding = " ".repeat(maxWidth - fullCommand.length + 3)
                    println("${" ".repeat(entry.indent)}$commandPart$paramsPart$padding - ${entry.description}")
                }
            }
            println()
        }
    }
}

fun main() {
    ShellApp().run()
}