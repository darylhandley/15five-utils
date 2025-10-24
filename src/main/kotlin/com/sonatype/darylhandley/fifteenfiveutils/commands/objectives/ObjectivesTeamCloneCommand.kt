package com.sonatype.darylhandley.fifteenfiveutils.commands.objectives

import com.sonatype.darylhandley.fifteenfiveutils.commands.Command
import com.sonatype.darylhandley.fifteenfiveutils.service.AliasService
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveCloneService
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveService
import com.sonatype.darylhandley.fifteenfiveutils.service.TeamsService
import com.sonatype.darylhandley.fifteenfiveutils.service.UserService
import com.sonatype.darylhandley.fifteenfiveutils.ui.ShellUI

class ObjectivesTeamCloneCommand(
    private val objectiveService: ObjectiveService,
    private val objectiveCloneService: ObjectiveCloneService,
    private val teamsService: TeamsService,
    private val userService: UserService,
    private val aliasService: AliasService,
    private val ui: ShellUI
) : Command {

    override fun execute(tokens: List<String>) {
        if (tokens.size != 4) {
            ui.printError(getUsage())
            return
        }

        val objectiveId = tokens[2].toIntOrNull()
        if (objectiveId == null) {
            ui.printError("Invalid objective ID: ${tokens[2]}")
            return
        }

        val teamName = tokens[3]

        // Get source objective
        val sourceObjective = objectiveService.getObjective(objectiveId)

        // Get team members
        val teamMembers = teamsService.getTeamMembers(teamName)
        if (teamMembers == null) {
            ui.printError("Team '$teamName' not found.")
            return
        }

        if (teamMembers.isEmpty()) {
            ui.printError("Team '$teamName' has no members.")
            return
        }

        // Resolve team members to user IDs and check for duplicates
        val validUsers = mutableListOf<Triple<String, Int, String>>() // alias, userId, userName
        val skippedUsers = mutableListOf<Triple<String, String, String>>() // alias, userName, reason
        val unresolvedAliases = mutableListOf<String>()

        teamMembers.forEach { alias ->
            val userId = aliasService.resolveUserIdentifier(alias)
            if (userId == null) {
                unresolvedAliases.add(alias)
            } else {
                val userName = userService.getUserById(userId)?.fullName ?: "User ID $userId"

                // Check for duplicates
                try {
                    val existingObjectives = objectiveService.listObjectivesByUser(userId)
                    val duplicates = existingObjectives.filter {
                        it.description.equals(sourceObjective.description, ignoreCase = true)
                    }

                    if (duplicates.isNotEmpty()) {
                        skippedUsers.add(Triple(alias, userName, "duplicate found"))
                    } else {
                        validUsers.add(Triple(alias, userId, userName))
                    }
                } catch (e: Exception) {
                    skippedUsers.add(Triple(alias, userName, "error checking duplicates: ${e.message}"))
                }
            }
        }

        // Handle unresolved aliases
        if (unresolvedAliases.isNotEmpty()) {
            ui.printWarning("⚠️  Warning: Could not resolve aliases: ${unresolvedAliases.joinToString(", ")}")
        }

        // Check if we have any valid users to clone to
        if (validUsers.isEmpty()) {
            ui.printError("No valid users to clone to. All team members either have duplicates or couldn't be resolved.")
            return
        }

        // Show summarized preview
        val preview = buildTeamClonePreview(sourceObjective, teamName, validUsers, skippedUsers)
        ui.printCyan(preview)

        if (ui.confirm("")) {
            ui.printWarning("Cloning objective to team...")

            var successCount = 0
            for ((index, user) in validUsers.withIndex()) {
                val (alias, userId, userName) = user
                ui.printDim("Cloning to user ${index + 1}/${validUsers.size}: $userName...")

                try {
                    objectiveCloneService.cloneObjective(sourceObjective, userId)
                    ui.printSuccess("✅ Cloned successfully to $userName ($alias)")
                    successCount++
                } catch (e: Exception) {
                    ui.printError("❌ FAILED: Error cloning to $userName ($alias): ${e.message}")
                    // Fail-fast: stop on first error
                    break
                }
            }

            // Summary report
            val totalAttempted = validUsers.size
            val totalSkipped = skippedUsers.size + unresolvedAliases.size
            val totalTeamMembers = teamMembers.size

            ui.print("\n${"═".repeat(60)}")
            if (successCount == totalAttempted) {
                ui.printSuccess("✅ Successfully cloned to $successCount/$totalTeamMembers team members")
            } else {
                ui.printWarning("⚠️  Partially completed: $successCount/$totalAttempted successful clones")
            }

            if (totalSkipped > 0) {
                ui.printDim("   ($totalSkipped members skipped)")
            }
            ui.print("${"═".repeat(60)}")

        } else {
            ui.printWarning("Team clone cancelled.")
        }
    }

    private fun buildTeamClonePreview(
        sourceObjective: com.sonatype.darylhandley.fifteenfiveutils.model.Objective,
        teamName: String,
        validUsers: List<Triple<String, Int, String>>, // alias, userId, userName
        skippedUsers: List<Triple<String, String, String>> // alias, userName, reason
    ): String {
        val result = StringBuilder()

        result.append("═".repeat(80)).append("\n")
        result.append("📋 TEAM CLONE PREVIEW\n")
        result.append("═".repeat(80)).append("\n")

        result.append("📝 Objective: \"${sourceObjective.description}\" (ID: ${sourceObjective.id})\n")
        result.append("👤 From: ${sourceObjective.user.name}\n")
        result.append("📅 Period: ${sourceObjective.getFormattedStartDate()} → ${sourceObjective.getFormattedEndDate()}\n")

        if (sourceObjective.tags.isNotEmpty()) {
            result.append("🏷️  Tags: ${sourceObjective.getTagNames()}\n")
        }

        result.append("🔑 Key Results: ${sourceObjective.keyResults.size}\n")

        result.append("\n👥 Team: $teamName (${validUsers.size + skippedUsers.size} members)\n")

        if (validUsers.isNotEmpty()) {
            result.append("✅ Will clone to (${validUsers.size}):\n")
            validUsers.forEach { (alias, _, userName) ->
                result.append("   • $userName ($alias)\n")
            }
        }

        if (skippedUsers.isNotEmpty()) {
            result.append("⚠️  Skipping (${skippedUsers.size}):\n")
            skippedUsers.forEach { (alias, userName, reason) ->
                result.append("   • $userName ($alias) - $reason\n")
            }
        }

        result.append("\n").append("═".repeat(80)).append("\n")
        result.append("Clone to ${validUsers.size} users? (y/N): ")

        return result.toString()
    }

    override fun getUsage(): String {
        return "Usage: objectives teamclone <objective_id> <team_name>"
    }
}
