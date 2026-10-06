package com.sonatype.darylhandley.fifteenfiveutils.auth

/** Thrown when 15Five rejects the session (login redirect, 401 or 403). */
class SessionExpiredException(message: String) : RuntimeException(message)
