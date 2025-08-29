package com.sonatype.darylhandley.fifteenfiveutils.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import de.vandermeer.asciitable.AsciiTable
import java.io.File

class TeamsService(private val userService: UserService, private val aliasService: AliasService) {

    private val teamsFile: File
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    init {
        val homeDir = System.getProperty("user.home")
        val configDir = File(homeDir, ".15fiveutils")

        // Ensure the config directory exists
        if (!configDir.exists()) {
            configDir.mkdirs()
        }

        teamsFile = File(configDir, "teams")
    }

    private fun loadTeams(): MutableMap<String, MutableList<String>> {
        if (!teamsFile.exists()) {
            return mutableMapOf()
        }

        return try {
            objectMapper.readValue<MutableMap<String, MutableList<String>>>(teamsFile)
        } catch (e: Exception) {
            // If file is corrupted, start with empty teams
            mutableMapOf()
        }
    }

    private fun saveTeams(teams: Map<String, List<String>>) {
        objectMapper.writeValue(teamsFile, teams)
    }

    fun isValidTeamName(teamName: String): Boolean {
        return teamName.isNotEmpty() && teamName.matches(Regex("^[a-zA-Z0-9]+$"))
    }

    fun createTeam(teamName: String): String {
        if (!isValidTeamName(teamName)) {
            return "Invalid team name '$teamName'. Team names must be alphanumeric only (no spaces or special characters)."
        }

        val teams = loadTeams()

        if (teams.containsKey(teamName)) {
            return "Team '$teamName' already exists."
        }

        teams[teamName] = mutableListOf()
        saveTeams(teams)

        return "Team '$teamName' created successfully."
    }

    fun addMemberToTeam(teamName: String, alias: String): String {
        val teams = loadTeams()

        if (!teams.containsKey(teamName)) {
            return "Team '$teamName' not found."
        }

        // Validate that the alias exists
        if (!aliasService.isAlias(alias)) {
            return "Alias '$alias' not found. Create the alias first before adding to team."
        }

        val teamMembers = teams[teamName]!!
        if (teamMembers.contains(alias.lowercase())) {
            return "Alias '$alias' is already in team '$teamName'."
        }

        teamMembers.add(alias.lowercase())
        saveTeams(teams)

        return "Added alias '$alias' to team '$teamName'."
    }

    fun removeMemberFromTeam(teamName: String, alias: String): String {
        val teams = loadTeams()

        if (!teams.containsKey(teamName)) {
            return "Team '$teamName' not found."
        }

        val teamMembers = teams[teamName]!!
        if (!teamMembers.remove(alias.lowercase())) {
            return "Alias '$alias' not found in team '$teamName'."
        }

        saveTeams(teams)
        return "Removed alias '$alias' from team '$teamName'."
    }

    fun getTeam(teamName: String): String {
        val teams = loadTeams()

        if (!teams.containsKey(teamName)) {
            return "Team '$teamName' not found."
        }

        val teamMembers = teams[teamName]!!
        if (teamMembers.isEmpty()) {
            return "Team '$teamName' has no members."
        }

        return formatTeamMembersTable(teamMembers)
    }

    private fun formatTeamMembersTable(members: List<String>): String {
        val table = AsciiTable()
        table.addRule()
        table.addRow("Alias", "User ID", "Full Name")
        table.addRule()

        val users = userService.listAllUsers()
        val userMap = users.associateBy { it.id }

        members.sorted().forEach { alias ->
            val userId = aliasService.resolveUserIdentifier(alias)
            val userName = if (userId != null) {
                userMap[userId]?.fullName ?: "Unknown User"
            } else {
                "Alias Not Found"
            }

            table.addRow(alias, userId?.toString() ?: "N/A", userName)
        }

        table.addRule()
        return table.render()
    }

    fun listTeams(): String {
        val teams = loadTeams()

        if (teams.isEmpty()) {
            return "No teams found."
        }

        return formatAllTeamsTable(teams)
    }

    private fun formatAllTeamsTable(teams: Map<String, List<String>>): String {
        val table = AsciiTable()
        table.addRule()
        table.addRow("Team Name", "Alias", "User Name")
        table.addRule()

        val users = userService.listAllUsers()
        val userMap = users.associateBy { it.id }

        teams.keys.sorted().forEach { teamName ->
            val aliases = teams[teamName]!!.sorted()
            
            if (aliases.isEmpty()) {
                table.addRow(teamName, "None", "None")
            } else {
                aliases.forEachIndexed { index, alias ->
                    val userId = aliasService.resolveUserIdentifier(alias)
                    val userName = if (userId != null) {
                        userMap[userId]?.fullName ?: "Unknown"
                    } else {
                        "NotFound"
                    }
                    
                    // Show team name only in first row, empty for subsequent rows
                    val teamNameCell = if (index == 0) teamName else ""
                    table.addRow(teamNameCell, alias, userName)
                }
            }
            table.addRule()
        }

//        table.addRule()
        return table.render()
    }

    fun isAliasInAnyTeam(alias: String): Boolean {
        val teams = loadTeams()
        return teams.values.any { it.contains(alias.lowercase()) }
    }

    fun getTeamsContainingAlias(alias: String): List<String> {
        val teams = loadTeams()
        return teams.filterValues { it.contains(alias.lowercase()) }.keys.toList()
    }

    fun deleteTeam(teamName: String): String {
        val teams = loadTeams()

        if (!teams.containsKey(teamName)) {
            return "Team '$teamName' not found."
        }

        teams.remove(teamName)
        saveTeams(teams)

        return "Team '$teamName' deleted successfully. User aliases remain intact."
    }
}