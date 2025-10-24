package com.sonatype.darylhandley.fifteenfiveutils.client

import com.sonatype.darylhandley.fifteenfiveutils.model.Objective
import com.sonatype.darylhandley.fifteenfiveutils.model.ObjectivesResponse
import com.sonatype.darylhandley.fifteenfiveutils.model.User
import feign.Headers
import feign.Param
import feign.QueryMap
import feign.RequestLine

interface FifteenFiveClient {

    @RequestLine("GET /account/company/users/?include_active_only=false&include_avatars=true&include_reviewer_detail=true&include_viewer=true&include_is_active_field=true&include_15five_bot_user=false")
    @Headers("Cookie: sessionid={sessionId}")
    fun getUsers(@Param("sessionId") sessionId: String): List<User>

    @RequestLine("GET /objectives/api/objectives/?page={page}&page_size={pageSize}&state=current")
    @Headers("Cookie: sessionid={sessionId}")
    fun getObjectives(
        @Param("page") page: Int,
        @Param("pageSize") pageSize: Int,
        @Param("sessionId") sessionId: String
    ): ObjectivesResponse

    @RequestLine("GET /objectives/api/objectives/?page={page}&page_size={pageSize}&state=current&user={userId}")
    @Headers("Cookie: sessionid={sessionId}")
    fun getObjectivesByUser(
        @Param("page") page: Int,
        @Param("pageSize") pageSize: Int,
        @Param("userId") userId: Int,
        @Param("sessionId") sessionId: String
    ): ObjectivesResponse

    @RequestLine("GET /objectives/api/objectives/{id}")
    @Headers("Cookie: sessionid={sessionId}")
    fun getObjective(@Param("id") id: Int, @Param("sessionId") sessionId: String): Objective

    @RequestLine("POST /objectives/ajax/update-key-result/")
    @Headers(
        "Cookie: sessionid={sessionId}",
        "X-CSRFToken: {csrfToken}",
        "Content-Type: application/x-www-form-urlencoded"
    )
    fun updateKeyResult(
        @QueryMap params: Map<String, Any>,
        @Param("sessionId") sessionId: String,
        @Param("csrfToken") csrfToken: String
    )
}