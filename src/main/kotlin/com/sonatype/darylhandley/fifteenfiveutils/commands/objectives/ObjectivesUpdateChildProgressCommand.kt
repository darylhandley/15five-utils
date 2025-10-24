package com.sonatype.darylhandley.fifteenfiveutils.commands.objectives

import com.sonatype.darylhandley.fifteenfiveutils.commands.Command
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveService
import com.sonatype.darylhandley.fifteenfiveutils.ui.ShellUI

class ObjectivesUpdateChildProgressCommand(
    private val objectiveService: ObjectiveService,
    private val ui: ShellUI
) : Command {

    override fun execute(tokens: List<String>) {
        if (tokens.size != 3) {
            ui.printError(getUsage())
            return
        }

        val parentObjectiveId = tokens[2].toIntOrNull()
        if (parentObjectiveId == null) {
            ui.printError("Invalid objective ID: ${tokens[2]}")
            return
        }

        try {
            // Build the update plan
            val plan = objectiveService.buildChildrenUpdatePreview(parentObjectiveId)

            if (plan == null) {
                val parent = objectiveService.getObjective(parentObjectiveId)
                ui.printWarning("No children found for objective $parentObjectiveId (\"${parent.description}\")")
                return
            }

            // Show preview
            val preview = objectiveService.formatChildrenUpdatePreview(plan)
            ui.printCyan(preview)

            if (ui.confirm("Proceed with updates? (y/N): ")) {
                ui.printWarning("Updating child objectives...")
                val result = objectiveService.executeChildrenUpdate(plan)
                ui.printSuccess(result)
            } else {
                ui.printWarning("Update cancelled.")
            }

        } catch (e: IllegalStateException) {
            ui.printError("Error: ${e.message}")
        }
    }

    override fun getUsage(): String {
        return "Usage: objectives updatechildprogress <parent_objective_id>"
    }
}
