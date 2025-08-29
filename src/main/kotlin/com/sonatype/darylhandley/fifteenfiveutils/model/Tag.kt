package com.sonatype.darylhandley.fifteenfiveutils.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Tag(
    @JsonProperty("id")
    val id: Int,

    @JsonProperty("name")
    val name: String,

    @JsonProperty("slug")
    val slug: String,

    @JsonProperty("archived")
    val archived: Boolean
)