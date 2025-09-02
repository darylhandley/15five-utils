package com.sonatype.darylhandley.fifteenfiveutils.util

import com.sonatype.darylhandley.fifteenfiveutils.model.Objective
import com.sonatype.darylhandley.fifteenfiveutils.model.User
import de.vandermeer.asciitable.AsciiTable

object TableFormatter {

    fun formatUsersTable(users: List<User>): String {
        if (users.isEmpty()) {
            return "No users found."
        }

        val table = AsciiTable()
        table.addRule()
        table.addRow("User ID", "Full Name", "Title", "Active")
        table.addRule()

        users.forEach { user ->
            table.addRow(
                user.id.toString(),
                user.fullName,
                user.title ?: "N/A",
                if (user.isActive) "Yes" else "No"
            )
        }

        table.addRule()

        return table.render()
    }

    fun formatObjectivesList(objectives: List<Objective>): String {
        if (objectives.isEmpty()) {
            return "No objectives found."
        }

        val result = StringBuilder()
        result.append("Found ${objectives.size} objective(s):\n\n")

        objectives.forEachIndexed { index, objective ->
            result.append(formatSingleObjective(objective))

            // Add separator between objectives (but not after the last one)
            if (index < objectives.size - 1) {
                result.append("\n${"═".repeat(80)}\n\n")
            }
        }

        return result.toString()
    }

    fun formatSingleObjective(objective: Objective): String {
        val result = StringBuilder()

        // Objective header
        result.append("🎯 OBJECTIVE #${objective.id}\n")
        result.append("👤 User: ${objective.user.name}\n")
        result.append("📅 Period: ${objective.getFormattedStartDate()} → ${objective.getFormattedEndDate()}\n")
        result.append("📊 Progress: ${objective.percentage}%\n")
        result.append("🔗 Link: https://sonatype.15five.com/objectives/details/${objective.id}/\n")

        if (objective.tags.isNotEmpty()) {
            result.append("🏷️  Tags: ${objective.getTagNames()}\n")
        }

        result.append("\n📝 Description:\n")
        result.append("${objective.description}\n")

        // Key Results section
        if (objective.keyResults.isNotEmpty()) {
            result.append("\n🔑 KEY RESULTS:\n")
            objective.keyResults.forEachIndexed { index, keyResult ->
                result.append("  ${index + 1}. ${keyResult.description}\n")
                result.append("     👤 Owner: ${keyResult.owner.name}\n")
                result.append("     📈 Progress: ${keyResult.currentValueDisplay} (${keyResult.startValueDisplay} → ${keyResult.targetValueDisplay})\n")

                if (index < objective.keyResults.size - 1) {
                    result.append("\n")
                }
            }
        }

        return result.toString()
    }

    fun formatObjectivesCompactTable(objectives: List<Objective>): String {
        if (objectives.isEmpty()) {
            return "No objectives found."
        }

        val table = AsciiTable()
        table.addRule()
        table.addRow("User", "Description", "Key Results", "Link")
        table.addRule()

        objectives.forEach { objective ->
            val userName = objective.user.name
            val description = truncateDescription(objective.description, 50)
            val keyResults = formatKeyResultsForTable(objective.keyResults)
            val link = "https://sonatype.15five.com/objectives/details/${objective.id}/"

            table.addRow(userName, description, keyResults, link)
            table.addRule()
        }

        table.addRule()
        return table.render()
    }

    private fun truncateDescription(description: String, maxLength: Int): String {
        if (description.length <= maxLength) {
            return description
        }

        // Find the last space before maxLength to avoid breaking words
        val truncateAt = description.lastIndexOf(' ', maxLength)
        return if (truncateAt > 0) {
            "${description.substring(0, truncateAt)}..."
        } else {
            // If no space found, just truncate at maxLength (edge case)
            "${description.substring(0, maxLength)}..."
        }
    }

    private fun formatKeyResultsForTable(keyResults: List<com.sonatype.darylhandley.fifteenfiveutils.model.KeyResult>): String {
        if (keyResults.isEmpty()) {
            return "None"
        }

        return keyResults.joinToString("\n") { "• ${it.description}" }
    }
}