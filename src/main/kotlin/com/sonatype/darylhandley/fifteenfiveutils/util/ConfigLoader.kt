package com.sonatype.darylhandley.fifteenfiveutils.util

import java.io.FileInputStream
import java.util.*

object ConfigLoader {
    
    private val properties = Properties()
    
    init {
        loadProperties()
    }
    
    private fun loadProperties() {
        try {
            FileInputStream("application.properties").use { fis ->
                properties.load(fis)
            }
        } catch (e: Exception) {
            throw RuntimeException("Could not load application.properties. Please copy sample.properties to application.properties and configure your settings.", e)
        }
    }
    
    fun getSessionId(): String {
        return properties.getProperty("fifteen.five.session.id")
            ?: throw RuntimeException("fifteen.five.session.id not found in application.properties")
    }
    
    fun getCsrfToken(): String {
        return properties.getProperty("fifteen.five.csrf.token")
            ?: throw RuntimeException("fifteen.five.csrf.token not found in application.properties")
    }
}