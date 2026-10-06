package com.sonatype.darylhandley.fifteenfiveutils.auth

import com.sonatype.darylhandley.fifteenfiveutils.client.FifteenFiveFormClient
import com.sonatype.darylhandley.fifteenfiveutils.client.FifteenFiveHttp
import java.util.concurrent.TimeUnit

/**
 * Manages the 15Five session: reading it from the clipboard, validating it, persisting it, and
 * swapping it into the live HTTP client.
 */
class AuthService(
    private val http: FifteenFiveHttp,
    private val formClient: FifteenFiveFormClient,
    private val store: SessionStore
) {
    data class Status(val ok: Boolean, val message: String)

    val source: String get() = store.source

    /** Cheap read to confirm the session works. Never throws. */
    fun check(): Status {
        if (http.sessionId == null) {
            return Status(false, "No session configured. Copy the 'sessionid' cookie from your browser and run 'auth'.")
        }
        return try {
            http.api.getObjectives(1, 1)
            Status(true, "Session OK (from ${store.source})")
        } catch (e: SessionExpiredException) {
            Status(false, e.message ?: "Session invalid")
        } catch (e: Exception) {
            Status(false, "Could not verify session: ${e.message}")
        }
    }

    /**
     * Verifies that writes would pass CSRF using a no-op probe (updates non-existent key result 0).
     */
    fun checkWrite(): Status {
        return try {
            val code = formClient.probeWriteAccess()
            Status(true, "Write path OK — CSRF accepted (no-op probe returned HTTP $code)")
        } catch (e: SessionExpiredException) {
            Status(false, "Write path rejected: ${e.message}")
        } catch (e: Exception) {
            Status(false, "Write probe failed: ${e.message}")
        }
    }

    /**
     * Validates [candidate] and, only if valid, saves it and switches the live client to it.
     * The previous session is restored if validation fails.
     */
    fun login(candidate: String): Status {
        val sessionId = normalise(candidate)
        if (sessionId.isEmpty()) {
            return Status(false, "No session id provided. Copy the 'sessionid' cookie value to your clipboard, then run 'auth'.")
        }
        val previous = http.sessionId
        http.sessionId = sessionId
        val status = check()
        if (!status.ok) {
            http.sessionId = previous
            return Status(false, "Session not saved — ${status.message}")
        }
        store.save(sessionId)
        return Status(true, "Session saved to ${store.source} and verified.")
    }

    fun readClipboard(): String {
        return try {
            val process = ProcessBuilder("pbpaste").redirectErrorStream(true).start()
            val text = process.inputStream.bufferedReader().readText()
            process.waitFor(5, TimeUnit.SECONDS)
            text
        } catch (e: Exception) {
            ""
        }
    }

    /** Accepts a bare value, or a pasted "sessionid=..." / cookie-header fragment. */
    internal fun normalise(raw: String): String {
        val trimmed = raw.trim()
        val match = Regex("(?:^|[;\\s])sessionid=([^;\\s]+)").find(trimmed)
        return (match?.groupValues?.get(1) ?: trimmed).trim().trim('"')
    }
}
