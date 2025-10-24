package com.sonatype.darylhandley.fifteenfiveutils.commands.useralias

import com.sonatype.darylhandley.fifteenfiveutils.commands.Command
import com.sonatype.darylhandley.fifteenfiveutils.service.AliasService
import com.sonatype.darylhandley.fifteenfiveutils.ui.ShellUI

class UserAliasDeleteCommand(
    private val aliasService: AliasService,
    private val ui: ShellUI
) : Command {

    override fun execute(tokens: List<String>) {
        if (tokens.size != 3) {
            ui.printError(getUsage())
            return
        }

        val alias = tokens[2]
        val result = aliasService.deleteAlias(alias)
        ui.printSuccess(result)
    }

    override fun getUsage(): String {
        return "Usage: useralias delete <alias>"
    }
}
