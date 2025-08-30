package com.sonatype.darylhandley.fifteenfiveutils.service

import com.sonatype.darylhandley.fifteenfiveutils.client.FifteenFiveClient
import com.sonatype.darylhandley.fifteenfiveutils.model.User
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.okhttp.OkHttpClient

class UserService(private val sessionId: String) {

    private val client: FifteenFiveClient = Feign.builder()
        .client(OkHttpClient())
        .encoder(JacksonEncoder())
        .decoder(JacksonDecoder())
        .target(FifteenFiveClient::class.java, "https://sonatype.15five.com")

    // Cache fields
    private var cachedUsers: List<User>? = null
    private var cachedUserMap: Map<Int, User>? = null

    private fun loadUsersFromAPI(): List<User> {
        return client.getUsers(sessionId)
    }

    private fun ensureCacheLoaded() {
        if (cachedUsers == null) {
            val users = loadUsersFromAPI()
            cachedUsers = users
            cachedUserMap = users.associateBy { it.id }
        }
    }

    fun listAllUsers(): List<User> {
        ensureCacheLoaded()
        return cachedUsers!!
    }

    fun getUserById(userId: Int): User? {
        ensureCacheLoaded()
        return cachedUserMap!![userId]
    }

    fun refreshUserCache(): String {
        return try {
            val users = loadUsersFromAPI()
            cachedUsers = users
            cachedUserMap = users.associateBy { it.id }
            "User cache refreshed successfully. Loaded ${users.size} users."
        } catch (e: Exception) {
            "Failed to refresh user cache: ${e.message}. Using existing cached data."
        }
    }

    fun isCacheLoaded(): Boolean {
        return cachedUsers != null
    }

    fun searchUsers(searchTerm: String): List<User> {
        val allUsers = listAllUsers()
        return allUsers.filter { user ->
            user.fullName.lowercase().contains(searchTerm.lowercase()) ||
                user.title?.lowercase()?.contains(searchTerm.lowercase()) == true
        }
    }
}