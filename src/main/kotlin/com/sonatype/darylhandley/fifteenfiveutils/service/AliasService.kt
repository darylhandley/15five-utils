package com.sonatype.darylhandley.fifteenfiveutils.service

import de.vandermeer.asciitable.AsciiTable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

class AliasService(private val userService: UserService? = null, private var teamsService: TeamsService? = null) {

    private val aliasFile: File

    init {
        val homeDir = System.getProperty("user.home")
        val configDir = File(homeDir, ".15fiveutils")

        // Ensure the config directory exists
        if (!configDir.exists()) {
            configDir.mkdirs()
        }

        aliasFile = File(configDir, "useraliases")
    }

    private fun loadAliases(): Properties {
        val properties = Properties()
        if (aliasFile.exists()) {
            FileInputStream(aliasFile).use { inputStream ->
                properties.load(inputStream)
            }
        }
        return properties
    }

    private fun saveAliases(properties: Properties) {
        FileOutputStream(aliasFile).use { outputStream ->
            properties.store(outputStream, "15Five Utils User Aliases")
        }
    }

    fun isValidAlias(alias: String): Boolean {
        return alias.isNotEmpty() && alias.matches(Regex("^[a-zA-Z0-9]+$"))
    }

    fun createAlias(alias: String, userId: Int): String {
        if (!isValidAlias(alias)) {
            return "Invalid alias '$alias'. Aliases must be alphanumeric only (no spaces or special characters)."
        }

        val properties = loadAliases()
        val lowerAlias = alias.lowercase()

        if (properties.containsKey(lowerAlias)) {
            return "Alias '$alias' already exists with value ${properties.getProperty(lowerAlias)}."
        }

        properties.setProperty(lowerAlias, userId.toString())
        saveAliases(properties)

        return "Alias '$alias' created for user ID $userId."
    }

    fun setTeamsService(teamsService: TeamsService) {
        this.teamsService = teamsService
    }

    fun removeAlias(alias: String): String {
        val properties = loadAliases()
        val lowerAlias = alias.lowercase()

        if (!properties.containsKey(lowerAlias)) {
            return "Alias '$alias' not found."
        }

        // Check if alias is used in any team
        teamsService?.let { teams ->
            if (teams.isAliasInAnyTeam(alias)) {
                val teamsUsingAlias = teams.getTeamsContainingAlias(alias)
                return "Cannot remove alias '$alias' - it is used in the following team(s): ${teamsUsingAlias.joinToString(", ")}. Remove it from teams first."
            }
        }

        val userId = properties.getProperty(lowerAlias)
        properties.remove(lowerAlias)
        saveAliases(properties)

        return "Alias '$alias' (user ID $userId) removed."
    }

    fun listAliases(): String {
        val properties = loadAliases()

        if (properties.isEmpty) {
            return "No aliases found."
        }

        return if (userService != null) {
            formatAliasesTable(properties)
        } else {
            // Fallback to simple format if no UserService available
            val result = StringBuilder()
            result.append("User aliases:\n")

            properties.keys.map { it.toString() }.sorted().forEach { alias ->
                val userId = properties.getProperty(alias)
                result.append("  $alias → $userId\n")
            }

            result.toString().trimEnd()
        }
    }

    private fun formatAliasesTable(properties: Properties): String {
        val table = AsciiTable()
        table.addRule()
        table.addRow("Alias", "User ID", "Username")
        table.addRule()

        val users = userService?.listAllUsers() ?: emptyList()
        val userMap = users.associateBy { it.id }

        properties.keys.map { it.toString() }.sorted().forEach { alias ->
            val userId = properties.getProperty(alias)?.toIntOrNull() ?: 0
            val username = userMap[userId]?.fullName ?: "Unknown User"

            table.addRow(alias, userId.toString(), username)
        }

        table.addRule()
        return table.render()
    }

    fun resolveUserIdentifier(input: String): Int? {
        // If it's already a number, return it
        input.toIntOrNull()?.let { return it }

        // Otherwise, try to resolve as alias
        val properties = loadAliases()
        val lowerInput = input.lowercase()

        return properties.getProperty(lowerInput)?.toIntOrNull()
    }

    fun isAlias(input: String): Boolean {
        return input.toIntOrNull() == null && loadAliases().containsKey(input.lowercase())
    }
}