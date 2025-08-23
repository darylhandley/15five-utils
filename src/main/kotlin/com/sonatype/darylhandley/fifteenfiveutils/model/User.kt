package com.sonatype.darylhandley.fifteenfiveutils.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class User(
    @JsonProperty("id")
    val id: Int,
    
    @JsonProperty("global_id")
    val globalId: String,
    
    @JsonProperty("full_name")
    val fullName: String,
    
    @JsonProperty("title")
    val title: String?,
    
    @JsonProperty("avatar_url")
    val avatarUrl: String?,
    
    @JsonProperty("is_reviewer")
    val isReviewer: Boolean,
    
    @JsonProperty("reviewer_id")
    val reviewerId: Int?,
    
    @JsonProperty("reviewer_full_name")
    val reviewerFullName: String?,
    
    @JsonProperty("reviewer_global_id")
    val reviewerGlobalId: String?,
    
    @JsonProperty("is_active")
    val isActive: Boolean
)