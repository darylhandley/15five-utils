package com.sonatype.darylhandley.fifteenfiveutils.service

import com.sonatype.darylhandley.fifteenfiveutils.client.FifteenFiveClient
import com.sonatype.darylhandley.fifteenfiveutils.model.Objective
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.okhttp.OkHttpClient
import kotlin.math.min

class ObjectiveService(private val sessionId: String) {
    
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
}