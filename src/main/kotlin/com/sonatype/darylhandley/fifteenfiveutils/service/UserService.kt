package com.sonatype.darylhandley.fifteenfiveutils.service

import com.sonatype.darylhandley.fifteenfiveutils.client.FifteenFiveClient
import com.sonatype.darylhandley.fifteenfiveutils.model.User
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.okhttp.OkHttpClient
import java.util.*

class UserService(private val sessionId: String) {
    
    private val client: FifteenFiveClient = Feign.builder()
        .client(OkHttpClient())
        .encoder(JacksonEncoder())
        .decoder(JacksonDecoder())
        .target(FifteenFiveClient::class.java, "https://sonatype.15five.com")
    
    fun listAllUsers(): List<User> {
        return client.getUsers(sessionId)
    }
    
    fun searchUsers(searchTerm: String): List<User> {
        val allUsers = listAllUsers()
        return allUsers.filter { user ->
            user.fullName.lowercase().contains(searchTerm.lowercase()) ||
            user.title?.lowercase()?.contains(searchTerm.lowercase()) == true
        }
    }
}