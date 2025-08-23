package com.sonatype.darylhandley.fifteenfiveutils.util

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
}