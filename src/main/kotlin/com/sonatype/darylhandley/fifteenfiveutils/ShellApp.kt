package com.sonatype.darylhandley.fifteenfiveutils

import com.sonatype.darylhandley.fifteenfiveutils.service.UserService
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveService
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveCloneService
import com.sonatype.darylhandley.fifteenfiveutils.service.AliasService
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

fun main() {
    println("${Colors.BOLD}${Colors.CYAN}15Five Utils Shell${Colors.RESET} - Type '${Colors.YELLOW}help${Colors.RESET}' or '${Colors.YELLOW}?${Colors.RESET}' for commands or '${Colors.YELLOW}q${Colors.RESET}' to exit")
    println("${Colors.DIM}${"─".repeat(60)}${Colors.RESET}")
    
    val sessionId = try {
        ConfigLoader.getSessionId()
    } catch (e: Exception) {
        println("${Colors.RED}Configuration error: ${e.message}${Colors.RESET}")
        return
    }
    
    val userService = UserService(sessionId)
    val objectiveService = ObjectiveService(sessionId)
    val objectiveCloneService = ObjectiveCloneService(sessionId)
    val aliasService = AliasService(userService)
    
    // Set up JLine3 terminal and line reader
    val terminal: Terminal = TerminalBuilder.builder()
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
        "q",
        "users list",
        "objectives list",
        "objectives listbyuser",
        "objectives get",
        "objectives clone",
        "useralias create",
        "useralias list",
        "useralias remove"
    )
    
    val lineReader: LineReader = LineReaderBuilder.builder()
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
            
            when {
                input.lowercase() == "users list" -> {
                    try {
                        val users = userService.listAllUsers()
                        println("${Colors.GREEN}${TableFormatter.formatUsersTable(users)}${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error fetching users: ${e.message}${Colors.RESET}")
                        e.printStackTrace()
                    }
                }
                input.lowercase().startsWith("users list ") -> {
                    try {
                        val searchTerm = input.substring(11) // "users list ".length = 11
                        val users = userService.searchUsers(searchTerm)
                        println("${Colors.GREEN}${TableFormatter.formatUsersTable(users)}${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error searching users: ${e.message}${Colors.RESET}")
                        e.printStackTrace()
                    }
                }
                input.lowercase() == "objectives list" -> {
                    try {
                        val objectives = objectiveService.listObjectives(100)
                        println("${Colors.GREEN}${TableFormatter.formatObjectivesList(objectives)}${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error fetching objectives: ${e.message}${Colors.RESET}")
                        e.printStackTrace()
                    }
                }
                input.lowercase().startsWith("objectives list ") -> {
                    val limitStr = input.substring(16) // "objectives list ".length = 16
                    try {
                        val limit = limitStr.toInt()
                        val objectives = objectiveService.listObjectives(limit)
                        println("${Colors.GREEN}${TableFormatter.formatObjectivesList(objectives)}${Colors.RESET}")
                    } catch (e: NumberFormatException) {
                        println("${Colors.RED}Invalid number: $limitStr${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error fetching objectives: ${e.message}${Colors.RESET}")
                        e.printStackTrace()
                    }
                }
                input.lowercase().startsWith("objectives listbyuser ") -> {
                    val userIdentifier = input.substring(22).trim() // "objectives listbyuser ".length = 22
                    if (userIdentifier.isEmpty()) {
                        println("${Colors.RED}Usage: objectives listbyuser <userid or alias>${Colors.RESET}")
                    } else {
                        try {
                            val userId = aliasService.resolveUserIdentifier(userIdentifier)
                            if (userId == null) {
                                println("${Colors.RED}Unknown user identifier: $userIdentifier${Colors.RESET}")
                            } else {
                                val objectives = objectiveService.listObjectivesByUser(userId)
                                println("${Colors.GREEN}${TableFormatter.formatObjectivesList(objectives)}${Colors.RESET}")
                            }
                        } catch (e: Exception) {
                            println("${Colors.RED}Error fetching objectives: ${e.message}${Colors.RESET}")
                            e.printStackTrace()
                        }
                    }
                }
                input.lowercase().startsWith("objectives get ") -> {
                    val objectiveIdStr = input.substring(15) // "objectives get ".length = 15
                    try {
                        val objectiveId = objectiveIdStr.toInt()
                        val objective = objectiveService.getObjective(objectiveId)
                        println("${Colors.GREEN}${TableFormatter.formatSingleObjective(objective)}${Colors.RESET}")
                    } catch (e: NumberFormatException) {
                        println("${Colors.RED}Invalid objective ID: $objectiveIdStr${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error fetching objective: ${e.message}${Colors.RESET}")
                        e.printStackTrace()
                    }
                }
                input.lowercase().startsWith("objectives clone ") -> {
                    val params = input.substring(17).trim() // "objectives clone ".length = 17
                    val parts = params.split(" ", limit = 2)
                    if (parts.size != 2) {
                        println("${Colors.RED}Usage: objectives clone <objective_id> <target_user_id_or_alias>${Colors.RESET}")
                    } else {
                        try {
                            val objectiveId = parts[0].toInt()
                            val targetUserIdentifier = parts[1]
                            
                            // Resolve target user
                            val targetUserId = aliasService.resolveUserIdentifier(targetUserIdentifier)
                            if (targetUserId == null) {
                                println("${Colors.RED}Unknown user identifier: $targetUserIdentifier${Colors.RESET}")
                                continue
                            }
                            
                            // Get source objective
                            val sourceObjective = objectiveService.getObjective(objectiveId)
                            
                            // Get target user name for display
                            val targetUserName = userService.listAllUsers()
                                .find { it.id == targetUserId }?.fullName ?: "User ID $targetUserId"
                            
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
                            println("${Colors.RED}Invalid objective ID: ${parts[0]}${Colors.RESET}")
                        } catch (e: Exception) {
                            println("${Colors.RED}Error cloning objective: ${e.message}${Colors.RESET}")
                            e.printStackTrace()
                        }
                    }
                }
                input.lowercase().startsWith("useralias create ") -> {
                    val params = input.substring(17).trim() // "useralias create ".length = 17
                    val parts = params.split(" ", limit = 2)
                    if (parts.size != 2) {
                        println("${Colors.RED}Usage: useralias create <alias> <userid>${Colors.RESET}")
                    } else {
                        try {
                            val alias = parts[0]
                            val userId = parts[1].toInt()
                            val result = aliasService.createAlias(alias, userId)
                            println("${Colors.GREEN}$result${Colors.RESET}")
                        } catch (e: NumberFormatException) {
                            println("${Colors.RED}Invalid user ID: ${parts[1]}${Colors.RESET}")
                        } catch (e: Exception) {
                            println("${Colors.RED}Error creating alias: ${e.message}${Colors.RESET}")
                        }
                    }
                }
                input.lowercase() == "useralias list" -> {
                    try {
                        val result = aliasService.listAliases()
                        println("${Colors.GREEN}$result${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error listing aliases: ${e.message}${Colors.RESET}")
                    }
                }
                input.lowercase().startsWith("useralias remove ") -> {
                    val alias = input.substring(17).trim() // "useralias remove ".length = 17
                    if (alias.isEmpty()) {
                        println("${Colors.RED}Usage: useralias remove <alias>${Colors.RESET}")
                    } else {
                        try {
                            val result = aliasService.removeAlias(alias)
                            println("${Colors.GREEN}$result${Colors.RESET}")
                        } catch (e: Exception) {
                            println("${Colors.RED}Error removing alias: ${e.message}${Colors.RESET}")
                        }
                    }
                }
                input.lowercase() == "quit" || input.lowercase() == "q" -> {
                    println("${Colors.YELLOW}Goodbye!${Colors.RESET}")
                    running = false
                }
                input.lowercase() == "help" || input == "?" -> {
                    showHelp()
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

private fun showHelp() {
    println("${Colors.BOLD}${Colors.CYAN}General:${Colors.RESET}")
    println("  ${Colors.YELLOW}help/?${Colors.RESET}                     - Show this help")
    println("  ${Colors.YELLOW}quit/q${Colors.RESET}                    - Exit the shell")
    println("${Colors.BOLD}${Colors.CYAN}Users:${Colors.RESET}")
    println("  ${Colors.YELLOW}users list${Colors.RESET}                 - List all users")
    println("  ${Colors.YELLOW}users list${Colors.RESET} ${Colors.DIM}<search>${Colors.RESET}        - Search for users by name")
    println()
    println("${Colors.BOLD}${Colors.CYAN}Objectives:${Colors.RESET}")
    println("  ${Colors.YELLOW}objectives list${Colors.RESET}             - List top 100 objectives")
    println("  ${Colors.YELLOW}objectives list${Colors.RESET} ${Colors.DIM}<limit>${Colors.RESET}     - List objectives (custom limit)")
    println("  ${Colors.YELLOW}objectives listbyuser${Colors.RESET} ${Colors.DIM}<id>${Colors.RESET} - List objectives for user ID or alias")
    println("  ${Colors.YELLOW}objectives get${Colors.RESET} ${Colors.DIM}<id>${Colors.RESET}        - Get single objective by ID")
    println("  ${Colors.YELLOW}objectives clone${Colors.RESET} ${Colors.DIM}<id> <user>${Colors.RESET} - Clone objective to another user")
    println()
    println("${Colors.BOLD}${Colors.CYAN}User Aliases:${Colors.RESET}")
    println("  ${Colors.YELLOW}useralias create${Colors.RESET} ${Colors.DIM}<alias> <userid>${Colors.RESET} - Create user alias")
    println("  ${Colors.YELLOW}useralias list${Colors.RESET}              - List all user aliases")
    println("  ${Colors.YELLOW}useralias remove${Colors.RESET} ${Colors.DIM}<alias>${Colors.RESET}      - Remove user alias")
    println()
}