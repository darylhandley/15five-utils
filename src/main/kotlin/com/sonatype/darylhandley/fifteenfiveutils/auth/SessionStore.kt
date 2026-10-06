package com.sonatype.darylhandley.fifteenfiveutils.auth

import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.Properties

/**
 * Persists the 15Five `sessionid` cookie in ~/.15fiveutils/session (chmod 600).
 *
 * The session id is a full login to the user's 15Five account, so it lives outside the repo.
 * For backwards compatibility, falls back to `fifteen.five.sessionid` in ./application.properties
 * if the session file does not exist yet.
 */
class SessionStore(
    private val configDir: File = File(System.getProperty("user.home"), ".15fiveutils"),
    private val legacyPropertiesFile: File = File("application.properties")
) {
    private val sessionFile = File(configDir, "session")

    /** Where the currently loaded session came from (for status messages). */
    var source: String = "none"
        private set

    fun load(): String? {
        if (sessionFile.exists()) {
            val value = sessionFile.readText().trim()
            if (value.isNotEmpty()) {
                source = sessionFile.path
                return value
            }
        }
        val legacy = loadLegacy()
        if (legacy != null) {
            source = "${legacyPropertiesFile.name} (legacy — run 'auth' to move it to ${sessionFile.path})"
        }
        return legacy
    }

    fun save(sessionId: String) {
        if (!configDir.exists()) configDir.mkdirs()
        sessionFile.writeText(sessionId.trim())
        try {
            Files.setPosixFilePermissions(sessionFile.toPath(), PosixFilePermissions.fromString("rw-------"))
        } catch (e: UnsupportedOperationException) {
            // Non-POSIX filesystem; best effort only.
        }
        source = sessionFile.path
    }

    private fun loadLegacy(): String? {
        if (!legacyPropertiesFile.exists()) return null
        return try {
            val props = Properties()
            FileInputStream(legacyPropertiesFile).use { props.load(it) }
            props.getProperty("fifteen.five.sessionid")?.trim()
                ?.takeIf { it.isNotEmpty() && it != "your_session_id_here" }
        } catch (e: Exception) {
            null
        }
    }
}
