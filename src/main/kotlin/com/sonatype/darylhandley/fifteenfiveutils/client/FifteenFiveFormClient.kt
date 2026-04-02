package com.sonatype.darylhandley.fifteenfiveutils.client

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * Client for making form-based POST requests to the 15Five API.
 * Uses raw OkHttp for operations not suitable for Feign (e.g., form posts with CSRF tokens).
 */
class FifteenFiveFormClient(
    private val sessionId: String,
    private val csrfToken: String
) {
    private val httpClient = OkHttpClient()
    private val baseUrl = "https://sonatype.15five.com"

    /**
     * Posts form data to the specified endpoint with proper authentication headers.
     * @param endpoint The API endpoint (relative to base URL)
     * @param formData Map of form field names to values
     * @return The HTTP response
     * @throws RuntimeException if the request fails
     */
    private fun postForm(endpoint: String, formData: Map<String, String>): Response {
        val formBody = FormBody.Builder().apply {
            formData.forEach { (key, value) ->
                add(key, value)
            }
        }.build()

        val request = Request.Builder()
            .url("$baseUrl$endpoint")
            .post(formBody)
            .header("Origin", baseUrl)
            .header("Referer", "$baseUrl/objectives/")
            .header("X-CSRFToken", csrfToken)
            .header("Cookie", "ff_csrf_token=$csrfToken; sessionid=$sessionId;")
            .build()

        return httpClient.newCall(request).execute()
    }

    /**
     * Posts form data and validates success, throwing exception on failure.
     * @param endpoint The API endpoint
     * @param formData Map of form field names to values
     * @param errorMessage Custom error message prefix
     * @throws RuntimeException if the API call fails
     */
    private fun postFormWithValidation(
        endpoint: String,
        formData: Map<String, String>,
        errorMessage: String
    ) {
        val response = postForm(endpoint, formData)

        if (!response.isSuccessful) {
            throw RuntimeException("$errorMessage: ${response.code} ${response.message}")
        }
    }

    /**
     * Updates a single key result with a new value via the 15Five API.
     * @param keyResultId The ID of the key result to update
     * @param newValue The new value to set
     * @throws RuntimeException if the API call fails
     */
    fun updateKeyResult(keyResultId: Int, newValue: Int) {
        postFormWithValidation(
            endpoint = "/objectives/ajax/update-key-result/",
            formData = mapOf(
                "key_result_id" to keyResultId.toString(),
                "value" to newValue.toString()
            ),
            errorMessage = "Failed to update key result $keyResultId"
        )
    }
}
