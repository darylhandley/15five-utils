package com.sonatype.darylhandley.fifteenfiveutils.commands.teams

import com.sonatype.darylhandley.fifteenfiveutils.commands.Command
import com.sonatype.darylhandley.fifteenfiveutils.service.TeamsService
import com.sonatype.darylhandley.fifteenfiveutils.ui.ShellUI

class TeamsRemoveCommand(
    private val teamsService: TeamsService,
    private val ui: ShellUI
) : Command {

    override fun execute(tokens: List<String>) {
        if (tokens.size != 4) {
            ui.printError(getUsage())
            return
        }

        val result = teamsService.removeMemberFromTeam(tokens[2], tokens[3])
        ui.printSuccess(result)
    }

    override fun getUsage(): String {
        return "Usage: teams remove <team_name> <alias>"
    }
}
