package com.sonatype.darylhandley.fifteenfiveutils.client

import okhttp3.FormBody
import okhttp3.Request

/**
 * Form-based POST requests to 15Five's internal web endpoints.
 * Cookies, CSRF and session-expiry handling come from the shared [FifteenFiveHttp] client.
 */
class FifteenFiveFormClient(private val http: FifteenFiveHttp) {

    /**
     * Updates a single key result's current value.
     * @param value Numeric value as a plain string (e.g. "3", "2.5") — decimals are preserved.
     */
    fun updateKeyResult(keyResultId: Int, value: String) {
        val body = FormBody.Builder()
            .add("key_result_id", keyResultId.toString())
            .add("value", value)
            .build()

        val request = Request.Builder()
            .url("${FifteenFiveHttp.BASE_URL}/objectives/ajax/update-key-result/")
            .post(body)
            .build()

        http.okHttp.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Failed to update key result $keyResultId: ${response.code} ${response.message}")
            }
        }
    }

    /**
     * Probes whether the CSRF + session combination is accepted for writes, without changing anything:
     * posts an update for key result id 0, which does not exist. A 403 means CSRF was rejected (or the
     * session is invalid — reported via SessionExpiredException); any other status means CSRF passed.
     * @return the HTTP status returned for the no-op probe
     */
    fun probeWriteAccess(): Int {
        val body = FormBody.Builder()
            .add("key_result_id", "0")
            .add("value", "0")
            .build()
        val request = Request.Builder()
            .url("${FifteenFiveHttp.BASE_URL}/objectives/ajax/update-key-result/")
            .post(body)
            .build()
        return http.okHttp.newCall(request).execute().use { it.code }
    }
}
