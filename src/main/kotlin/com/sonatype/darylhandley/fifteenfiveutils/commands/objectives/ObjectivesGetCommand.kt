package com.sonatype.darylhandley.fifteenfiveutils.commands.objectives

import com.sonatype.darylhandley.fifteenfiveutils.commands.Command
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveService
import com.sonatype.darylhandley.fifteenfiveutils.ui.ShellUI
import com.sonatype.darylhandley.fifteenfiveutils.util.TableFormatter

class ObjectivesGetCommand(
    private val objectiveService: ObjectiveService,
    private val ui: ShellUI
) : Command {

    override fun execute(tokens: List<String>) {
        if (tokens.size != 3) {
            ui.printError(getUsage())
            return
        }

        val objectiveId = tokens[2].toIntOrNull()
        if (objectiveId == null) {
            ui.printError("Invalid objective ID: ${tokens[2]}")
            return
        }

        val objective = objectiveService.getObjective(objectiveId)
        ui.printSuccess(TableFormatter.formatSingleObjective(objective))
    }

    override fun getUsage(): String {
        return "Usage: objectives get <objective_id>"
    }
}
