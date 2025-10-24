package com.sonatype.darylhandley.fifteenfiveutils.service

import com.sonatype.darylhandley.fifteenfiveutils.client.FifteenFiveClient
import com.sonatype.darylhandley.fifteenfiveutils.model.Objective
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.okhttp.OkHttpClient
import kotlin.math.min

class ObjectiveService(private val sessionId: String, private val csrfToken: String) {

    private val client: FifteenFiveClient = Feign.builder()
        .client(OkHttpClient())
        .encoder(JacksonEncoder())
        .decoder(JacksonDecoder())
        .target(FifteenFiveClient::class.java, "https://sonatype.15five.com")

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

    fun getObjective(id: Int): Objective {
        return client.getObjective(id, sessionId)
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
                val params = mapOf(
                    "key_result_id" to childKR.id,
                    "value" to value
                )
                client.updateKeyResult(params, sessionId, csrfToken)
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
}