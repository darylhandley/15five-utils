package com.sonatype.darylhandley.fifteenfiveutils.ui

import org.jline.reader.LineReader
import org.jline.terminal.Terminal

class ShellUI(
    private val terminal: Terminal,
    private val lineReader: LineReader
) {
    fun printSuccess(message: String) {
        println("${Colors.GREEN}$message${Colors.RESET}")
    }

    fun printError(message: String) {
        println("${Colors.RED}$message${Colors.RESET}")
    }

    fun printWarning(message: String) {
        println("${Colors.YELLOW}$message${Colors.RESET}")
    }

    fun printCyan(message: String) {
        println("${Colors.CYAN}$message${Colors.RESET}")
    }

    fun printDim(message: String) {
        println("${Colors.DIM}$message${Colors.RESET}")
    }

    fun print(message: String) {
        println(message)
    }

    fun printSeparator(length: Int = 30) {
        println("${Colors.DIM}${"─".repeat(length)}${Colors.RESET}")
    }

    fun confirm(prompt: String): Boolean {
        val response = lineReader.readLine("${Colors.YELLOW}$prompt${Colors.RESET}")
        return response.lowercase() in listOf("y", "yes")
    }

    fun readLine(prompt: String): String {
        return lineReader.readLine(prompt)
    }

    fun getTerminalWidth(): Int {
        return try {
            terminal.width
        } catch (e: Exception) {
            120
        }
    }
}
