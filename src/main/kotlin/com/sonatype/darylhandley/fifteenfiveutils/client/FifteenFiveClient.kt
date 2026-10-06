package com.sonatype.darylhandley.fifteenfiveutils.client

import com.sonatype.darylhandley.fifteenfiveutils.model.Objective
import com.sonatype.darylhandley.fifteenfiveutils.model.ObjectivesResponse
import com.sonatype.darylhandley.fifteenfiveutils.model.User
import feign.Param
import feign.RequestLine

/**
 * Read endpoints of 15Five's internal web API. Auth headers are added by [FifteenFiveHttp]'s interceptor.
 */
interface FifteenFiveClient {

    @RequestLine("GET /account/company/users/?include_active_only=false&include_avatars=true&include_reviewer_detail=true&include_viewer=true&include_is_active_field=true&include_15five_bot_user=false")
    fun getUsers(): List<User>

    @RequestLine("GET /objectives/api/objectives/?page={page}&page_size={pageSize}&state=current")
    fun getObjectives(
        @Param("page") page: Int,
        @Param("pageSize") pageSize: Int
    ): ObjectivesResponse

    @RequestLine("GET /objectives/api/objectives/?page={page}&page_size={pageSize}&state=current&user={userId}")
    fun getObjectivesByUser(
        @Param("page") page: Int,
        @Param("pageSize") pageSize: Int,
        @Param("userId") userId: Int
    ): ObjectivesResponse

    @RequestLine("GET /objectives/api/objectives/{id}")
    fun getObjective(@Param("id") id: Int): Objective

    @RequestLine("GET /objectives/api/objectives/?page={page}&page_size={pageSize}&state=current&parent_id={parentId}")
    fun getObjectivesByParent(
        @Param("page") page: Int,
        @Param("pageSize") pageSize: Int,
        @Param("parentId") parentId: Int
    ): ObjectivesResponse
}
