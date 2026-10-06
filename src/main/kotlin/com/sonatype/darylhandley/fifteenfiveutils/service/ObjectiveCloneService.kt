package com.sonatype.darylhandley.fifteenfiveutils.service

import com.sonatype.darylhandley.fifteenfiveutils.client.FifteenFiveHttp
import com.sonatype.darylhandley.fifteenfiveutils.model.Objective
import okhttp3.FormBody
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ObjectiveCloneService(private val http: FifteenFiveHttp) {
    fun cloneObjective(sourceObjective: Objective, targetUserId: Int): Int {
        val formData = buildFormData(sourceObjective, targetUserId)
        val responseUrl = submitObjectiveForm(formData)
        return parseObjectiveIdFromUrl(responseUrl)
    }

    fun buildClonePreview(sourceObjective: Objective, targetUserName: String, targetUserId: Int): String {
        val result = StringBuilder()

        result.append("═".repeat(80)).append("\n")
        result.append("📋 CLONE OBJECTIVE PREVIEW\n")
        result.append("═".repeat(80)).append("\n")

        result.append("📝 Title: \"${sourceObjective.description}\"\n")
        result.append("👤 From: ${sourceObjective.user.name} → $targetUserName ($targetUserId)\n")
        result.append("📅 Period: ${sourceObjective.getFormattedStartDate()} → ${sourceObjective.getFormattedEndDate()}\n")

        // Note: Objective model doesn't have longDescription field from API
        // Will use empty long description in form submission

        if (sourceObjective.tags.isNotEmpty()) {
            result.append("🏷️  Tags: ${sourceObjective.getTagNames()}\n")
        }

        result.append("\n🔑 Key Results (${sourceObjective.keyResults.size}):\n")
        sourceObjective.keyResults.forEachIndexed { index, keyResult ->
            result.append("  ${index + 1}. \"${keyResult.description}\" (${keyResult.startValueDisplay} → ${keyResult.targetValueDisplay})\n")
            result.append("     Owner: $targetUserName\n")
        }

        result.append("═".repeat(80)).append("\n")
        result.append("Clone this objective? (y/N): ")

        return result.toString()
    }

    private fun buildFormData(sourceObjective: Objective, targetUserId: Int): FormBody {
        val formBuilder = FormBody.Builder()

        // Self-generated CSRF token; must match the ff_csrf_token cookie set by FifteenFiveHttp
        formBuilder.add("csrfmiddlewaretoken", http.csrfToken)

        // Django formset management for key results
        formBuilder.add("key-result-TOTAL_FORMS", sourceObjective.keyResults.size.toString())
        formBuilder.add("key-result-INITIAL_FORMS", "0")
        formBuilder.add("key-result-MIN_NUM_FORMS", "0")
        formBuilder.add("key-result-MAX_NUM_FORMS", "25")

        // Objective fields
        formBuilder.add("description", sourceObjective.description)
        formBuilder.add("long_description", "") // No long description available from API
        formBuilder.add("user", targetUserId.toString())

        // Scope settings (copy from source or use defaults)
        formBuilder.add("scope_option", "company") // Default to company-wide
        formBuilder.add("scope", "company-wide")
        formBuilder.add("group_type", "")
        formBuilder.add("group", "")
        formBuilder.add("parent", sourceObjective.id.toString())
        formBuilder.add("is_progress_aligned", "0")

        //        formBuilder.add("is_progress_aligned", "")

        // Period settings
        formBuilder.add("period", "custom")
        formBuilder.add("start_ts", formatDateForForm(sourceObjective.getFormattedStartDate()))
        formBuilder.add("end_ts", formatDateForForm(sourceObjective.getFormattedEndDate()))

        // Visibility
        formBuilder.add("visibility", "public")

        // Tags
        sourceObjective.tags.forEach { tag ->
            formBuilder.add("tags", tag.id.toString())
        }

        // Key results
        sourceObjective.keyResults.forEachIndexed { index, keyResult ->
            formBuilder.add("key-result-$index-description", keyResult.description)
            formBuilder.add("key-result-$index-id", "")
            formBuilder.add("key-result-$index-integration_link", "")
            formBuilder.add("key-result-$index-sort_order", keyResult.sortOrder.toString())
            formBuilder.add("key-result-$index-type", keyResult.type)
            formBuilder.add("key-result-$index-currency", "")
            formBuilder.add("key-result-$index-start_value", keyResult.startValue)
            formBuilder.add("key-result-$index-target_value", keyResult.targetValue)
            formBuilder.add("key-result-$index-owner", targetUserId.toString())
        }

        return formBuilder.build()
    }

    private fun formatDateForForm(dateString: String): String {
        // Parse the ISO date from the API (e.g., "2025-07-01")
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val date = inputFormat.parse(dateString)
        return outputFormat.format(date ?: Date())
    }

    /**
     * Posts the Django create-objective form. On success Django answers with a 302 redirect to the new
     * objective's details page (we don't follow redirects), so the new ID is read from the Location header.
     * A 200 means the form was re-rendered with validation errors.
     * @return the redirect location (absolute or relative)
     */
    private fun submitObjectiveForm(formData: FormBody): String {
        val request = Request.Builder()
            .url("${FifteenFiveHttp.BASE_URL}/objectives/create/")
            .post(formData)
            .header("Referer", "${FifteenFiveHttp.BASE_URL}/objectives/create/")
            .build()

        http.okHttp.newCall(request).execute().use { response ->
            if (response.isRedirect) {
                return response.header("Location")
                    ?: throw RuntimeException("Create objective redirected without a Location header")
            }
            if (response.code == 200) {
                // Uncomment to inspect form errors in a browser:
                // java.io.File("create_response.html").writeText(response.body?.string() ?: "")
                throw RuntimeException("Create objective form was rejected (HTTP 200, form re-rendered — likely a validation error)")
            }
            throw RuntimeException("Failed to create objective: ${response.code} ${response.message}")
        }
    }

    private fun parseObjectiveIdFromUrl(url: String): Int {
        // URL format: https://sonatype.15five.com/objectives/details/12885743/
        val objectiveIdRegex = Regex("/objectives/details/(\\d+)")
        val match = objectiveIdRegex.find(url)

        return if (match != null) {
            match.groupValues[1].toInt()
        } else {
            throw RuntimeException("Could not parse objective ID from URL: $url")
        }
    }
}