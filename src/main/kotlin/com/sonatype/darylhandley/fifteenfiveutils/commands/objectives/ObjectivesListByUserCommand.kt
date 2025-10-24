package com.sonatype.darylhandley.fifteenfiveutils.commands.objectives

import com.sonatype.darylhandley.fifteenfiveutils.commands.Command
import com.sonatype.darylhandley.fifteenfiveutils.service.AliasService
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveService
import com.sonatype.darylhandley.fifteenfiveutils.ui.ShellUI
import com.sonatype.darylhandley.fifteenfiveutils.util.TableFormatter

class ObjectivesListByUserCommand(
    private val objectiveService: ObjectiveService,
    private val aliasService: AliasService,
    private val ui: ShellUI
) : Command {

    override fun execute(tokens: List<String>) {
        // Check for verbose flag
        val isVerbose = tokens.lastOrNull()?.lowercase() in listOf("-verbose", "-v")
        val effectiveTokens = if (isVerbose) tokens.dropLast(1) else tokens

        if (effectiveTokens.size != 3) {
            ui.printError(getUsage())
            return
        }

        val userIdentifier = effectiveTokens[2]
        val userId = aliasService.resolveUserIdentifier(userIdentifier)

        if (userId == null) {
            ui.printError("Unknown user identifier: $userIdentifier")
            return
        }

        val objectives = objectiveService.listObjectivesByUser(userId)
        val formatted = if (isVerbose) {
            TableFormatter.formatObjectivesList(objectives)
        } else {
            TableFormatter.formatObjectivesCompactTable(objectives, ui.getTerminalWidth())
        }
        ui.printSuccess(formatted)
    }

    override fun getUsage(): String {
        return "Usage: objectives listbyuser <userid or alias> [-verbose|-v]"
    }
}
