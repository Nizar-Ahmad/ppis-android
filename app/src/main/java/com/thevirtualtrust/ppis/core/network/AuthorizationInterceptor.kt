package com.thevirtualtrust.ppis.core.network

import com.thevirtualtrust.ppis.core.session.SessionManager
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class AuthorizationInterceptor @Inject constructor(
    private val sessionManager:
        SessionManager
) : Interceptor {

    override fun intercept(
        chain: Interceptor.Chain
    ): Response {

        val accessToken =
            sessionManager
                .currentAccessTokenSnapshot()

        val originalRequest =
            chain.request()

        if (
            accessToken.isNullOrBlank()
        ) {
            return chain.proceed(
                originalRequest
            )
        }

        val authenticatedRequest =
            originalRequest
                .newBuilder()
                .header(
                    "Authorization",
                    "Bearer $accessToken"
                )
                .build()

        return chain.proceed(
            authenticatedRequest
        )
    }
}
