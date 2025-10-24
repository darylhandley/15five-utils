package com.sonatype.darylhandley.fifteenfiveutils.commands.users

import com.sonatype.darylhandley.fifteenfiveutils.commands.Command
import com.sonatype.darylhandley.fifteenfiveutils.service.UserService
import com.sonatype.darylhandley.fifteenfiveutils.ui.ShellUI
import com.sonatype.darylhandley.fifteenfiveutils.util.TableFormatter

class UsersListCommand(
    private val userService: UserService,
    private val ui: ShellUI
) : Command {

    override fun execute(tokens: List<String>) {
        when {
            // users list
            tokens.size == 2 -> {
                val users = userService.listAllUsers()
                ui.printSuccess(TableFormatter.formatUsersTable(users))
            }

            // users list <search_term>
            tokens.size > 2 -> {
                val searchTerm = tokens.drop(2).joinToString(" ")
                val users = userService.searchUsers(searchTerm)
                ui.printSuccess(TableFormatter.formatUsersTable(users))
            }

            else -> {
                ui.printError(getUsage())
            }
        }
    }

    override fun getUsage(): String {
        return "Usage: users list [search_term]"
    }
}
