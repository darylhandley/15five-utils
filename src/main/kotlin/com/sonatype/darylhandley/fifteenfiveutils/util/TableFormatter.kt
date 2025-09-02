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

    private fun getKeyResultText(keyResults: List<com.sonatype.darylhandley.fifteenfiveutils.model.KeyResult>, index: Int, emptyText: String = ""): String {
        return if (index < keyResults.size) {
            val description = truncateAndWrapText(keyResults[index].description, 80)
            "• $description"
        } else {
            emptyText
        }
    }

    fun formatObjectivesCompactTable(objectives: List<Objective>, terminalWidth: Int = 120): String {
        if (objectives.isEmpty()) {
            return "No objectives found."
        }

        val table = AsciiTable()

        // Set overall table width
        table.context.setWidth(terminalWidth)

        table.addRule()
        table.addRow("Objective", "Key Results")
        table.addRule()

        objectives.forEach { objective ->
            // because of the way ascii table works we need to hack
            // the rows a bit
            val keyResults = objective.keyResults
            
            // add username and first KR
            val firstKR = getKeyResultText(keyResults, 0, "None")
            table.addRow("👤 ${objective.user.name}", firstKR)

            // add description and second KR
            val secondKR = getKeyResultText(keyResults, 1)
            val description = "📝 ${truncateAndWrapText(objective.description, 150)}"
            table.addRow(description, secondKR)

            // add link and 3rd KR
            val thirdKR = getKeyResultText(keyResults, 2)
            val link = "🔗 https://sonatype.15five.com/objectives/details/${objective.id}/"
            table.addRow(link, thirdKR)

            // add any remaining KRs
            if (keyResults.size > 3) {
                for (i in 3 until keyResults.size) {
                    table.addRow("", getKeyResultText(keyResults, i))
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