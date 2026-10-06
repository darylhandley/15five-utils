package com.sonatype.darylhandley.fifteenfiveutils.client

import com.sonatype.darylhandley.fifteenfiveutils.auth.SessionExpiredException
import feign.Feign
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

/**
 * Single shared HTTP stack for talking to 15Five's internal (unofficial) web endpoints.
 *
 * Auth model:
 * - `sessionid` cookie — copied from the browser once via the `auth` command. Mutable so it can be
 *   refreshed without restarting the shell.
 * - CSRF — 15Five is Django; its CSRF check is double-submit (the `ff_csrf_token` cookie must equal the
 *   `X-CSRFToken` header / `csrfmiddlewaretoken` form field) and is not bound to the session. So we generate
 *   our own random token per run and send it in both places. No token needs to be copied from the browser.
 *
 * Redirects are NOT followed. 15Five answers an expired session with a 302 to `/?next=...` (pages) or a
 * 401/403 (JSON endpoints); following the redirect would hand Jackson the login page HTML. The interceptor
 * turns those responses into a [SessionExpiredException] with an actionable message.
 */
class FifteenFiveHttp(@Volatile var sessionId: String?) {

    companion object {
        const val BASE_URL = "https://sonatype.15five.com"
        private const val TOKEN_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

        private fun generateCsrfToken(): String {
            val random = SecureRandom()
            return (1..32).map { TOKEN_CHARS[random.nextInt(TOKEN_CHARS.length)] }.joinToString("")
        }
    }

    /** Self-generated CSRF token, valid for any request as long as cookie and header/field match. */
    val csrfToken: String = generateCsrfToken()

    val okHttp: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .addInterceptor(Interceptor { chain -> intercept(chain) })
        .build()

    val api: FifteenFiveClient = Feign.builder()
        .client(feign.okhttp.OkHttpClient(okHttp))
        // Feign's OkHttp adapter re-applies Options per request; its default followRedirects=true would
        // silently follow the login redirect, so it must be disabled here as well.
        .options(feign.Request.Options(10, TimeUnit.SECONDS, 60, TimeUnit.SECONDS, false))
        .encoder(JacksonEncoder())
        .decoder(JacksonDecoder())
        .target(FifteenFiveClient::class.java, BASE_URL)

    private fun intercept(chain: Interceptor.Chain): Response {
        val session = sessionId
            ?: throw SessionExpiredException("No 15Five session configured. Copy the 'sessionid' cookie from your browser and run 'auth'.")

        val original = chain.request()
        val builder = original.newBuilder()
            .header("Cookie", "sessionid=$session; ff_csrf_token=$csrfToken")
        if (original.method != "GET") {
            builder.header("X-CSRFToken", csrfToken)
            builder.header("Origin", BASE_URL)
            if (original.header("Referer") == null) {
                builder.header("Referer", "$BASE_URL/objectives/")
            }
        }

        val response = chain.proceed(builder.build())
        checkSession(response)
        return response
    }

    private fun checkSession(response: Response) {
        val location = response.header("Location") ?: ""
        val isLoginRedirect = response.isRedirect &&
            (location.startsWith("/?next=") || location == "/" || location.contains("/login"))

        if (isLoginRedirect || response.code == 401 || response.code == 403) {
            response.close()
            val detail = if (isLoginRedirect) "redirected to login" else "HTTP ${response.code}"
            throw SessionExpiredException(
                "15Five session expired or invalid ($detail). Copy a fresh 'sessionid' cookie from your browser and run 'auth'."
            )
        }
    }
}
