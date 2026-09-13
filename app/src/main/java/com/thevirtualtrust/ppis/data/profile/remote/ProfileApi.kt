package com.thevirtualtrust.ppis.data.profile.remote

import com.thevirtualtrust.ppis.data.profile.remote.dto.ProfileDto
import com.thevirtualtrust.ppis.data.profile.remote.dto.ProfileNotificationPreferencesDto
import com.thevirtualtrust.ppis.data.profile.remote.dto.ProfileUpdateRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface ProfileApi {

    @GET("profile")
    suspend fun getProfile():
        ProfileDto

    @PUT("profile")
    suspend fun updateProfile(
        @Body
        request: ProfileUpdateRequestDto
    ): ProfileDto

    @GET("profile/notifications")
    suspend fun getNotificationPreferences():
        ProfileNotificationPreferencesDto

    @PUT("profile/notifications")
    suspend fun updateNotificationPreferences(
        @Body
        request:
            ProfileNotificationPreferencesDto
    ): ProfileNotificationPreferencesDto
}
