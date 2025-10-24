package com.sonatype.darylhandley.fifteenfiveutils.commands.useralias

import com.sonatype.darylhandley.fifteenfiveutils.commands.Command
import com.sonatype.darylhandley.fifteenfiveutils.service.AliasService
import com.sonatype.darylhandley.fifteenfiveutils.ui.ShellUI

class UserAliasCreateCommand(
    private val aliasService: AliasService,
    private val ui: ShellUI
) : Command {

    override fun execute(tokens: List<String>) {
        if (tokens.size != 4) {
            ui.printError(getUsage())
            return
        }

        val alias = tokens[2]
        val userId = tokens[3].toIntOrNull()

        if (userId == null) {
            ui.printError("Invalid user ID: ${tokens[3]}")
            return
        }

        val result = aliasService.createAlias(alias, userId)
        ui.printSuccess(result)
    }

    override fun getUsage(): String {
        return "Usage: useralias create <alias> <userid>"
    }
}
