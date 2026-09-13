package com.thevirtualtrust.ppis.data.auth.google

import android.util.Log
import com.thevirtualtrust.ppis.core.device.ClientInfoProvider
import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.core.session.SessionManager
import com.thevirtualtrust.ppis.data.auth.remote.PublicAuthApi
import com.thevirtualtrust.ppis.data.auth.remote.dto.GoogleLoginRequestDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleLoginRepository @Inject constructor(
    private val publicAuthApi:
        PublicAuthApi,
    private val clientInfoProvider:
        ClientInfoProvider,
    private val sessionManager:
        SessionManager,
    private val apiCallExecutor:
        ApiCallExecutor
) {

    suspend fun login(
        idToken: String
    ): ApiResult<Unit> =
        apiCallExecutor.execute {

            val normalizedToken =
                idToken.trim()

            require(
                normalizedToken.length >=
                    MINIMUM_ID_TOKEN_LENGTH
            ) {
                "Invalid Google ID token"
            }

            val response =
                publicAuthApi
                    .googleLogin(
                        GoogleLoginRequestDto(
                            idToken =
                                normalizedToken,
                            client =
                                clientInfoProvider
                                    .get()
                        )
                    )

            /*
             * Only PPIS credentials are persisted.
             *
             * The Google ID token exists only long enough
             * to be verified by the PPIS backend.
             */
            sessionManager
                .setAuthenticated(
                    response
                        .toSessionCredentials()
                )

            Log.i(
                TAG,
                "PPIS Google authentication succeeded"
            )

            Unit
        }

    private companion object {

        const val TAG =
            "PPIS-GoogleAuth"

        const val
            MINIMUM_ID_TOKEN_LENGTH =
            20
    }
}
