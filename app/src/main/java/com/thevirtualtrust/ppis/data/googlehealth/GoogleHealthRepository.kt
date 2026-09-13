package com.thevirtualtrust.ppis.data.googlehealth

import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.googlehealth.remote.GoogleHealthApi
import com.thevirtualtrust.ppis.data.googlehealth.remote.dto.GoogleHealthStatusResponseDto
import com.thevirtualtrust.ppis.data.googlehealth.remote.dto.GoogleHealthSyncResponseDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleHealthRepository @Inject constructor(
    private val api:
        GoogleHealthApi,
    private val apiCallExecutor:
        ApiCallExecutor
) {

    suspend fun getStatus():
        ApiResult<GoogleHealthConnectionStatus> =
        apiCallExecutor.execute {

            api
                .status()
                .toDomain()
        }


    suspend fun getAuthorizationUrl():
        ApiResult<String> =
        apiCallExecutor.execute {

            val authorizationUrl =
                api
                    .connect(
                        mode =
                            "server"
                    )
                    .authorizationUrl
                    .trim()

            require(
                authorizationUrl
                    .isNotEmpty()
            ) {
                "Google Health authorization URL is empty"
            }

            authorizationUrl
        }


    /*
     * daysBack=0:
     * today only.
     *
     * daysBack=7:
     * today + previous 7 local dates
     * = 8 dates total.
     */
    suspend fun sync(
        daysBack: Int = 7
    ):
        ApiResult<GoogleHealthSyncSummary> =
        apiCallExecutor.execute {

            api
                .sync(
                    daysBack =
                        daysBack.coerceIn(
                            0,
                            7
                        )
                )
                .toDomain()
        }


    suspend fun disconnect():
        ApiResult<Unit> =
        apiCallExecutor.execute {

            api.disconnect()

            Unit
        }


    private fun GoogleHealthStatusResponseDto
        .toDomain():
        GoogleHealthConnectionStatus =
        GoogleHealthConnectionStatus(
            connected =
                connected,

            provider =
                provider,

            expiresAt =
                expiresAt,

            lastSyncAt =
                lastSyncAt
        )


    private fun GoogleHealthSyncResponseDto
        .toDomain():
        GoogleHealthSyncSummary =
        GoogleHealthSyncSummary(
            daysRequested =
                daysRequested,

            daysImported =
                daysImported,

            daysSkipped =
                daysSkipped,

            daysWithoutData =
                daysWithoutData
        )
}
