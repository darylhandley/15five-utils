package com.sonatype.darylhandley.fifteenfiveutils.commands.teams

import com.sonatype.darylhandley.fifteenfiveutils.commands.Command
import com.sonatype.darylhandley.fifteenfiveutils.service.TeamsService
import com.sonatype.darylhandley.fifteenfiveutils.ui.ShellUI

class TeamsDeleteCommand(
    private val teamsService: TeamsService,
    private val ui: ShellUI
) : Command {

    override fun execute(tokens: List<String>) {
        if (tokens.size != 3) {
            ui.printError(getUsage())
            return
        }

        val result = teamsService.deleteTeam(tokens[2])
        ui.printSuccess(result)
    }

    override fun getUsage(): String {
        return "Usage: teams delete <team_name>"
    }
}
