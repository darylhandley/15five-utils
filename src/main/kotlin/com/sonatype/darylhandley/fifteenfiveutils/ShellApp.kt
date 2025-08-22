package com.sonatype.darylhandley.fifteenfiveutils

fun main() {
    println("15Five Utils Shell - Type 'help' for commands or 'quit' to exit")
    
    var running = true
    
    while (running) {
        print("> ")
        val input = readLine()?.trim() ?: continue
        
        if (input.isEmpty()) continue
        
        val parts = input.split(" ", limit = 2)
        val command = parts[0].lowercase()
        
        when (command) {
            "echo" -> {
                if (parts.size > 1) {
                    println(parts[1])
                } else {
                    println("echo: missing argument")
                }
            }
            "quit", "exit" -> {
                println("Goodbye!")
                running = false
            }
            "help" -> {
                showHelp()
            }
            else -> {
                println("Unknown command: $command. Type 'help' for available commands.")
            }
        }
    }
}

private fun showHelp() {
    println("Available commands:")
    println("  echo <message>  - Echo the message back")
    println("  help           - Show this help")
    println("  quit/exit      - Exit the shell")
}