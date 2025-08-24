package com.sonatype.darylhandley.fifteenfiveutils.service

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

class AliasService {
    
    private val aliasFile: File
    
    init {
        val homeDir = System.getProperty("user.home")
        val configDir = File(homeDir, ".15fiveutils")
        
        // Ensure the config directory exists
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
        
        aliasFile = File(configDir, "useraliases")
    }
    
    private fun loadAliases(): Properties {
        val properties = Properties()
        if (aliasFile.exists()) {
            FileInputStream(aliasFile).use { inputStream ->
                properties.load(inputStream)
            }
        }
        return properties
    }
    
    private fun saveAliases(properties: Properties) {
        FileOutputStream(aliasFile).use { outputStream ->
            properties.store(outputStream, "15Five Utils User Aliases")
        }
    }
    
    fun isValidAlias(alias: String): Boolean {
        return alias.isNotEmpty() && alias.matches(Regex("^[a-zA-Z0-9]+$"))
    }
    
    fun createAlias(alias: String, userId: Int): String {
        if (!isValidAlias(alias)) {
            return "Invalid alias '$alias'. Aliases must be alphanumeric only (no spaces or special characters)."
        }
        
        val properties = loadAliases()
        val lowerAlias = alias.lowercase()
        
        if (properties.containsKey(lowerAlias)) {
            return "Alias '$alias' already exists with value ${properties.getProperty(lowerAlias)}."
        }
        
        properties.setProperty(lowerAlias, userId.toString())
        saveAliases(properties)
        
        return "Alias '$alias' created for user ID $userId."
    }
    
    fun removeAlias(alias: String): String {
        val properties = loadAliases()
        val lowerAlias = alias.lowercase()
        
        if (!properties.containsKey(lowerAlias)) {
            return "Alias '$alias' not found."
        }
        
        val userId = properties.getProperty(lowerAlias)
        properties.remove(lowerAlias)
        saveAliases(properties)
        
        return "Alias '$alias' (user ID $userId) removed."
    }
    
    fun listAliases(): String {
        val properties = loadAliases()
        
        if (properties.isEmpty) {
            return "No aliases found."
        }
        
        val result = StringBuilder()
        result.append("User aliases:\n")
        
        properties.keys.map { it.toString() }.sorted().forEach { alias ->
            val userId = properties.getProperty(alias)
            result.append("  $alias → $userId\n")
        }
        
        return result.toString().trimEnd()
    }
    
    fun resolveUserIdentifier(input: String): Int? {
        // If it's already a number, return it
        input.toIntOrNull()?.let { return it }
        
        // Otherwise, try to resolve as alias
        val properties = loadAliases()
        val lowerInput = input.lowercase()
        
        return properties.getProperty(lowerInput)?.toIntOrNull()
    }
    
    fun isAlias(input: String): Boolean {
        return input.toIntOrNull() == null && loadAliases().containsKey(input.lowercase())
    }
}