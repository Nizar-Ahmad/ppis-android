package com.thevirtualtrust.ppis.data.profile

import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.profile.remote.ProfileApi
import com.thevirtualtrust.ppis.data.profile.remote.dto.ProfileDto
import com.thevirtualtrust.ppis.data.profile.remote.dto.ProfileNotificationPreferencesDto
import com.thevirtualtrust.ppis.data.profile.remote.dto.ProfileUpdateRequestDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val profileApi:
        ProfileApi,
    private val apiCallExecutor:
        ApiCallExecutor
) {

    suspend fun getProfile():
        ApiResult<UserProfile> =
        apiCallExecutor.execute {

            profileApi
                .getProfile()
                .toDomain()
        }

    suspend fun updateProfile(
        update: ProfileUpdate
    ): ApiResult<UserProfile> =
        apiCallExecutor.execute {

            profileApi
                .updateProfile(
                    ProfileUpdateRequestDto(
                        fullName =
                            update.fullName.trim(),
                        birthDate =
                            update.birthDate
                                ?.trim()
                                ?.takeIf {
                                    it.isNotBlank()
                                },
                        country =
                            update.country
                                ?.trim()
                                ?.takeIf {
                                    it.isNotBlank()
                                },
                        occupation =
                            update.occupation
                                ?.trim()
                                ?.takeIf {
                                    it.isNotBlank()
                                },
                        timezone =
                            update.timezone.trim(),
                        preferredLanguage =
                            update.preferredLanguage
                                .trim(),
                        loginOtpEnabled =
                            update.loginOtpEnabled
                    )
                )
                .toDomain()
        }

    suspend fun getNotificationPreferences():
        ApiResult<ProfileNotificationPreferences> =
        apiCallExecutor.execute {

            profileApi
                .getNotificationPreferences()
                .toDomain()
        }

    suspend fun updateNotificationPreferences(
        preferences:
            ProfileNotificationPreferences
    ): ApiResult<ProfileNotificationPreferences> =
        apiCallExecutor.execute {

            profileApi
                .updateNotificationPreferences(
                    ProfileNotificationPreferencesDto(
                        dailyReportEmail =
                            preferences
                                .dailyReportEmail,
                        weeklyReportEmail =
                            preferences
                                .weeklyReportEmail,
                        monthlyReportEmail =
                            preferences
                                .monthlyReportEmail,
                        newLoginEmail =
                            preferences
                                .newLoginEmail
                    )
                )
                .toDomain()
        }

    private fun ProfileDto.toDomain():
        UserProfile =
        UserProfile(
            id = id,
            email = email,
            fullName = fullName,
            role = role,
            hasPassword = hasPassword,
            googleConnected =
                googleConnected,
            birthDate = birthDate,
            country = country,
            occupation = occupation,
            timezone = timezone,
            preferredLanguage =
                preferredLanguage,
            loginOtpEnabled =
                loginOtpEnabled,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

    private fun ProfileNotificationPreferencesDto
        .toDomain():
        ProfileNotificationPreferences =
        ProfileNotificationPreferences(
            dailyReportEmail =
                dailyReportEmail,
            weeklyReportEmail =
                weeklyReportEmail,
            monthlyReportEmail =
                monthlyReportEmail,
            newLoginEmail =
                newLoginEmail
        )
}
