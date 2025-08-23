package com.sonatype.darylhandley.fifteenfiveutils.client

import com.sonatype.darylhandley.fifteenfiveutils.model.User
import feign.Headers
import feign.Param
import feign.RequestLine

interface FifteenFiveClient {
    
    @RequestLine("GET /account/company/users/?include_active_only=false&include_avatars=true&include_reviewer_detail=true&include_viewer=true&include_is_active_field=true&include_15five_bot_user=false")
    @Headers("Cookie: sessionid={sessionId}")
    fun getUsers(@Param("sessionId") sessionId: String): List<User>
}