package com.sonatype.darylhandley.fifteenfiveutils

import com.sonatype.darylhandley.fifteenfiveutils.service.AliasService
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveCloneService
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveService
import com.sonatype.darylhandley.fifteenfiveutils.service.TeamsService
import com.sonatype.darylhandley.fifteenfiveutils.service.UserService
import com.sonatype.darylhandley.fifteenfiveutils.util.ConfigLoader
import com.sonatype.darylhandley.fifteenfiveutils.util.TableFormatter
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import java.io.File

object Colors {
    const val RESET = "\u001B[0m"
    const val BLUE = "\u001B[34m"
    const val GREEN = "\u001B[32m"
    const val YELLOW = "\u001B[33m"
    const val RED = "\u001B[31m"
    const val CYAN = "\u001B[36m"
    const val BOLD = "\u001B[1m"
    const val DIM = "\u001B[2m"
}

class ShellApp {
    private lateinit var userService: UserService
    private lateinit var objectiveService: ObjectiveService
    private lateinit var objectiveCloneService: ObjectiveCloneService
    private lateinit var aliasService: AliasService
    private lateinit var teamsService: TeamsService
    private lateinit var lineReader: LineReader
    private lateinit var terminal: Terminal
    private fun tokenizeInput(input: String): List<String> {
        return input.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    }

    private fun getTerminalWidth(): Int {
        return try {
            terminal.width
        } catch (e: Exception) {
            120 // Fallback width if detection fails
        }
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

        userService = UserService(sessionId)
        objectiveService = ObjectiveService(sessionId)
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
            "objectives list -compact",
            "objectives list -c",
            "objectives listbyuser",
            "objectives listbyuser -compact", 
            "objectives listbyuser -c",
            "objectives get",
            "objectives clone",
            "useralias create",
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

        var running = true

        while (running) {
            try {
                val prompt = "${Colors.BLUE}15five${Colors.RESET}${Colors.DIM}>${Colors.RESET} "
                val input = lineReader.readLine(prompt).trim()

                if (input.isEmpty()) continue

                val tokens = tokenizeInput(input)
                when (tokens.firstOrNull()?.lowercase()) {
                    "users" -> handleUsersCommand(tokens)
                    "objectives" -> handleObjectivesCommand(tokens)
                    "useralias" -> handleUserAliasCommand(tokens)
                    "teams" -> handleTeamsCommand(tokens)
                    "help", "?" -> handleHelpCommand()
                    "quit", "exit", "q" -> {
                        println("${Colors.YELLOW}Goodbye!${Colors.RESET}")
                        running = false
                    }

                    else -> {
                        println("${Colors.RED}Unknown command: $input. Type '${Colors.YELLOW}help${Colors.RESET}' or '${Colors.YELLOW}?${Colors.RESET}' for available commands.${Colors.RESET}")
                    }
                }

                if (running) {
                    println("${Colors.DIM}${"─".repeat(30)}${Colors.RESET}")
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

    private fun handleUsersCommand(tokens: List<String>) {
        if (tokens.size == 2 && tokens[1].lowercase() == "list") {
            try {
                val users = userService.listAllUsers()
                println("${Colors.GREEN}${TableFormatter.formatUsersTable(users)}${Colors.RESET}")
            } catch (e: Exception) {
                println("${Colors.RED}Error fetching users: ${e.message}${Colors.RESET}")
                e.printStackTrace()
            }
        } else if (tokens.size == 2 && tokens[1].lowercase() == "refresh") {
            try {
                val result = userService.refreshUserCache()
                println("${Colors.GREEN}$result${Colors.RESET}")
            } catch (e: Exception) {
                println("${Colors.RED}Error refreshing user cache: ${e.message}${Colors.RESET}")
                e.printStackTrace()
            }
        } else if (tokens.size > 2 && tokens[1].lowercase() == "list") {
            try {
                val searchTerm = tokens.drop(2).joinToString(" ")
                val users = userService.searchUsers(searchTerm)
                println("${Colors.GREEN}${TableFormatter.formatUsersTable(users)}${Colors.RESET}")
            } catch (e: Exception) {
                println("${Colors.RED}Error searching users: ${e.message}${Colors.RESET}")
                e.printStackTrace()
            }
        } else {
            println("${Colors.RED}Usage: users <list|refresh> [search_term]${Colors.RESET}")
        }
    }

    private fun handleObjectivesCommand(tokens: List<String>) {
        if (tokens.size < 2) {
            println("${Colors.RED}Usage: objectives <list|listbyuser|get|clone> [args...]${Colors.RESET}")
            return
        }

        when (tokens[1].lowercase()) {
            "list" -> handleObjectivesListCommand(tokens)
            "listbyuser" -> handleObjectivesListByUserCommand(tokens)
            "get" -> handleObjectivesGetCommand(tokens)
            "clone" -> handleObjectivesCloneCommand(tokens)
            else -> {
                println("${Colors.RED}Unknown objectives subcommand: ${tokens[1]}${Colors.RESET}")
                println("${Colors.RED}Usage: objectives <list|listbyuser|get|clone> [args...]${Colors.RESET}")
            }
        }
    }

    private fun handleObjectivesListCommand(tokens: List<String>) {
        // Check for compact flag
        val isCompact = tokens.lastOrNull()?.lowercase() in listOf("-compact", "-c")
        val effectiveTokens = if (isCompact) tokens.dropLast(1) else tokens
        
        if (effectiveTokens.size == 2) {
            try {
                val objectives = objectiveService.listObjectives(100)
                val formatted = if (isCompact) {
                    TableFormatter.formatObjectivesCompactTable(objectives, getTerminalWidth())
                } else {
                    TableFormatter.formatObjectivesList(objectives)
                }
                println("${Colors.GREEN}$formatted${Colors.RESET}")
            } catch (e: Exception) {
                println("${Colors.RED}Error fetching objectives: ${e.message}${Colors.RESET}")
                e.printStackTrace()
            }
        } else if (effectiveTokens.size == 3) {
            try {
                val limit = effectiveTokens[2].toInt()
                val objectives = objectiveService.listObjectives(limit)
                val formatted = if (isCompact) {
                    TableFormatter.formatObjectivesCompactTable(objectives, getTerminalWidth())
                } else {
                    TableFormatter.formatObjectivesList(objectives)
                }
                println("${Colors.GREEN}$formatted${Colors.RESET}")
            } catch (e: NumberFormatException) {
                println("${Colors.RED}Invalid number: ${effectiveTokens[2]}${Colors.RESET}")
            } catch (e: Exception) {
                println("${Colors.RED}Error fetching objectives: ${e.message}${Colors.RESET}")
                e.printStackTrace()
            }
        } else {
            println("${Colors.RED}Usage: objectives list [limit] [-compact|-c]${Colors.RESET}")
        }
    }

    private fun handleObjectivesListByUserCommand(tokens: List<String>) {
        // Check for compact flag
        val isCompact = tokens.lastOrNull()?.lowercase() in listOf("-compact", "-c")
        val effectiveTokens = if (isCompact) tokens.dropLast(1) else tokens
        
        if (effectiveTokens.size != 3) {
            println("${Colors.RED}Usage: objectives listbyuser <userid or alias> [-compact|-c]${Colors.RESET}")
        } else {
            val userIdentifier = effectiveTokens[2]
            try {
                val userId = aliasService.resolveUserIdentifier(userIdentifier)
                if (userId == null) {
                    println("${Colors.RED}Unknown user identifier: $userIdentifier${Colors.RESET}")
                } else {
                    val objectives = objectiveService.listObjectivesByUser(userId)
                    val formatted = if (isCompact) {
                        TableFormatter.formatObjectivesCompactTable(objectives, getTerminalWidth())
                    } else {
                        TableFormatter.formatObjectivesList(objectives)
                    }
                    println("${Colors.GREEN}$formatted${Colors.RESET}")
                }
            } catch (e: Exception) {
                println("${Colors.RED}Error fetching objectives: ${e.message}${Colors.RESET}")
                e.printStackTrace()
            }
        }
    }

    private fun handleObjectivesGetCommand(tokens: List<String>) {
        if (tokens.size != 3) {
            println("${Colors.RED}Usage: objectives get <objective_id>${Colors.RESET}")
        } else {
            try {
                val objectiveId = tokens[2].toInt()
                val objective = objectiveService.getObjective(objectiveId)
                println("${Colors.GREEN}${TableFormatter.formatSingleObjective(objective)}${Colors.RESET}")
            } catch (e: NumberFormatException) {
                println("${Colors.RED}Invalid objective ID: ${tokens[2]}${Colors.RESET}")
            } catch (e: Exception) {
                println("${Colors.RED}Error fetching objective: ${e.message}${Colors.RESET}")
                e.printStackTrace()
            }
        }
    }

    private fun handleObjectivesCloneCommand(tokens: List<String>) {
        if (tokens.size != 4) {
            println("${Colors.RED}Usage: objectives clone <objective_id> <target_user_id_or_alias>${Colors.RESET}")
        } else {
            try {
                val objectiveId = tokens[2].toInt()
                val targetUserIdentifier = tokens[3]

                // Resolve target user
                val targetUserId = aliasService.resolveUserIdentifier(targetUserIdentifier)
                if (targetUserId == null) {
                    println("${Colors.RED}Unknown user identifier: $targetUserIdentifier${Colors.RESET}")
                    return
                }

                // Get source objective
                val sourceObjective = objectiveService.getObjective(objectiveId)

                // Get target user name for display
                val targetUserName = userService.listAllUsers()
                    .find { it.id == targetUserId }?.fullName ?: "User ID $targetUserId"

                // Check for duplicate objectives
                val existingObjectives = objectiveService.listObjectivesByUser(targetUserId)
                val duplicates = existingObjectives.filter {
                    it.description.equals(sourceObjective.description, ignoreCase = true)
                }

                if (duplicates.isNotEmpty()) {
                    println("${Colors.YELLOW}⚠️  WARNING: Found ${duplicates.size} existing objective(s) with matching title:${Colors.RESET}")
                    duplicates.forEach { duplicate ->
                        println("${Colors.YELLOW}   • ID ${duplicate.id}: \"${duplicate.description}\" (${duplicate.getFormattedStartDate()} → ${duplicate.getFormattedEndDate()})${Colors.RESET}")
                    }
                    print("${Colors.YELLOW}Continue with clone anyway? (y/N): ${Colors.RESET}")
                    val duplicateConfirmation = lineReader.readLine()
                    if (duplicateConfirmation.lowercase() != "y" && duplicateConfirmation.lowercase() != "yes") {
                        println("${Colors.YELLOW}Clone cancelled due to duplicate objectives.${Colors.RESET}")
                        return
                    }
                    println()
                }

                // Show preview and get confirmation
                val preview = objectiveCloneService.buildClonePreview(sourceObjective, targetUserName, targetUserId)
                print("${Colors.CYAN}$preview${Colors.RESET}")

                val confirmation = lineReader.readLine()
                if (confirmation.lowercase() == "y" || confirmation.lowercase() == "yes") {
                    println("${Colors.YELLOW}Cloning objective...${Colors.RESET}")
                    val clonedObjectiveId = objectiveCloneService.cloneObjective(sourceObjective, targetUserId)
                    println("${Colors.GREEN}✅ Objective cloned successfully!${Colors.RESET}")

                    // Fetch and display the cloned objective
                    val clonedObjective = objectiveService.getObjective(clonedObjectiveId)
                    println("${Colors.GREEN}${TableFormatter.formatSingleObjective(clonedObjective)}${Colors.RESET}")
                } else {
                    println("${Colors.YELLOW}Clone cancelled.${Colors.RESET}")
                }
            } catch (e: NumberFormatException) {
                println("${Colors.RED}Invalid objective ID: ${tokens[2]}${Colors.RESET}")
            } catch (e: Exception) {
                println("${Colors.RED}Error cloning objective: ${e.message}${Colors.RESET}")
                e.printStackTrace()
            }
        }
    }

    private fun handleUserAliasCommand(tokens: List<String>) {
        if (tokens.size < 2) {
            println("${Colors.RED}Usage: useralias <create|list|delete> [args...]${Colors.RESET}")
            return
        }

        when (tokens[1].lowercase()) {
            "create" -> {
                if (tokens.size != 4) {
                    println("${Colors.RED}Usage: useralias create <alias> <userid>${Colors.RESET}")
                } else {
                    try {
                        val alias = tokens[2]
                        val userId = tokens[3].toInt()
                        val result = aliasService.createAlias(alias, userId)
                        println("${Colors.GREEN}$result${Colors.RESET}")
                    } catch (e: NumberFormatException) {
                        println("${Colors.RED}Invalid user ID: ${tokens[3]}${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error creating alias: ${e.message}${Colors.RESET}")
                    }
                }
            }

            "list" -> {
                if (tokens.size != 2) {
                    println("${Colors.RED}Usage: useralias list${Colors.RESET}")
                } else {
                    try {
                        val result = aliasService.listAliases()
                        println("${Colors.GREEN}$result${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error listing aliases: ${e.message}${Colors.RESET}")
                    }
                }
            }

            "delete" -> {
                if (tokens.size != 3) {
                    println("${Colors.RED}Usage: useralias delete <alias>${Colors.RESET}")
                } else {
                    val alias = tokens[2]
                    try {
                        val result = aliasService.deleteAlias(alias)
                        println("${Colors.GREEN}$result${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error deleting alias: ${e.message}${Colors.RESET}")
                    }
                }
            }

            else -> {
                println("${Colors.RED}Unknown useralias subcommand: ${tokens[1]}${Colors.RESET}")
                println("${Colors.RED}Usage: useralias <create|list|delete> [args...]${Colors.RESET}")
            }
        }
    }

    private fun handleTeamsCommand(tokens: List<String>) {
        if (tokens.size < 2) {
            println("${Colors.RED}Usage: teams <create|get|add|remove|delete|list> [args...]${Colors.RESET}")
            return
        }

        when (tokens[1].lowercase()) {
            "create" -> {
                if (tokens.size != 3) {
                    println("${Colors.RED}Usage: teams create <team_name>${Colors.RESET}")
                } else {
                    try {
                        val result = teamsService.createTeam(tokens[2])
                        println("${Colors.GREEN}$result${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error creating team: ${e.message}${Colors.RESET}")
                    }
                }
            }

            "get" -> {
                if (tokens.size != 3) {
                    println("${Colors.RED}Usage: teams get <team_name>${Colors.RESET}")
                } else {
                    try {
                        val result = teamsService.getTeam(tokens[2])
                        println("${Colors.GREEN}$result${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error getting team: ${e.message}${Colors.RESET}")
                    }
                }
            }

            "add" -> {
                if (tokens.size != 4) {
                    println("${Colors.RED}Usage: teams add <team_name> <alias>${Colors.RESET}")
                } else {
                    try {
                        val result = teamsService.addMemberToTeam(tokens[2], tokens[3])
                        println("${Colors.GREEN}$result${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error adding member to team: ${e.message}${Colors.RESET}")
                    }
                }
            }

            "remove" -> {
                if (tokens.size != 4) {
                    println("${Colors.RED}Usage: teams remove <team_name> <alias>${Colors.RESET}")
                } else {
                    try {
                        val result = teamsService.removeMemberFromTeam(tokens[2], tokens[3])
                        println("${Colors.GREEN}$result${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error removing member from team: ${e.message}${Colors.RESET}")
                    }
                }
            }

            "list" -> {
                if (tokens.size != 2) {
                    println("${Colors.RED}Usage: teams list${Colors.RESET}")
                } else {
                    try {
                        val result = teamsService.listTeams()
                        println("${Colors.GREEN}$result${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error listing teams: ${e.message}${Colors.RESET}")
                    }
                }
            }

            "delete" -> {
                if (tokens.size != 3) {
                    println("${Colors.RED}Usage: teams delete <team_name>${Colors.RESET}")
                } else {
                    try {
                        val result = teamsService.deleteTeam(tokens[2])
                        println("${Colors.GREEN}$result${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error deleting team: ${e.message}${Colors.RESET}")
                    }
                }
            }

            else -> {
                println("${Colors.RED}Unknown teams subcommand: ${tokens[1]}${Colors.RESET}")
                println("${Colors.RED}Usage: teams <create|get|add|remove|delete|list> [args...]${Colors.RESET}")
            }
        }
    }

    private fun handleHelpCommand() {
        println("${Colors.BOLD}${Colors.CYAN}General:${Colors.RESET}")
        println("  ${Colors.YELLOW}help/?${Colors.RESET}                     - Show this help")
        println("  ${Colors.YELLOW}quit/exit/q${Colors.RESET}               - Exit the shell")
        println("${Colors.BOLD}${Colors.CYAN}Users:${Colors.RESET}")
        println("  ${Colors.YELLOW}users list${Colors.RESET}                 - List all users")
        println("  ${Colors.YELLOW}users list${Colors.RESET} ${Colors.DIM}<search>${Colors.RESET}        - Search for users by name")
        println("  ${Colors.YELLOW}users refresh${Colors.RESET}              - Refresh user cache from API")
        println()
        println("${Colors.BOLD}${Colors.CYAN}Objectives:${Colors.RESET}")
        println("  ${Colors.YELLOW}objectives list${Colors.RESET} ${Colors.DIM}[-c]${Colors.RESET}          - List top 100 objectives")
        println("  ${Colors.YELLOW}objectives list${Colors.RESET} ${Colors.DIM}<limit> [-c]${Colors.RESET}  - List objectives (custom limit)")
        println("  ${Colors.YELLOW}objectives listbyuser${Colors.RESET} ${Colors.DIM}<id> [-c]${Colors.RESET} - List objectives for user ID or alias")
        println("    ${Colors.DIM}Use -compact or -c for table view instead of detailed view${Colors.RESET}")
        println("  ${Colors.YELLOW}objectives get${Colors.RESET} ${Colors.DIM}<id>${Colors.RESET}        - Get single objective by ID")
        println("  ${Colors.YELLOW}objectives clone${Colors.RESET} ${Colors.DIM}<id> <user>${Colors.RESET} - Clone objective to another user")
        println()
        println("${Colors.BOLD}${Colors.CYAN}User Aliases:${Colors.RESET}")
        println("  ${Colors.YELLOW}useralias create${Colors.RESET} ${Colors.DIM}<alias> <userid>${Colors.RESET} - Create user alias")
        println("  ${Colors.YELLOW}useralias list${Colors.RESET}              - List all user aliases")
        println("  ${Colors.YELLOW}useralias delete${Colors.RESET} ${Colors.DIM}<alias>${Colors.RESET}      - Delete user alias")
        println()
        println("${Colors.BOLD}${Colors.CYAN}Teams:${Colors.RESET}")
        println("  ${Colors.YELLOW}teams create${Colors.RESET} ${Colors.DIM}<name>${Colors.RESET}          - Create a new team")
        println("  ${Colors.YELLOW}teams get${Colors.RESET} ${Colors.DIM}<name>${Colors.RESET}             - Show team members with details")
        println("  ${Colors.YELLOW}teams add${Colors.RESET} ${Colors.DIM}<team> <alias>${Colors.RESET}     - Add alias to team")
        println("  ${Colors.YELLOW}teams remove${Colors.RESET} ${Colors.DIM}<team> <alias>${Colors.RESET}  - Remove alias from team")
        println("  ${Colors.YELLOW}teams delete${Colors.RESET} ${Colors.DIM}<name>${Colors.RESET}          - Delete team (keeps aliases)")
        println("  ${Colors.YELLOW}teams list${Colors.RESET}                  - List all teams")
        println()
    }
}

fun main() {
    ShellApp().run()
}