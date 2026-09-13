package com.thevirtualtrust.ppis.sync.work

import com.thevirtualtrust.ppis.core.session.SessionManager
import com.thevirtualtrust.ppis.data.analytics.AnalyticsRepository
import com.thevirtualtrust.ppis.data.profile.ProfileRepository
import com.thevirtualtrust.ppis.sync.TelemetrySyncCoordinator
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(
    SingletonComponent::class
)
interface TelemetryWorkerEntryPoint {

    fun sessionManager():
        SessionManager

    fun telemetrySyncCoordinator():
        TelemetrySyncCoordinator

    fun analyticsRepository():
        AnalyticsRepository

    fun profileRepository():
        ProfileRepository
}
