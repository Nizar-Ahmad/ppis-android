package com.thevirtualtrust.ppis.data.googlecalendar

import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.googlecalendar.remote.GoogleCalendarApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleCalendarRepository @Inject constructor(
    private val api:
        GoogleCalendarApi,
    private val executor:
        ApiCallExecutor
) {

    suspend fun getAuthorizationUrl():
        ApiResult<String> =
        executor.execute {

            api.connect(
                mode =
                    "server"
            ).authorizationUrl
        }

    suspend fun getStatus():
        ApiResult<
            GoogleCalendarConnectionStatus
        > =
        executor.execute {

            val response =
                api.status()

            GoogleCalendarConnectionStatus(
                connected =
                    response.connected,
                scope =
                    response.scope,
                expiresAt =
                    response.expiresAt
            )
        }

    suspend fun sync(
        daysBack: Int = 7,
        daysForward: Int = 30
    ): ApiResult<
        GoogleCalendarSyncSummary
    > =
        executor.execute {

            val response =
                api.sync(
                    daysBack =
                        daysBack,
                    daysForward =
                        daysForward
                )

            GoogleCalendarSyncSummary(
                calendarsChecked =
                    response.calendarsChecked,
                eventsCreated =
                    response.eventsCreated,
                eventsUpdated =
                    response.eventsUpdated,
                eventsSkipped =
                    response.eventsSkipped
            )
        }

    suspend fun disconnect():
        ApiResult<Unit> =
        executor.execute {

            api.disconnect()
        }
}
