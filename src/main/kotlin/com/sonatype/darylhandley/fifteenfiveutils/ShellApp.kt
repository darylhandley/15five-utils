package com.sonatype.darylhandley.fifteenfiveutils

import com.sonatype.darylhandley.fifteenfiveutils.service.AliasService
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveCloneService
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveService
import com.sonatype.darylhandley.fifteenfiveutils.service.TeamsService
import com.sonatype.darylhandley.fifteenfiveutils.service.UserService
import com.sonatype.darylhandley.fifteenfiveutils.util.ConfigLoader
import com.sonatype.darylhandley.fifteenfiveutils.util.TableFormatter
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import java.io.File

object Colors {
    const val RESET = "\u001B[0m"
    const val BLUE = "\u001B[34m"
    const val GREEN = "\u001B[32m"
    const val YELLOW = "\u001B[33m"
    const val RED = "\u001B[31m"
    const val CYAN = "\u001B[36m"
    const val BOLD = "\u001B[1m"
    const val DIM = "\u001B[2m"
}

class ShellApp {
    private lateinit var userService: UserService
    private lateinit var objectiveService: ObjectiveService
    private lateinit var objectiveCloneService: ObjectiveCloneService
    private lateinit var aliasService: AliasService
    private lateinit var teamsService: TeamsService
    private lateinit var lineReader: LineReader
    private lateinit var terminal: Terminal
    private fun tokenizeInput(input: String): List<String> {
        return input.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    }

    private fun getTerminalWidth(): Int {
        return try {
            terminal.width
        } catch (e: Exception) {
            120 // Fallback width if detection fails
        }
    }

    fun run() {
        println("${Colors.BOLD}${Colors.CYAN}15Five Utils Shell${Colors.RESET} - Type '${Colors.YELLOW}help${Colors.RESET}' or '${Colors.YELLOW}?${Colors.RESET}' for commands or '${Colors.YELLOW}quit/exit/q${Colors.RESET}' to exit")
        println("${Colors.DIM}${"─".repeat(60)}${Colors.RESET}")

        val sessionId = try {
            ConfigLoader.getSessionId()
        } catch (e: Exception) {
            println("${Colors.RED}Configuration error: ${e.message}${Colors.RESET}")
            return
        }

        userService = UserService(sessionId)
        objectiveService = ObjectiveService(sessionId)
        objectiveCloneService = ObjectiveCloneService(sessionId)
        aliasService = AliasService(userService)
        teamsService = TeamsService(userService, aliasService)
        
        // Set up circular reference for alias protection
        aliasService.setTeamsService(teamsService)

        // Set up JLine3 terminal and line reader
        terminal = TerminalBuilder.builder()
            .system(true)
            .build()

        // Set up command history file
        val homeDir = System.getProperty("user.home")
        val configDir = File(homeDir, ".15fiveutils")
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
        val historyFile = File(configDir, "history")

        // Set up tab completion for available commands
        val completer = StringsCompleter(
            "help",
            "?",
            "quit",
            "exit",
            "q",
            "users list",
            "users refresh",
            "objectives list",
            "objectives list -verbose",
            "objectives list -v",
            "objectives listbyuser",
            "objectives listbyuser -verbose",
            "objectives listbyuser -v",
            "objectives listbyteam",
            "objectives listbyteam -verbose",
            "objectives listbyteam -v",
            "objectives get",
            "objectives clone",
            "objectives teamclone",
            "useralias create",
            "useralias createbysearch",
            "useralias list",
            "useralias delete",
            "teams create",
            "teams get",
            "teams add",
            "teams remove",
            "teams delete",
            "teams list"
        )

        lineReader = LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(completer)
            .history(DefaultHistory())
            .variable(LineReader.HISTORY_FILE, historyFile.absolutePath)
            .build()

        var running = true

        while (running) {
            try {
                val prompt = "${Colors.BLUE}15five${Colors.RESET}${Colors.DIM}>${Colors.RESET} "
                val input = lineReader.readLine(prompt).trim()

                if (input.isEmpty()) continue

                val tokens = tokenizeInput(input)
                when (tokens.firstOrNull()?.lowercase()) {
                    "users" -> handleUsersCommand(tokens)
                    "objectives" -> handleObjectivesCommand(tokens)
                    "useralias" -> handleUserAliasCommand(tokens)
                    "teams" -> handleTeamsCommand(tokens)
                    "help", "?" -> handleHelpCommand()
                    "quit", "exit", "q" -> {
                        println("${Colors.YELLOW}Goodbye!${Colors.RESET}")
                        running = false
                    }

                    else -> {
                        println("${Colors.RED}Unknown command: $input. Type '${Colors.YELLOW}help${Colors.RESET}' or '${Colors.YELLOW}?${Colors.RESET}' for available commands.${Colors.RESET}")
                    }
                }

                if (running) {
                    println("${Colors.DIM}${"─".repeat(30)}${Colors.RESET}")
                }
            } catch (e: org.jline.reader.UserInterruptException) {
                // Handle Ctrl+C gracefully
                e.printStackTrace()
                println("\n${Colors.YELLOW}Goodbye!${Colors.RESET}")
                break
            } catch (e: org.jline.reader.EndOfFileException) {
                // Handle EOF (Ctrl+D) gracefully
                e.printStackTrace()
                println("\n${Colors.YELLOW}Goodbye!${Colors.RESET}")
                break
            }
        }

        terminal.close()

        // added for fast shutdown since okhttp client wants to hold onto connections
        System.exit(0)
    }

    private fun handleUsersCommand(tokens: List<String>) {
        if (tokens.size == 2 && tokens[1].lowercase() == "list") {
            try {
                val users = userService.listAllUsers()
                println("${Colors.GREEN}${TableFormatter.formatUsersTable(users)}${Colors.RESET}")
            } catch (e: Exception) {
                println("${Colors.RED}Error fetching users: ${e.message}${Colors.RESET}")
                e.printStackTrace()
            }
        } else if (tokens.size == 2 && tokens[1].lowercase() == "refresh") {
            try {
                val result = userService.refreshUserCache()
                println("${Colors.GREEN}$result${Colors.RESET}")
            } catch (e: Exception) {
                println("${Colors.RED}Error refreshing user cache: ${e.message}${Colors.RESET}")
                e.printStackTrace()
            }
        } else if (tokens.size > 2 && tokens[1].lowercase() == "list") {
            try {
                val searchTerm = tokens.drop(2).joinToString(" ")
                val users = userService.searchUsers(searchTerm)
                println("${Colors.GREEN}${TableFormatter.formatUsersTable(users)}${Colors.RESET}")
            } catch (e: Exception) {
                println("${Colors.RED}Error searching users: ${e.message}${Colors.RESET}")
                e.printStackTrace()
            }
        } else {
            println("${Colors.RED}Usage: users <list|refresh> [search_term]${Colors.RESET}")
        }
    }

    private fun handleObjectivesCommand(tokens: List<String>) {
        if (tokens.size < 2) {
            println("${Colors.RED}Usage: objectives <list|listbyuser|listbyteam|get|clone> [args...]${Colors.RESET}")
            return
        }

        when (tokens[1].lowercase()) {
            "list" -> handleObjectivesListCommand(tokens)
            "listbyuser" -> handleObjectivesListByUserCommand(tokens)
            "listbyteam" -> handleObjectivesListByTeamCommand(tokens)
            "get" -> handleObjectivesGetCommand(tokens)
            "clone" -> handleObjectivesCloneCommand(tokens)
            "teamclone" -> handleObjectivesTeamCloneCommand(tokens)
            else -> {
                println("${Colors.RED}Unknown objectives subcommand: ${tokens[1]}${Colors.RESET}")
                println("${Colors.RED}Usage: objectives <list|listbyuser|listbyteam|get|clone|teamclone> [args...]${Colors.RESET}")
            }
        }
    }

    private fun handleObjectivesListCommand(tokens: List<String>) {
        // Check for verbose flag
        val isVerbose = tokens.lastOrNull()?.lowercase() in listOf("-verbose", "-v")
        val effectiveTokens = if (isVerbose) tokens.dropLast(1) else tokens

        if (effectiveTokens.size == 2) {
            try {
                val objectives = objectiveService.listObjectives(100)
                val formatted = if (isVerbose) {
                    TableFormatter.formatObjectivesList(objectives)
                } else {
                    TableFormatter.formatObjectivesCompactTable(objectives, getTerminalWidth())
                }
                println("${Colors.GREEN}$formatted${Colors.RESET}")
            } catch (e: Exception) {
                println("${Colors.RED}Error fetching objectives: ${e.message}${Colors.RESET}")
                e.printStackTrace()
            }
        } else if (effectiveTokens.size == 3) {
            try {
                val limit = effectiveTokens[2].toInt()
                val objectives = objectiveService.listObjectives(limit)
                val formatted = if (isVerbose) {
                    TableFormatter.formatObjectivesList(objectives)
                } else {
                    TableFormatter.formatObjectivesCompactTable(objectives, getTerminalWidth())
                }
                println("${Colors.GREEN}$formatted${Colors.RESET}")
            } catch (e: NumberFormatException) {
                println("${Colors.RED}Invalid number: ${effectiveTokens[2]}${Colors.RESET}")
            } catch (e: Exception) {
                println("${Colors.RED}Error fetching objectives: ${e.message}${Colors.RESET}")
                e.printStackTrace()
            }
        } else {
            println("${Colors.RED}Usage: objectives list [limit] [-verbose|-v]${Colors.RESET}")
        }
    }

    private fun handleObjectivesListByUserCommand(tokens: List<String>) {
        // Check for verbose flag
        val isVerbose = tokens.lastOrNull()?.lowercase() in listOf("-verbose", "-v")
        val effectiveTokens = if (isVerbose) tokens.dropLast(1) else tokens

        if (effectiveTokens.size != 3) {
            println("${Colors.RED}Usage: objectives listbyuser <userid or alias> [-verbose|-v]${Colors.RESET}")
        } else {
            val userIdentifier = effectiveTokens[2]
            try {
                val userId = aliasService.resolveUserIdentifier(userIdentifier)
                if (userId == null) {
                    println("${Colors.RED}Unknown user identifier: $userIdentifier${Colors.RESET}")
                } else {
                    val objectives = objectiveService.listObjectivesByUser(userId)
                    val formatted = if (isVerbose) {
                        TableFormatter.formatObjectivesList(objectives)
                    } else {
                        TableFormatter.formatObjectivesCompactTable(objectives, getTerminalWidth())
                    }
                    println("${Colors.GREEN}$formatted${Colors.RESET}")
                }
            } catch (e: Exception) {
                println("${Colors.RED}Error fetching objectives: ${e.message}${Colors.RESET}")
                e.printStackTrace()
            }
        }
    }

    private fun handleObjectivesListByTeamCommand(tokens: List<String>) {
        // Check for verbose flag
        val isVerbose = tokens.lastOrNull()?.lowercase() in listOf("-verbose", "-v")
        val effectiveTokens = if (isVerbose) tokens.dropLast(1) else tokens

        if (effectiveTokens.size != 3) {
            println("${Colors.RED}Usage: objectives listbyteam <team_name> [-verbose|-v]${Colors.RESET}")
            return
        }

        val teamName = effectiveTokens[2]
        try {
            // Get team members
            val teamMembers = teamsService.getTeamMembers(teamName)
            if (teamMembers == null) {
                println("${Colors.RED}Team '$teamName' not found.${Colors.RESET}")
                return
            }

            if (teamMembers.isEmpty()) {
                println("${Colors.YELLOW}Team '$teamName' has no members.${Colors.RESET}")
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
                println("${Colors.YELLOW}Warning: $warning${Colors.RESET}")
            }

            // Sort objectives by username, then objectiveId
            val sortedObjectives = allObjectives.sortedWith(compareBy({ it.user.name }, { it.id }))

            // Format output
            val formatted = if (isVerbose) {
                TableFormatter.formatObjectivesList(sortedObjectives)
            } else {
                TableFormatter.formatObjectivesCompactTable(sortedObjectives, getTerminalWidth())
            }

            println("${Colors.GREEN}$formatted${Colors.RESET}")

            // Show users with no objectives
            if (usersWithNoObjectives.isNotEmpty()) {
                println("${Colors.YELLOW}Users with no objectives:${Colors.RESET}")
                usersWithNoObjectives.sorted().forEach { userName ->
                    println("${Colors.YELLOW}  • $userName${Colors.RESET}")
                }
            }

        } catch (e: Exception) {
            println("${Colors.RED}Error fetching team objectives: ${e.message}${Colors.RESET}")
            e.printStackTrace()
        }
    }

    private fun handleObjectivesGetCommand(tokens: List<String>) {
        if (tokens.size != 3) {
            println("${Colors.RED}Usage: objectives get <objective_id>${Colors.RESET}")
        } else {
            try {
                val objectiveId = tokens[2].toInt()
                val objective = objectiveService.getObjective(objectiveId)
                println("${Colors.GREEN}${TableFormatter.formatSingleObjective(objective)}${Colors.RESET}")
            } catch (e: NumberFormatException) {
                println("${Colors.RED}Invalid objective ID: ${tokens[2]}${Colors.RESET}")
            } catch (e: Exception) {
                println("${Colors.RED}Error fetching objective: ${e.message}${Colors.RESET}")
                e.printStackTrace()
            }
        }
    }

    private fun handleObjectivesCloneCommand(tokens: List<String>) {
        if (tokens.size != 4) {
            println("${Colors.RED}Usage: objectives clone <objective_id> <target_user_id_or_alias>${Colors.RESET}")
        } else {
            try {
                val objectiveId = tokens[2].toInt()
                val targetUserIdentifier = tokens[3]

                // Resolve target user
                val targetUserId = aliasService.resolveUserIdentifier(targetUserIdentifier)
                if (targetUserId == null) {
                    println("${Colors.RED}Unknown user identifier: $targetUserIdentifier${Colors.RESET}")
                    return
                }

                // Get source objective
                val sourceObjective = objectiveService.getObjective(objectiveId)

                // Get target user name for display
                val targetUserName = userService.listAllUsers()
                    .find { it.id == targetUserId }?.fullName ?: "User ID $targetUserId"

                // Check for duplicate objectives
                val existingObjectives = objectiveService.listObjectivesByUser(targetUserId)
                val duplicates = existingObjectives.filter {
                    it.description.equals(sourceObjective.description, ignoreCase = true)
                }

                if (duplicates.isNotEmpty()) {
                    println("${Colors.YELLOW}⚠️  WARNING: Found ${duplicates.size} existing objective(s) with matching title:${Colors.RESET}")
                    duplicates.forEach { duplicate ->
                        println("${Colors.YELLOW}   • ID ${duplicate.id}: \"${duplicate.description}\" (${duplicate.getFormattedStartDate()} → ${duplicate.getFormattedEndDate()})${Colors.RESET}")
                    }
                    print("${Colors.YELLOW}Continue with clone anyway? (y/N): ${Colors.RESET}")
                    val duplicateConfirmation = lineReader.readLine()
                    if (duplicateConfirmation.lowercase() != "y" && duplicateConfirmation.lowercase() != "yes") {
                        println("${Colors.YELLOW}Clone cancelled due to duplicate objectives.${Colors.RESET}")
                        return
                    }
                    println()
                }

                // Show preview and get confirmation
                val preview = objectiveCloneService.buildClonePreview(sourceObjective, targetUserName, targetUserId)
                print("${Colors.CYAN}$preview${Colors.RESET}")

                val confirmation = lineReader.readLine()
                if (confirmation.lowercase() == "y" || confirmation.lowercase() == "yes") {
                    println("${Colors.YELLOW}Cloning objective...${Colors.RESET}")
                    val clonedObjectiveId = objectiveCloneService.cloneObjective(sourceObjective, targetUserId)
                    println("${Colors.GREEN}✅ Objective cloned successfully!${Colors.RESET}")

                    // Fetch and display the cloned objective
                    val clonedObjective = objectiveService.getObjective(clonedObjectiveId)
                    println("${Colors.GREEN}${TableFormatter.formatSingleObjective(clonedObjective)}${Colors.RESET}")
                } else {
                    println("${Colors.YELLOW}Clone cancelled.${Colors.RESET}")
                }
            } catch (e: NumberFormatException) {
                println("${Colors.RED}Invalid objective ID: ${tokens[2]}${Colors.RESET}")
            } catch (e: Exception) {
                println("${Colors.RED}Error cloning objective: ${e.message}${Colors.RESET}")
                e.printStackTrace()
            }
        }
    }

    private fun handleObjectivesTeamCloneCommand(tokens: List<String>) {
        if (tokens.size != 4) {
            println("${Colors.RED}Usage: objectives teamclone <objective_id> <team_name>${Colors.RESET}")
        } else {
            try {
                val objectiveId = tokens[2].toInt()
                val teamName = tokens[3]

                // Get source objective
                val sourceObjective = objectiveService.getObjective(objectiveId)

                // Get team members
                val teamMembers = teamsService.getTeamMembers(teamName)
                if (teamMembers == null) {
                    println("${Colors.RED}Team '$teamName' not found.${Colors.RESET}")
                    return
                }

                if (teamMembers.isEmpty()) {
                    println("${Colors.RED}Team '$teamName' has no members.${Colors.RESET}")
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
                    println("${Colors.YELLOW}⚠️  Warning: Could not resolve aliases: ${unresolvedAliases.joinToString(", ")}${Colors.RESET}")
                }

                // Check if we have any valid users to clone to
                if (validUsers.isEmpty()) {
                    println("${Colors.RED}No valid users to clone to. All team members either have duplicates or couldn't be resolved.${Colors.RESET}")
                    return
                }

                // Show summarized preview
                val preview = buildTeamClonePreview(sourceObjective, teamName, validUsers, skippedUsers)
                print("${Colors.CYAN}$preview${Colors.RESET}")

                val confirmation = lineReader.readLine()
                if (confirmation.lowercase() == "y" || confirmation.lowercase() == "yes") {
                    println("${Colors.YELLOW}Cloning objective to team...${Colors.RESET}")

                    var successCount = 0
                    for ((index, user) in validUsers.withIndex()) {
                        val (alias, userId, userName) = user
                        println("${Colors.DIM}Cloning to user ${index + 1}/${validUsers.size}: $userName...${Colors.RESET}")

                        try {
                            objectiveCloneService.cloneObjective(sourceObjective, userId)
                            println("${Colors.GREEN}✅ Cloned successfully to $userName ($alias)${Colors.RESET}")
                            successCount++
                        } catch (e: Exception) {
                            println("${Colors.RED}❌ FAILED: Error cloning to $userName ($alias): ${e.message}${Colors.RESET}")
                            // Fail-fast: stop on first error
                            break
                        }
                    }

                    // Summary report
                    val totalAttempted = validUsers.size
                    val totalSkipped = skippedUsers.size + unresolvedAliases.size
                    val totalTeamMembers = teamMembers.size

                    println("\n${Colors.CYAN}${"═".repeat(60)}${Colors.RESET}")
                    if (successCount == totalAttempted) {
                        println("${Colors.GREEN}✅ Successfully cloned to $successCount/$totalTeamMembers team members${Colors.RESET}")
                    } else {
                        println("${Colors.YELLOW}⚠️  Partially completed: $successCount/$totalAttempted successful clones${Colors.RESET}")
                    }

                    if (totalSkipped > 0) {
                        println("${Colors.DIM}   ($totalSkipped members skipped)${Colors.RESET}")
                    }
                    println("${Colors.CYAN}${"═".repeat(60)}${Colors.RESET}")

                } else {
                    println("${Colors.YELLOW}Team clone cancelled.${Colors.RESET}")
                }

            } catch (e: NumberFormatException) {
                println("${Colors.RED}Invalid objective ID: ${tokens[2]}${Colors.RESET}")
            } catch (e: Exception) {
                println("${Colors.RED}Error cloning objective to team: ${e.message}${Colors.RESET}")
                e.printStackTrace()
            }
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

    private fun handleUserAliasCommand(tokens: List<String>) {
        if (tokens.size < 2) {
            println("${Colors.RED}Usage: useralias <create|createbysearch|list|delete> [args...]${Colors.RESET}")
            return
        }

        when (tokens[1].lowercase()) {
            "create" -> {
                if (tokens.size != 4) {
                    println("${Colors.RED}Usage: useralias create <alias> <userid>${Colors.RESET}")
                } else {
                    try {
                        val alias = tokens[2]
                        val userId = tokens[3].toInt()
                        val result = aliasService.createAlias(alias, userId)
                        println("${Colors.GREEN}$result${Colors.RESET}")
                    } catch (e: NumberFormatException) {
                        println("${Colors.RED}Invalid user ID: ${tokens[3]}${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error creating alias: ${e.message}${Colors.RESET}")
                    }
                }
            }

            "list" -> {
                if (tokens.size != 2) {
                    println("${Colors.RED}Usage: useralias list${Colors.RESET}")
                } else {
                    try {
                        val result = aliasService.listAliases()
                        println("${Colors.GREEN}$result${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error listing aliases: ${e.message}${Colors.RESET}")
                    }
                }
            }

            "delete" -> {
                if (tokens.size != 3) {
                    println("${Colors.RED}Usage: useralias delete <alias>${Colors.RESET}")
                } else {
                    val alias = tokens[2]
                    try {
                        val result = aliasService.deleteAlias(alias)
                        println("${Colors.GREEN}$result${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error deleting alias: ${e.message}${Colors.RESET}")
                    }
                }
            }

            "createbysearch" -> {
                if (tokens.size < 4) {
                    println("${Colors.RED}Usage: useralias createbysearch <alias> <search_term>${Colors.RESET}")
                } else {
                    try {
                        val alias = tokens[2]
                        val searchTerm = tokens.drop(3).joinToString(" ")
                        
                        val users = aliasService.searchUsersForAlias(searchTerm)
                        
                        when {
                            users == null -> {
                                println("${Colors.RED}Error: User service not available${Colors.RESET}")
                            }
                            users.isEmpty() -> {
                                println("${Colors.RED}No users found matching '$searchTerm'${Colors.RESET}")
                            }
                            users.size == 1 -> {
                                val user = users[0]
                                print("${Colors.CYAN}Create alias '$alias' for user '${user.fullName}' (ID: ${user.id})? (y/N): ${Colors.RESET}")
                                val confirmation = lineReader.readLine()
                                
                                if (confirmation.lowercase() == "y" || confirmation.lowercase() == "yes") {
                                    val result = aliasService.createAlias(alias, user.id)
                                    println("${Colors.GREEN}$result${Colors.RESET}")
                                } else {
                                    println("${Colors.YELLOW}Alias creation cancelled.${Colors.RESET}")
                                }
                            }
                            else -> {
                                println("${Colors.RED}Multiple users found matching '$searchTerm':${Colors.RESET}")
                                users.forEach { user ->
                                    println("${Colors.RED}  • ${user.fullName} (ID: ${user.id})${Colors.RESET}")
                                }
                                println("${Colors.RED}Please make your search more specific.${Colors.RESET}")
                            }
                        }
                    } catch (e: Exception) {
                        println("${Colors.RED}Error searching for users: ${e.message}${Colors.RESET}")
                    }
                }
            }

            else -> {
                println("${Colors.RED}Unknown useralias subcommand: ${tokens[1]}${Colors.RESET}")
                println("${Colors.RED}Usage: useralias <create|createbysearch|list|delete> [args...]${Colors.RESET}")
            }
        }
    }

    private fun handleTeamsCommand(tokens: List<String>) {
        if (tokens.size < 2) {
            println("${Colors.RED}Usage: teams <create|get|add|remove|delete|list> [args...]${Colors.RESET}")
            return
        }

        when (tokens[1].lowercase()) {
            "create" -> {
                if (tokens.size != 3) {
                    println("${Colors.RED}Usage: teams create <team_name>${Colors.RESET}")
                } else {
                    try {
                        val result = teamsService.createTeam(tokens[2])
                        println("${Colors.GREEN}$result${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error creating team: ${e.message}${Colors.RESET}")
                    }
                }
            }

            "get" -> {
                if (tokens.size != 3) {
                    println("${Colors.RED}Usage: teams get <team_name>${Colors.RESET}")
                } else {
                    try {
                        val result = teamsService.getTeam(tokens[2])
                        println("${Colors.GREEN}$result${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error getting team: ${e.message}${Colors.RESET}")
                    }
                }
            }

            "add" -> {
                if (tokens.size != 4) {
                    println("${Colors.RED}Usage: teams add <team_name> <alias>${Colors.RESET}")
                } else {
                    try {
                        val result = teamsService.addMemberToTeam(tokens[2], tokens[3])
                        println("${Colors.GREEN}$result${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error adding member to team: ${e.message}${Colors.RESET}")
                    }
                }
            }

            "remove" -> {
                if (tokens.size != 4) {
                    println("${Colors.RED}Usage: teams remove <team_name> <alias>${Colors.RESET}")
                } else {
                    try {
                        val result = teamsService.removeMemberFromTeam(tokens[2], tokens[3])
                        println("${Colors.GREEN}$result${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error removing member from team: ${e.message}${Colors.RESET}")
                    }
                }
            }

            "list" -> {
                if (tokens.size != 2) {
                    println("${Colors.RED}Usage: teams list${Colors.RESET}")
                } else {
                    try {
                        val result = teamsService.listTeams()
                        println("${Colors.GREEN}$result${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error listing teams: ${e.message}${Colors.RESET}")
                    }
                }
            }

            "delete" -> {
                if (tokens.size != 3) {
                    println("${Colors.RED}Usage: teams delete <team_name>${Colors.RESET}")
                } else {
                    try {
                        val result = teamsService.deleteTeam(tokens[2])
                        println("${Colors.GREEN}$result${Colors.RESET}")
                    } catch (e: Exception) {
                        println("${Colors.RED}Error deleting team: ${e.message}${Colors.RESET}")
                    }
                }
            }

            else -> {
                println("${Colors.RED}Unknown teams subcommand: ${tokens[1]}${Colors.RESET}")
                println("${Colors.RED}Usage: teams <create|get|add|remove|delete|list> [args...]${Colors.RESET}")
            }
        }
    }

    private fun handleHelpCommand() {
        println("${Colors.BOLD}${Colors.CYAN}General:${Colors.RESET}")
        println("  ${Colors.YELLOW}help/?${Colors.RESET}${" ".repeat(37)} - Show this help")
        println("  ${Colors.YELLOW}quit/exit/q${Colors.RESET}${" ".repeat(32)} - Exit the shell")
        println("${Colors.BOLD}${Colors.CYAN}Users:${Colors.RESET}")
        println("  ${Colors.YELLOW}users list${Colors.RESET}${" ".repeat(33)} - List all users")
        println("  ${Colors.YELLOW}users list${Colors.RESET} ${Colors.DIM}<search>${Colors.RESET}${" ".repeat(25)} - Search for users by name")
        println("  ${Colors.YELLOW}users refresh${Colors.RESET}${" ".repeat(30)} - Refresh user cache from API")
        println()
        println("${Colors.BOLD}${Colors.CYAN}Objectives:${Colors.RESET}")
        println("  ${Colors.YELLOW}objectives list${Colors.RESET} ${Colors.DIM}[-v]${Colors.RESET}${" ".repeat(25)} - List top 100 objectives")
        println("  ${Colors.YELLOW}objectives list${Colors.RESET} ${Colors.DIM}<limit> [-v]${Colors.RESET}${" ".repeat(17)} - List objectives (custom limit)")
        println("  ${Colors.YELLOW}objectives listbyuser${Colors.RESET} ${Colors.DIM}<id> [-v]${Colors.RESET}${" ".repeat(14)} - List objectives for user ID or alias")
        println("  ${Colors.YELLOW}objectives listbyteam${Colors.RESET} ${Colors.DIM}<team> [-v]${Colors.RESET}${" ".repeat(12)} - List objectives for team members")
        println("    ${Colors.DIM}Use -verbose or -v for detailed view instead of compact table${Colors.RESET}")
        println("  ${Colors.YELLOW}objectives get${Colors.RESET} ${Colors.DIM}<id>${Colors.RESET}${" ".repeat(26)} - Get single objective by ID")
        println("  ${Colors.YELLOW}objectives clone${Colors.RESET} ${Colors.DIM}<id> <user>${Colors.RESET}${" ".repeat(17)} - Clone objective to another user")
        println("  ${Colors.YELLOW}objectives teamclone${Colors.RESET} ${Colors.DIM}<id> <team>${Colors.RESET}${" ".repeat(13)} - Clone objective to all team members")
        println()
        println("${Colors.BOLD}${Colors.CYAN}User Aliases:${Colors.RESET}")
        println("  ${Colors.YELLOW}useralias create${Colors.RESET} ${Colors.DIM}<alias> <userid>${Colors.RESET}${" ".repeat(12)} - Create user alias")
        println("  ${Colors.YELLOW}useralias createbysearch${Colors.RESET} ${Colors.DIM}<alias> <search>${Colors.RESET}${" ".repeat(4)} - Create alias by searching for user")
        println("  ${Colors.YELLOW}useralias list${Colors.RESET}${" ".repeat(29)} - List all user aliases")
        println("  ${Colors.YELLOW}useralias delete${Colors.RESET} ${Colors.DIM}<alias>${Colors.RESET}${" ".repeat(22)} - Delete user alias")
        println()
        println("${Colors.BOLD}${Colors.CYAN}Teams:${Colors.RESET}")
        println("  ${Colors.YELLOW}teams create${Colors.RESET} ${Colors.DIM}<name>${Colors.RESET}${" ".repeat(27)} - Create a new team")
        println("  ${Colors.YELLOW}teams get${Colors.RESET} ${Colors.DIM}<name>${Colors.RESET}${" ".repeat(30)} - Show team members with details")
        println("  ${Colors.YELLOW}teams add${Colors.RESET} ${Colors.DIM}<team> <alias>${Colors.RESET}${" ".repeat(22)} - Add alias to team")
        println("  ${Colors.YELLOW}teams remove${Colors.RESET} ${Colors.DIM}<team> <alias>${Colors.RESET}${" ".repeat(19)} - Remove alias from team")
        println("  ${Colors.YELLOW}teams delete${Colors.RESET} ${Colors.DIM}<name>${Colors.RESET}${" ".repeat(27)} - Delete team (keeps aliases)")
        println("  ${Colors.YELLOW}teams list${Colors.RESET}${" ".repeat(33)} - List all teams")
        println()
    }
}

fun main() {
    ShellApp().run()
}