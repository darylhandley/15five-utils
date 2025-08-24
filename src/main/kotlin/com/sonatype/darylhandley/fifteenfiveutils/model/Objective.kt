package com.sonatype.darylhandley.fifteenfiveutils.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@JsonIgnoreProperties(ignoreUnknown = true)
data class Objective(
    @JsonProperty("id")
    val id: Int,
    
    @JsonProperty("user")
    val user: ObjectiveUser,
    
    @JsonProperty("description")
    val description: String,
    
    @JsonProperty("start_ts")
    val startTs: String,
    
    @JsonProperty("end_ts")
    val endTs: String,
    
    @JsonProperty("color")
    val color: String,
    
    @JsonProperty("percentage")
    val percentage: String,
    
    @JsonProperty("scope")
    val scope: String,
    
    @JsonProperty("is_active")
    val isActive: Boolean,
    
    @JsonProperty("is_archived")
    val isArchived: Boolean,
    
    @JsonProperty("key_results")
    val keyResults: List<KeyResult>,
    
    @JsonProperty("tags")
    val tags: List<Tag>
) {
    fun getFormattedStartDate(): String {
        return try {
            LocalDateTime.parse(startTs, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } catch (e: Exception) {
            startTs.substring(0, 10) // fallback to first 10 chars
        }
    }
    
    fun getFormattedEndDate(): String {
        return try {
            LocalDateTime.parse(endTs, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } catch (e: Exception) {
            endTs.substring(0, 10) // fallback to first 10 chars
        }
    }
    
    fun getTagNames(): String {
        return tags.joinToString(", ") { it.name }
    }
}