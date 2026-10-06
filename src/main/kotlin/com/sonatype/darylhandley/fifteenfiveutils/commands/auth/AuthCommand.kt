package com.sonatype.darylhandley.fifteenfiveutils.commands.auth

import com.sonatype.darylhandley.fifteenfiveutils.auth.AuthService
import com.sonatype.darylhandley.fifteenfiveutils.commands.Command
import com.sonatype.darylhandley.fifteenfiveutils.ui.ShellUI

/**
 * auth          — read the sessionid cookie value from the clipboard, verify it, save it
 * auth <value>  — same, with the value given inline
 * auth status   — check the current session (read) and the write path (no-op CSRF probe)
 */
class AuthCommand(
    private val authService: AuthService,
    private val ui: ShellUI
) : Command {

    override fun execute(tokens: List<String>) {
        when {
            tokens.size == 2 && tokens[1].lowercase() == "status" -> status()
            tokens.size == 2 -> login(tokens[1])
            tokens.size == 1 -> login(authService.readClipboard())
            else -> ui.printError(getUsage())
        }
    }

    private fun login(candidate: String) {
        val result = authService.login(candidate)
        if (result.ok) ui.printSuccess("✅ ${result.message}") else ui.printError("❌ ${result.message}")
    }

    private fun status() {
        val read = authService.check()
        if (read.ok) ui.printSuccess("✅ ${read.message}") else ui.printError("❌ ${read.message}")
        if (!read.ok) return
        val write = authService.checkWrite()
        if (write.ok) ui.printSuccess("✅ ${write.message}") else ui.printError("❌ ${write.message}")
    }

    override fun getUsage(): String {
        return "Usage: auth [status | <sessionid>]   (no argument = read sessionid from clipboard)"
    }
}
