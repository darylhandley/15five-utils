package com.sonatype.darylhandley.fifteenfiveutils.commands.teams

import com.sonatype.darylhandley.fifteenfiveutils.commands.Command
import com.sonatype.darylhandley.fifteenfiveutils.service.TeamsService
import com.sonatype.darylhandley.fifteenfiveutils.ui.ShellUI

class TeamsListCommand(
    private val teamsService: TeamsService,
    private val ui: ShellUI
) : Command {

    override fun execute(tokens: List<String>) {
        if (tokens.size != 2) {
            ui.printError(getUsage())
            return
        }

        val result = teamsService.listTeams()
        ui.printSuccess(result)
    }

    override fun getUsage(): String {
        return "Usage: teams list"
    }
}
