package com.sonatype.darylhandley.fifteenfiveutils.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ObjectivesResponse(
    @JsonProperty("count")
    val count: Int,

    @JsonProperty("next")
    val next: String?,

    @JsonProperty("previous")
    val previous: String?,

    @JsonProperty("results")
    val results: List<Objective>
)