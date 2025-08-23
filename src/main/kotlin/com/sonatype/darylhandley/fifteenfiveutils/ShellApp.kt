package com.sonatype.darylhandley.fifteenfiveutils

import com.sonatype.darylhandley.fifteenfiveutils.service.UserService
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
    
    val userService = try {
        UserService(ConfigLoader.getSessionId())
    } catch (e: Exception) {
        println("${Colors.RED}Configuration error: ${e.message}${Colors.RESET}")
        return
    }
    
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
        "users list"
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
                    val users = userService.listAllUsers()
                    println("${Colors.GREEN}${TableFormatter.formatUsersTable(users)}${Colors.RESET}")
                }
                input.lowercase().startsWith("users list ") -> {
                    val searchTerm = input.substring(11) // "users list ".length = 11
                    val users = userService.searchUsers(searchTerm)
                    println("${Colors.GREEN}${TableFormatter.formatUsersTable(users)}${Colors.RESET}")
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
            
        } catch (e: Exception) {
            // Handle Ctrl+C or other interruptions gracefully
            println("\n${Colors.YELLOW}Goodbye!${Colors.RESET}")
            break
        }
    }
    
    terminal.close()
}

private fun showHelp() {
    println("${Colors.BOLD}${Colors.CYAN}Available commands:${Colors.RESET}")
    println("  ${Colors.YELLOW}echo${Colors.RESET} ${Colors.DIM}<message>${Colors.RESET}         - Echo the message back")
    println("  ${Colors.YELLOW}users list${Colors.RESET}              - List all users")
    println("  ${Colors.YELLOW}users list${Colors.RESET} ${Colors.DIM}<search>${Colors.RESET}     - Search for users by name")
    println("  ${Colors.YELLOW}help${Colors.RESET}                    - Show this help")
    println("  ${Colors.YELLOW}quit/exit${Colors.RESET}               - Exit the shell")
    println()
    println("${Colors.BOLD}${Colors.CYAN}Shell features:${Colors.RESET}")
    println("  ${Colors.DIM}↑/↓ arrows${Colors.RESET}              - Navigate command history")
    println("  ${Colors.DIM}Tab${Colors.RESET}                     - Auto-complete commands")
    println("  ${Colors.DIM}←/→ arrows${Colors.RESET}              - Edit current line")
    println("  ${Colors.DIM}Ctrl+C${Colors.RESET}                  - Exit the shell")
}