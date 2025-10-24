package com.sonatype.darylhandley.fifteenfiveutils.commands.useralias

import com.sonatype.darylhandley.fifteenfiveutils.commands.Command
import com.sonatype.darylhandley.fifteenfiveutils.service.AliasService
import com.sonatype.darylhandley.fifteenfiveutils.ui.ShellUI

class UserAliasCreateBySearchCommand(
    private val aliasService: AliasService,
    private val ui: ShellUI
) : Command {

    override fun execute(tokens: List<String>) {
        if (tokens.size < 4) {
            ui.printError(getUsage())
            return
        }

        val alias = tokens[2]
        val searchTerm = tokens.drop(3).joinToString(" ")

        val users = aliasService.searchUsersForAlias(searchTerm)

        when {
            users == null -> {
                ui.printError("Error: User service not available")
            }
            users.isEmpty() -> {
                ui.printError("No users found matching '$searchTerm'")
            }
            users.size == 1 -> {
                val user = users[0]
                ui.printCyan("Create alias '$alias' for user '${user.fullName}' (ID: ${user.id})? (y/N): ")

                if (ui.confirm("")) {
                    val result = aliasService.createAlias(alias, user.id)
                    ui.printSuccess(result)
                } else {
                    ui.printWarning("Alias creation cancelled.")
                }
            }
            else -> {
                ui.printError("Multiple users found matching '$searchTerm':")
                users.forEach { user ->
                    ui.printError("  • ${user.fullName} (ID: ${user.id})")
                }
                ui.printError("Please make your search more specific.")
            }
        }
    }

    override fun getUsage(): String {
        return "Usage: useralias createbysearch <alias> <search_term>"
    }
}
