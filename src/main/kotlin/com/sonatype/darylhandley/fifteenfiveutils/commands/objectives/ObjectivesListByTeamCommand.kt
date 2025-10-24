package com.sonatype.darylhandley.fifteenfiveutils.commands.objectives

import com.sonatype.darylhandley.fifteenfiveutils.commands.Command
import com.sonatype.darylhandley.fifteenfiveutils.service.AliasService
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveService
import com.sonatype.darylhandley.fifteenfiveutils.service.TeamsService
import com.sonatype.darylhandley.fifteenfiveutils.service.UserService
import com.sonatype.darylhandley.fifteenfiveutils.ui.ShellUI
import com.sonatype.darylhandley.fifteenfiveutils.util.TableFormatter

class ObjectivesListByTeamCommand(
    private val objectiveService: ObjectiveService,
    private val teamsService: TeamsService,
    private val userService: UserService,
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

        val teamName = effectiveTokens[2]

        // Get team members
        val teamMembers = teamsService.getTeamMembers(teamName)
        if (teamMembers == null) {
            ui.printError("Team '$teamName' not found.")
            return
        }

        if (teamMembers.isEmpty()) {
            ui.printWarning("Team '$teamName' has no members.")
            return
        }

        // Collect objectives for all team members
        val allObjectives = mutableListOf<com.sonatype.darylhandley.fifteenfiveutils.model.Objective>()
        val warnings = mutableListOf<String>()
        val usersWithNoObjectives = mutableListOf<String>()

        teamMembers.forEach { alias ->
            val userId = aliasService.resolveUserIdentifier(alias)
            if (userId == null) {
                warnings.add("Could not resolve alias '$alias'")
            } else {
                try {
                    val objectives = objectiveService.listObjectivesByUser(userId)
                    if (objectives.isEmpty()) {
                        val user = userService.getUserById(userId)
                        usersWithNoObjectives.add(user?.fullName ?: "User ID $userId")
                    } else {
                        allObjectives.addAll(objectives)
                    }
                } catch (e: Exception) {
                    warnings.add("Error fetching objectives for alias '$alias': ${e.message}")
                }
            }
        }

        // Show warnings if any
        warnings.forEach { warning ->
            ui.printWarning("Warning: $warning")
        }

        // Sort objectives by username, then objectiveId
        val sortedObjectives = allObjectives.sortedWith(compareBy({ it.user.name }, { it.id }))

        // Format output
        val formatted = if (isVerbose) {
            TableFormatter.formatObjectivesList(sortedObjectives)
        } else {
            TableFormatter.formatObjectivesCompactTable(sortedObjectives, ui.getTerminalWidth())
        }

        ui.printSuccess(formatted)

        // Show users with no objectives
        if (usersWithNoObjectives.isNotEmpty()) {
            ui.printWarning("Users with no objectives:")
            usersWithNoObjectives.sorted().forEach { userName ->
                ui.printWarning("  • $userName")
            }
        }
    }

    override fun getUsage(): String {
        return "Usage: objectives listbyteam <team_name> [-verbose|-v]"
    }
}
