package com.sonatype.darylhandley.fifteenfiveutils.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class KeyResult(
    @JsonProperty("id")
    val id: Int,

    @JsonProperty("description")
    val description: String,

    @JsonProperty("sort_order")
    val sortOrder: Int,

    @JsonProperty("type")
    val type: String,

    @JsonProperty("start_value")
    val startValue: String,

    @JsonProperty("target_value")
    val targetValue: String,

    @JsonProperty("current_value")
    val currentValue: String,

    @JsonProperty("current_value_display")
    val currentValueDisplay: String,

    @JsonProperty("start_value_display")
    val startValueDisplay: String,

    @JsonProperty("target_value_display")
    val targetValueDisplay: String,

    @JsonProperty("symbol")
    val symbol: String?,

    @JsonProperty("owner")
    val owner: ObjectiveUser
)