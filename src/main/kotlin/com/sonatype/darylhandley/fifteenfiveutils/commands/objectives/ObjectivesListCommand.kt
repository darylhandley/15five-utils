package com.sonatype.darylhandley.fifteenfiveutils.commands.objectives

import com.sonatype.darylhandley.fifteenfiveutils.commands.Command
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveService
import com.sonatype.darylhandley.fifteenfiveutils.ui.ShellUI
import com.sonatype.darylhandley.fifteenfiveutils.util.TableFormatter

class ObjectivesListCommand(
    private val objectiveService: ObjectiveService,
    private val ui: ShellUI
) : Command {

    override fun execute(tokens: List<String>) {
        // Check for verbose flag
        val isVerbose = tokens.lastOrNull()?.lowercase() in listOf("-verbose", "-v")
        val effectiveTokens = if (isVerbose) tokens.dropLast(1) else tokens

        when {
            // objectives list [-v]
            effectiveTokens.size == 2 -> {
                val objectives = objectiveService.listObjectives(100)
                val formatted = if (isVerbose) {
                    TableFormatter.formatObjectivesList(objectives)
                } else {
                    TableFormatter.formatObjectivesCompactTable(objectives, ui.getTerminalWidth())
                }
                ui.printSuccess(formatted)
            }

            // objectives list <limit> [-v]
            effectiveTokens.size == 3 -> {
                val limit = effectiveTokens[2].toIntOrNull()
                if (limit == null) {
                    ui.printError("Invalid number: ${effectiveTokens[2]}")
                    return
                }

                val objectives = objectiveService.listObjectives(limit)
                val formatted = if (isVerbose) {
                    TableFormatter.formatObjectivesList(objectives)
                } else {
                    TableFormatter.formatObjectivesCompactTable(objectives, ui.getTerminalWidth())
                }
                ui.printSuccess(formatted)
            }

            else -> {
                ui.printError(getUsage())
            }
        }
    }

    override fun getUsage(): String {
        return "Usage: objectives list [limit] [-verbose|-v]"
    }
}
