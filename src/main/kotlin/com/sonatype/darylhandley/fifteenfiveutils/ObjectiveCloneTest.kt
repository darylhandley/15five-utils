package com.sonatype.darylhandley.fifteenfiveutils

import com.sonatype.darylhandley.fifteenfiveutils.client.FifteenFiveFormClient
import com.sonatype.darylhandley.fifteenfiveutils.service.AliasService
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveCloneService
import com.sonatype.darylhandley.fifteenfiveutils.service.ObjectiveService
import com.sonatype.darylhandley.fifteenfiveutils.service.UserService
import com.sonatype.darylhandley.fifteenfiveutils.util.ConfigLoader

object ObjectiveCloneTest {

    private const val TEST_OBJECTIVE_ID = 12872509
    private const val TARGET_USER_ALIAS = "daryl"

    @JvmStatic
    fun main(args: Array<String>) {
        println("🧪 OBJECTIVE CLONE TEST")
        println("═".repeat(50))

        try {
            // Load configuration
            println("📋 Loading configuration...")
            val sessionId = ConfigLoader.getSessionId()
            val csrfToken = ConfigLoader.getCsrfMiddlewareToken()
            println("✅ Session ID loaded: ${sessionId.take(10)}...")
            println("✅ CSRF Token loaded: ${csrfToken.take(10)}...")

            // Initialize services
            println("\n🔧 Initializing services...")
            val userService = UserService(sessionId)
            val formClient = FifteenFiveFormClient(sessionId, csrfToken)
            val objectiveService = ObjectiveService(sessionId, formClient)
            val objectiveCloneService = ObjectiveCloneService(sessionId)
            val aliasService = AliasService(userService)
            println("✅ Services initialized")

            // Resolve target user alias
            println("\n👤 Resolving target user alias '$TARGET_USER_ALIAS'...")
            val targetUserId = aliasService.resolveUserIdentifier(TARGET_USER_ALIAS)
            if (targetUserId == null) {
                println("❌ ERROR: Could not resolve alias '$TARGET_USER_ALIAS'")
                println("Available aliases:")
                println(aliasService.listAliases())
                return
            }
            println("✅ Resolved '$TARGET_USER_ALIAS' → User ID: $targetUserId")

            // Get target user name
            println("\n📝 Getting target user details...")
            val targetUser = userService.listAllUsers().find { it.id == targetUserId }
            val targetUserName = targetUser?.fullName ?: "User ID $targetUserId"
            println("✅ Target user: $targetUserName")

            // Fetch source objective
            println("\n🎯 Fetching source objective $TEST_OBJECTIVE_ID...")
            val sourceObjective = objectiveService.getObjective(TEST_OBJECTIVE_ID)
            println("✅ Source objective loaded:")
            println("   Title: ${sourceObjective.description}")
            println("   Owner: ${sourceObjective.user.name}")
            println("   Key Results: ${sourceObjective.keyResults.size}")
            println("   Tags: ${sourceObjective.getTagNames()}")

            // Show clone preview
            println("\n📋 Clone Preview:")
            val preview = objectiveCloneService.buildClonePreview(sourceObjective, targetUserName, targetUserId)
            println(preview)

            // Attempt clone
            println("🚀 Attempting to clone objective...")
            objectiveCloneService.cloneObjective(sourceObjective, targetUserId)
            println("✅ SUCCESS!")
        } catch (e: Exception) {
            println("❌ ERROR: ${e.message}")
            e.printStackTrace()

            // Additional debugging info
            println("\n🔍 DEBUGGING INFO:")
            println("- Make sure your application.properties file contains both:")
            println("  • fifteen.five.session.id=YOUR_SESSION_ID")
            println("  • fifteen.five.csrf.token=YOUR_CSRF_TOKEN")
            println("- Make sure the 'daryl' alias exists (run: useralias list)")
            println("- Make sure objective $TEST_OBJECTIVE_ID exists and is accessible")
        }
    }
}