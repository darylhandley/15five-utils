package com.sonatype.darylhandley.fifteenfiveutils

import com.sonatype.darylhandley.fifteenfiveutils.service.UserService
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveService
import com.sonatype.darylhandley.fifteenfiveutils.util.ConfigLoader
import com.sonatype.darylhandley.fifteenfiveutils.util.TableFormatter
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder

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
    println("${Colors.BOLD}${Colors.CYAN}15Five Utils Shell${Colors.RESET} - Type '${Colors.YELLOW}help${Colors.RESET}' for commands or '${Colors.YELLOW}quit${Colors.RESET}' to exit")
    println("${Colors.DIM}${"─".repeat(60)}${Colors.RESET}")
    
    val sessionId = try {
        ConfigLoader.getSessionId()
    } catch (e: Exception) {
        println("${Colors.RED}Configuration error: ${e.message}${Colors.RESET}")
        return
    }
    
    val userService = UserService(sessionId)
    val objectiveService = ObjectiveService(sessionId)
    
    // Set up JLine3 terminal and line reader
    val terminal: Terminal = TerminalBuilder.builder()
        .system(true)
        .build()
        
    // Set up tab completion for available commands
    val completer = StringsCompleter(
        "help",
        "quit", 
        "exit",
        "echo",
        "users list",
        "objectives list",
        "objectives listbyuser",
        "objectives get"
    )
    
    val lineReader: LineReader = LineReaderBuilder.builder()
        .terminal(terminal)
        .completer(completer)
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
                    val userIdStr = input.substring(22) // "objectives listbyuser ".length = 22
                    try {
                        val userId = userIdStr.toInt()
                        val objectives = objectiveService.listObjectivesByUser(userId)
                        println("${Colors.GREEN}${TableFormatter.formatObjectivesList(objectives)}${Colors.RESET}")
                    } catch (e: NumberFormatException) {
                        println("${Colors.RED}Invalid user ID: $userIdStr${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error fetching objectives: ${e.message}${Colors.RESET}")
                        e.printStackTrace()
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
                input.lowercase().startsWith("echo ") -> {
                    val message = input.substring(5)
                    println("${Colors.GREEN}$message${Colors.RESET}")
                }
                input.lowercase() == "echo" -> {
                    println("${Colors.RED}echo: missing argument${Colors.RESET}")
                }
                input.lowercase() == "quit" || input.lowercase() == "exit" -> {
                    println("${Colors.YELLOW}Goodbye!${Colors.RESET}")
                    running = false
                }
                input.lowercase() == "help" -> {
                    showHelp()
                }
                else -> {
                    println("${Colors.RED}Unknown command: $input. Type '${Colors.YELLOW}help${Colors.RESET}' for available commands.${Colors.RESET}")
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
}

private fun showHelp() {
    println("${Colors.BOLD}${Colors.CYAN}Available commands:${Colors.RESET}")
    println("  ${Colors.YELLOW}echo${Colors.RESET} ${Colors.DIM}<message>${Colors.RESET}            - Echo the message back")
    println()
    println("${Colors.BOLD}${Colors.CYAN}Users:${Colors.RESET}")
    println("  ${Colors.YELLOW}users list${Colors.RESET}                 - List all users")
    println("  ${Colors.YELLOW}users list${Colors.RESET} ${Colors.DIM}<search>${Colors.RESET}        - Search for users by name")
    println()
    println("${Colors.BOLD}${Colors.CYAN}Objectives:${Colors.RESET}")
    println("  ${Colors.YELLOW}objectives list${Colors.RESET} ${Colors.DIM}<limit>${Colors.RESET}     - List objectives (limit number)")
    println("  ${Colors.YELLOW}objectives listbyuser${Colors.RESET} ${Colors.DIM}<id>${Colors.RESET} - List objectives for user ID")
    println("  ${Colors.YELLOW}objectives get${Colors.RESET} ${Colors.DIM}<id>${Colors.RESET}        - Get single objective by ID")
    println()
    println("${Colors.BOLD}${Colors.CYAN}General:${Colors.RESET}")
    println("  ${Colors.YELLOW}help${Colors.RESET}                       - Show this help")
    println("  ${Colors.YELLOW}quit/exit${Colors.RESET}                  - Exit the shell")
    println()
    println("${Colors.BOLD}${Colors.CYAN}Shell features:${Colors.RESET}")
    println("  ${Colors.DIM}↑/↓ arrows${Colors.RESET}              - Navigate command history")
    println("  ${Colors.DIM}Tab${Colors.RESET}                     - Auto-complete commands")
    println("  ${Colors.DIM}←/→ arrows${Colors.RESET}              - Edit current line")
    println("  ${Colors.DIM}Ctrl+C${Colors.RESET}                  - Exit the shell")
}