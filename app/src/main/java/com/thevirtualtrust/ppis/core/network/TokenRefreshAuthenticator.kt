package com.thevirtualtrust.ppis.core.network

import com.thevirtualtrust.ppis.core.session.RefreshCoordinator
import com.thevirtualtrust.ppis.core.session.RefreshResult
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

@Singleton
class TokenRefreshAuthenticator @Inject constructor(
    private val refreshCoordinator:
        RefreshCoordinator
) : Authenticator {

    override fun authenticate(
        route: Route?,
        response: Response
    ): Request? {

        /*
         * Permit only one authenticated retry.
         */
        if (
            responseCount(response) >= 2
        ) {
            return null
        }

        val failedAccessToken =
            response
                .request
                .header(
                    "Authorization"
                )
                ?.removePrefix(
                    "Bearer "
                )
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return null

        return when (
            val refreshResult =
                refreshCoordinator
                    .refreshIfNeeded(
                        failedAccessToken
                    )
        ) {

            is RefreshResult.Success ->

                response
                    .request
                    .newBuilder()
                    .header(
                        "Authorization",
                        "Bearer ${refreshResult.accessToken}"
                    )
                    .build()

            RefreshResult.SessionInvalid ->
                null

            RefreshResult.TransientFailure ->
                null
        }
    }

    private fun responseCount(
        response: Response
    ): Int {

        var count = 1

        var prior =
            response.priorResponse

        while (prior != null) {

            count += 1

            prior =
                prior.priorResponse
        }

        return count
    }
}
