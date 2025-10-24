package com.sonatype.darylhandley.fifteenfiveutils.commands.teams

import com.sonatype.darylhandley.fifteenfiveutils.commands.Command
import com.sonatype.darylhandley.fifteenfiveutils.service.TeamsService
import com.sonatype.darylhandley.fifteenfiveutils.ui.ShellUI

class TeamsGetCommand(
    private val teamsService: TeamsService,
    private val ui: ShellUI
) : Command {

    override fun execute(tokens: List<String>) {
        if (tokens.size != 3) {
            ui.printError(getUsage())
            return
        }

        val result = teamsService.getTeam(tokens[2])
        ui.printSuccess(result)
    }

    override fun getUsage(): String {
        return "Usage: teams get <team_name>"
    }
}
