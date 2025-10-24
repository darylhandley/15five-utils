package com.sonatype.darylhandley.fifteenfiveutils.commands.objectives

import com.sonatype.darylhandley.fifteenfiveutils.commands.Command
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveService
import com.sonatype.darylhandley.fifteenfiveutils.ui.ShellUI

class ObjectivesUpdateProgressCommand(
    private val objectiveService: ObjectiveService,
    private val ui: ShellUI
) : Command {

    override fun execute(tokens: List<String>) {
        if (tokens.size != 3) {
            ui.printError(getUsage())
            return
        }

        val childObjectiveId = tokens[2].toIntOrNull()
        if (childObjectiveId == null) {
            ui.printError("Invalid objective ID: ${tokens[2]}")
            return
        }

        try {
            val result = objectiveService.updateProgressFromParent(childObjectiveId)
            ui.printSuccess(result)
        } catch (e: IllegalStateException) {
            ui.printError("Error: ${e.message}")
        }
    }

    override fun getUsage(): String {
        return "Usage: objectives updateprogress <child_objective_id>"
    }
}
