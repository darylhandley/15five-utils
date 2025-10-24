package com.sonatype.darylhandley.fifteenfiveutils.service

import com.sonatype.darylhandley.fifteenfiveutils.client.FifteenFiveClient
import com.sonatype.darylhandley.fifteenfiveutils.model.Objective
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.okhttp.OkHttpClient
import okhttp3.FormBody
import okhttp3.Request
import kotlin.math.min

class ObjectiveService(private val sessionId: String, private val csrfToken: String) {

    private val client: FifteenFiveClient = Feign.builder()
        .client(OkHttpClient())
        .encoder(JacksonEncoder())
        .decoder(JacksonDecoder())
        .target(FifteenFiveClient::class.java, "https://sonatype.15five.com")

    private val httpClient = okhttp3.OkHttpClient()

    fun listObjectives(limit: Int): List<Objective> {
        val allObjectives = mutableListOf<Objective>()
        var page = 1
        val pageSize = min(limit, 50) // API might have max page size limits

        while (allObjectives.size < limit) {
            val response = client.getObjectives(page, pageSize, sessionId)
            allObjectives.addAll(response.results)

            // If we got fewer results than requested or no next page, we're done
            if (response.results.size < pageSize || response.next == null) {
                break
            }

            page++
        }

        // Return only the requested number of results
        return allObjectives.take(limit)
    }

    fun listObjectivesByUser(userId: Int): List<Objective> {
        val allObjectives = mutableListOf<Objective>()
        var page = 1
        val pageSize = 50

        while (true) {
            val response = client.getObjectivesByUser(page, pageSize, userId, sessionId)
            allObjectives.addAll(response.results)

            // If no next page, we're done
            if (response.next == null) {
                break
            }

            page++
        }

        return allObjectives
    }

    fun listObjectivesByParent(parentId: Int): List<Objective> {
        val allObjectives = mutableListOf<Objective>()
        var page = 1
        val pageSize = 50

        while (true) {
            val response = client.getObjectivesByParent(page, pageSize, parentId, sessionId)
            allObjectives.addAll(response.results)

            // If no next page, we're done
            if (response.next == null) {
                break
            }

            page++
        }

        return allObjectives
    }

    fun getObjective(id: Int): Objective {
        return client.getObjective(id, sessionId)
    }

    /**
     * Updates a single key result with a new value via the 15Five API.
     * @param keyResultId The ID of the key result to update
     * @param newValue The new value to set
     * @throws RuntimeException if the API call fails
     */
    private fun updateKeyResult(keyResultId: Int, newValue: Int) {
        val formData = FormBody.Builder()
            .add("key_result_id", keyResultId.toString())
            .add("value", newValue.toString())
            .build()

        val request = Request.Builder()
            .url("https://sonatype.15five.com/objectives/ajax/update-key-result/")
            .post(formData)
            .header("Origin", "https://sonatype.15five.com")
            .header("Referer", "https://sonatype.15five.com/objectives/")
            .header("X-CSRFToken", csrfToken)
            .header("Cookie", "ff_csrf_token=${csrfToken}; sessionid=${sessionId};")
            .build()

        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            throw RuntimeException("Failed to update key result $keyResultId: ${response.code} ${response.message}")
        }
    }

    fun updateProgressFromParent(childObjectiveId: Int): String {
        // 1. Fetch child objective
        val child = getObjective(childObjectiveId)

        // 2. Check parent exists
        if (child.parent == null) {
            throw IllegalStateException("Objective $childObjectiveId has no parent objective")
        }

        // 3. Fetch parent objective
        val parent = getObjective(child.parent)

        // 4. Create map of parent key results by description for fast lookup
        val parentKeyResultsByDesc = parent.keyResults.associateBy { it.description }

        // 5. For each child key result, find matching parent by description
        val updates = mutableListOf<String>()
        val skipped = mutableListOf<String>()

        child.keyResults.forEach { childKR ->
            val parentKR = parentKeyResultsByDesc[childKR.description]
            if (parentKR != null) {
                // Match found! Update child KR with parent's current_value
                val value = parentKR.currentValue.toDouble().toInt()
                updateKeyResult(childKR.id, value)
                updates.add("Updated \"${childKR.description}\" from ${childKR.currentValue} to ${parentKR.currentValue}")
            } else {
                // No match - skip
                skipped.add("No matching parent key result for: \"${childKR.description}\"")
            }
        }

        // Build result message
        val result = StringBuilder()
        result.append("Updated ${updates.size} key result(s) from parent objective ${parent.id} to child ${child.id}:\n")
        updates.forEach { result.append("  ✓ $it\n") }

        if (skipped.isNotEmpty()) {
            result.append("\nSkipped ${skipped.size} key result(s):\n")
            skipped.forEach { result.append("  ⚠ $it\n") }
        }

        return result.toString()
    }

    /**
     * Data class to hold information about pending child updates
     */
    data class ChildUpdatePlan(
        val parent: Objective,
        val children: List<Objective>,
        val parentKeyResultsByDesc: Map<String, com.sonatype.darylhandley.fifteenfiveutils.model.KeyResult>
    )

    /**
     * Builds a preview of what will be updated when updating children from parent
     */
    fun buildChildrenUpdatePreview(parentObjectiveId: Int): ChildUpdatePlan? {
        // 1. Fetch parent objective
        val parent = getObjective(parentObjectiveId)

        // 2. Fetch all child objectives using the parent filter
        val children = listObjectivesByParent(parentObjectiveId)

        if (children.isEmpty()) {
            return null
        }

        // 3. Create map of parent key results by description for fast lookup
        val parentKeyResultsByDesc = parent.keyResults.associateBy { it.description }

        return ChildUpdatePlan(parent, children, parentKeyResultsByDesc)
    }

    /**
     * Formats the preview of child updates into a displayable string
     */
    fun formatChildrenUpdatePreview(plan: ChildUpdatePlan): String {
        val result = StringBuilder()

        result.append("═".repeat(80)).append("\n")
        result.append("📋 UPDATE CHILD PROGRESS PREVIEW\n")
        result.append("═".repeat(80)).append("\n")

        result.append("📝 Parent Objective: \"${plan.parent.description}\" (ID: ${plan.parent.id})\n")
        result.append("👤 Owner: ${plan.parent.user.name}\n")
        result.append("📅 Period: ${plan.parent.getFormattedStartDate()} → ${plan.parent.getFormattedEndDate()}\n")
        result.append("\n🔑 Parent Key Results:\n")
        plan.parent.keyResults.forEach { kr ->
            result.append("   • \"${kr.description}\": ${kr.currentValue} / ${kr.targetValue}\n")
        }

        result.append("\n👶 Found ${plan.children.size} child objective(s):\n\n")

        var totalUpdates = 0
        var totalSkipped = 0

        plan.children.forEach { child ->
            val updates = mutableListOf<String>()
            val skipped = mutableListOf<String>()

            child.keyResults.forEach { childKR ->
                val parentKR = plan.parentKeyResultsByDesc[childKR.description]
                if (parentKR != null) {
                    updates.add("\"${childKR.description}\": ${childKR.currentValue} → ${parentKR.currentValue}")
                } else {
                    skipped.add("\"${childKR.description}\" (no matching parent KR)")
                }
            }

            totalUpdates += updates.size
            totalSkipped += skipped.size

            result.append("  Child ${child.id} - ${child.user.name}\n")
            if (updates.isNotEmpty()) {
                result.append("    ✅ Will update ${updates.size} key result(s):\n")
                updates.forEach { result.append("       • $it\n") }
            }
            if (skipped.isNotEmpty()) {
                result.append("    ⚠️  Will skip ${skipped.size} key result(s):\n")
                skipped.forEach { result.append("       • $it\n") }
            }
            result.append("\n")
        }

        result.append("═".repeat(80)).append("\n")
        result.append("📊 Summary: Will update $totalUpdates key result(s) across ${plan.children.size} child objective(s)")
        if (totalSkipped > 0) {
            result.append(", $totalSkipped will be skipped")
        }
        result.append("\n")
        result.append("═".repeat(80)).append("\n")

        return result.toString()
    }

    /**
     * Executes the child updates based on the plan
     */
    fun executeChildrenUpdate(plan: ChildUpdatePlan): String {
        val childResults = mutableListOf<String>()
        var totalUpdates = 0
        var totalSkipped = 0

        plan.children.forEach { child ->
            val updates = mutableListOf<String>()
            val skipped = mutableListOf<String>()

            child.keyResults.forEach { childKR ->
                val parentKR = plan.parentKeyResultsByDesc[childKR.description]
                if (parentKR != null) {
                    // Match found! Update child KR with parent's current_value
                    val value = parentKR.currentValue.toDouble().toInt()
                    updateKeyResult(childKR.id, value)
                    updates.add("\"${childKR.description}\" from ${childKR.currentValue} to ${parentKR.currentValue}")
                } else {
                    // No match - skip
                    skipped.add("\"${childKR.description}\"")
                }
            }

            totalUpdates += updates.size
            totalSkipped += skipped.size

            val childSummary = StringBuilder()
            childSummary.append("  Child ${child.id} (${child.user.name}): ")
            if (updates.isNotEmpty()) {
                childSummary.append("updated ${updates.size} KR(s)")
            }
            if (skipped.isNotEmpty()) {
                if (updates.isNotEmpty()) childSummary.append(", ")
                childSummary.append("skipped ${skipped.size} KR(s)")
            }
            childResults.add(childSummary.toString())
        }

        // Build result message
        val result = StringBuilder()
        result.append("✅ Updated ${plan.children.size} child objective(s) from parent ${plan.parent.id} (\"${plan.parent.description}\"):\n")
        result.append("Total: $totalUpdates key result(s) updated, $totalSkipped skipped\n\n")
        childResults.forEach { result.append("$it\n") }

        return result.toString()
    }
}