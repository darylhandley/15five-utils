package com.sonatype.darylhandley.fifteenfiveutils.commands.useralias

import com.sonatype.darylhandley.fifteenfiveutils.commands.Command
import com.sonatype.darylhandley.fifteenfiveutils.service.AliasService
import com.sonatype.darylhandley.fifteenfiveutils.ui.ShellUI

class UserAliasListCommand(
    private val aliasService: AliasService,
    private val ui: ShellUI
) : Command {

    override fun execute(tokens: List<String>) {
        if (tokens.size != 2) {
            ui.printError(getUsage())
            return
        }

        val result = aliasService.listAliases()
        ui.printSuccess(result)
    }

    override fun getUsage(): String {
        return "Usage: useralias list"
    }
}
