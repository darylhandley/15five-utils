package com.sonatype.darylhandley.fifteenfiveutils.commands.objectives

import com.sonatype.darylhandley.fifteenfiveutils.commands.Command
import com.sonatype.darylhandley.fifteenfiveutils.service.AliasService
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveCloneService
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveService
import com.sonatype.darylhandley.fifteenfiveutils.service.UserService
import com.sonatype.darylhandley.fifteenfiveutils.ui.ShellUI
import com.sonatype.darylhandley.fifteenfiveutils.util.TableFormatter

class ObjectivesCloneCommand(
    private val objectiveService: ObjectiveService,
    private val objectiveCloneService: ObjectiveCloneService,
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

        val targetUserIdentifier = tokens[3]

        // Resolve target user
        val targetUserId = aliasService.resolveUserIdentifier(targetUserIdentifier)
        if (targetUserId == null) {
            ui.printError("Unknown user identifier: $targetUserIdentifier")
            return
        }

        // Get source objective
        val sourceObjective = objectiveService.getObjective(objectiveId)

        // Get target user name for display
        val targetUserName = userService.listAllUsers()
            .find { it.id == targetUserId }?.fullName ?: "User ID $targetUserId"

        // Check for duplicate objectives
        if (!checkForDuplicates(sourceObjective, targetUserId)) {
            return // User cancelled
        }

        // Show preview and get confirmation
        val preview = objectiveCloneService.buildClonePreview(sourceObjective, targetUserName, targetUserId)
        ui.printCyan(preview)

        if (ui.confirm("")) {
            ui.printWarning("Cloning objective...")
            val clonedObjectiveId = objectiveCloneService.cloneObjective(sourceObjective, targetUserId)
            ui.printSuccess("✅ Objective cloned successfully!")

            // Fetch and display the cloned objective
            val clonedObjective = objectiveService.getObjective(clonedObjectiveId)
            ui.printSuccess(TableFormatter.formatSingleObjective(clonedObjective))
        } else {
            ui.printWarning("Clone cancelled.")
        }
    }

    private fun checkForDuplicates(
        sourceObjective: com.sonatype.darylhandley.fifteenfiveutils.model.Objective,
        targetUserId: Int
    ): Boolean {
        val existingObjectives = objectiveService.listObjectivesByUser(targetUserId)
        val duplicates = existingObjectives.filter {
            it.description.equals(sourceObjective.description, ignoreCase = true)
        }

        if (duplicates.isEmpty()) return true

        ui.printWarning("⚠️  WARNING: Found ${duplicates.size} existing objective(s) with matching title:")
        duplicates.forEach { duplicate ->
            ui.printWarning("   • ID ${duplicate.id}: \"${duplicate.description}\" (${duplicate.getFormattedStartDate()} → ${duplicate.getFormattedEndDate()})")
        }

        return ui.confirm("Continue with clone anyway? (y/N): ")
    }

    override fun getUsage(): String {
        return "Usage: objectives clone <objective_id> <target_user_id_or_alias>"
    }
}
