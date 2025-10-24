package com.sonatype.darylhandley.fifteenfiveutils.commands.users

import com.sonatype.darylhandley.fifteenfiveutils.commands.Command
import com.sonatype.darylhandley.fifteenfiveutils.service.UserService
import com.sonatype.darylhandley.fifteenfiveutils.ui.ShellUI

class UsersRefreshCommand(
    private val userService: UserService,
    private val ui: ShellUI
) : Command {

    override fun execute(tokens: List<String>) {
        if (tokens.size != 2) {
            ui.printError(getUsage())
            return
        }

        val result = userService.refreshUserCache()
        ui.printSuccess(result)
    }

    override fun getUsage(): String {
        return "Usage: users refresh"
    }
}
