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

    fun formatObjectivesCompactTable(objectives: List<Objective>, terminalWidth: Int = 120): String {
        if (objectives.isEmpty()) {
            return "No objectives found."
        }

        val table = AsciiTable()
        
        // Calculate column widths based on terminal width
        val userWidth = 12
        val linkWidth = 60
        val descriptionMaxLength = 100
        val tableOverhead = 8  // borders, padding, etc.
        val availableWidth = terminalWidth - userWidth - linkWidth - tableOverhead
        val descriptionWidth = (availableWidth * 0.4).toInt()
        val keyResultsWidth = (availableWidth * 0.4).toInt()

        // Set overall table width (AsciiTable will distribute columns automatically)
        table.context.setWidth(terminalWidth)

        table.addRule()
        table.addRow("User", "Description", "Key Results", "Link")
        table.addRule()

        objectives.forEach { objective ->
            val userName = truncateAndWrapText(objective.user.name, userWidth)
            val description = truncateAndWrapText(objective.description, descriptionMaxLength)
            val link = "https://sonatype.15five.com/objectives/details/${objective.id}/"

            if (objective.keyResults.isEmpty()) {
                table.addRow(userName, description, "None", link)
            } else {
                objective.keyResults.forEachIndexed { index, keyResult ->
                    val keyResultText = truncateAndWrapText("• ${keyResult.description}", keyResultsWidth)
                    
                    if (index == 0) {
                        // First row shows all info
                        table.addRow(userName, description, keyResultText, link)
                    } else {
                        // Subsequent rows show only the key result
                        table.addRow("", "", keyResultText, "")
                    }
                }
            }
            table.addRule()
        }

        return table.render()
    }

    private fun truncateAndWrapText(text: String, maxWidth: Int): String {
        if (text.length <= maxWidth) {
            return text
        }

        // First try to truncate at word boundary with "..."  
        val truncateAt = text.lastIndexOf(' ', maxWidth - 3) // Reserve 3 chars for "..."
        val truncated = if (truncateAt > 0) {
            "${text.substring(0, truncateAt)}..."
        } else {
            // If no space found, just truncate with "..."
            "${text.substring(0, maxWidth - 3)}..."
        }

        // If truncated version is still too long, let AsciiTable handle wrapping
        // (AsciiTable will wrap automatically when column width is set)
        return truncated
    }

}