package com.sonatype.darylhandley.fifteenfiveutils.util

import com.sonatype.darylhandley.fifteenfiveutils.model.User
import com.sonatype.darylhandley.fifteenfiveutils.model.Objective
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
}