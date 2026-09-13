package com.thevirtualtrust.ppis.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.auth.session.AuthSessionInfo
import com.thevirtualtrust.ppis.data.auth.session.SessionRepository
import com.thevirtualtrust.ppis.data.profile.ProfileNotificationPreferences
import com.thevirtualtrust.ppis.data.profile.ProfileRepository
import com.thevirtualtrust.ppis.data.profile.ProfileUpdate
import com.thevirtualtrust.ppis.data.profile.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(

    /*
     * Server profile snapshot.
     */
    val profile: UserProfile? =
        null,

    /*
     * Editable profile form.
     */
    val fullName: String =
        "",

    val birthDate: String =
        "",

    val country: String =
        "",

    val occupation: String =
        "",

    val timezone: String =
        "",

    val preferredLanguage: String =
        "",

    val loginOtpEnabled: Boolean =
        false,

    val isProfileLoading: Boolean =
        true,

    val isSavingProfile: Boolean =
        false,

    val profileFormError:
        ProfileFormError? =
        null,

    val profileMessage:
        ProfileSaveMessage? =
        null,

    /*
     * Notification preferences.
     */
    val notificationPreferences:
        ProfileNotificationPreferences? =
        null,

    val dailyReportEmail: Boolean =
        false,

    val weeklyReportEmail: Boolean =
        false,

    val monthlyReportEmail: Boolean =
        false,

    val newLoginEmail: Boolean =
        false,

    val isNotificationsLoading: Boolean =
        true,

    val isSavingNotifications: Boolean =
        false,

    /*
     * Session management.
     */
    val sessions: List<AuthSessionInfo> =
        emptyList(),

    val isLoading: Boolean =
        true,

    val action: ProfileSessionAction =
        ProfileSessionAction.NONE,

    val actionSessionId: String? =
        null,

    val message: ProfileMessage? =
        null,

    val error: ProfileError? =
        null
) {

    val isBusy: Boolean
        get() =
            isProfileLoading ||
                isNotificationsLoading ||
                isSavingProfile ||
                isSavingNotifications ||
                isLoading ||
                action !=
                    ProfileSessionAction.NONE

    val isProfileDirty: Boolean
        get() {

            val original =
                profile
                    ?: return false

            return fullName.trim() !=
                original.fullName ||
                normalizeNullable(
                    birthDate
                ) !=
                original.birthDate ||
                normalizeNullable(
                    country
                ) !=
                original.country ||
                normalizeNullable(
                    occupation
                ) !=
                original.occupation ||
                timezone.trim() !=
                original.timezone ||
                preferredLanguage.trim() !=
                original.preferredLanguage ||
                loginOtpEnabled !=
                original.loginOtpEnabled
        }

    val areNotificationsDirty: Boolean
        get() {

            val original =
                notificationPreferences
                    ?: return false

            return dailyReportEmail !=
                original.dailyReportEmail ||
                weeklyReportEmail !=
                original.weeklyReportEmail ||
                monthlyReportEmail !=
                original.monthlyReportEmail ||
                newLoginEmail !=
                original.newLoginEmail
        }

    companion object {

        private fun normalizeNullable(
            value: String
        ): String? =
            value
                .trim()
                .takeIf {
                    it.isNotBlank()
                }
    }
}

enum class ProfileSessionAction {

    NONE,

    REFRESHING,

    REVOKING_SESSION,

    REVOKING_OTHERS,

    LOGGING_OUT,

    LOGGING_OUT_ALL
}

enum class ProfileMessage {

    SESSION_REVOKED,

    OTHER_SESSIONS_REVOKED
}

enum class ProfileSaveMessage {

    PROFILE_SAVED,

    NOTIFICATIONS_SAVED
}

enum class ProfileFormError {

    FULL_NAME_REQUIRED,

    TIMEZONE_REQUIRED,

    LANGUAGE_REQUIRED,

    INVALID_INPUT,

    LOGIN_OTP_REQUIRES_PASSWORD,

    NETWORK_UNAVAILABLE,

    TIMEOUT,

    SERVER,

    UNKNOWN
}

enum class ProfileError {

    NETWORK_UNAVAILABLE,

    TIMEOUT,

    UNAUTHORIZED,

    FORBIDDEN,

    NOT_FOUND,

    SERVER,

    UNKNOWN
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionRepository:
        SessionRepository,
    private val profileRepository:
        ProfileRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            ProfileUiState()
        )

    val uiState:
        StateFlow<ProfileUiState> =
        _uiState.asStateFlow()

    init {

        loadProfile()

        loadNotificationPreferences()

        loadSessions(
            initial = true
        )
    }

    /*
     * -------------------------------------------------
     * Profile
     * -------------------------------------------------
     */

    fun onFullNameChanged(
        value: String
    ) {

        if (
            profileFormLocked()
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                fullName =
                    value,
                profileFormError =
                    null,
                profileMessage =
                    null
            )
    }

    fun onBirthDateChanged(
        value: String
    ) {

        if (
            profileFormLocked()
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                birthDate =
                    value,
                profileFormError =
                    null,
                profileMessage =
                    null
            )
    }

    fun onCountryChanged(
        value: String
    ) {

        if (
            profileFormLocked()
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                country =
                    value,
                profileFormError =
                    null,
                profileMessage =
                    null
            )
    }

    fun onOccupationChanged(
        value: String
    ) {

        if (
            profileFormLocked()
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                occupation =
                    value,
                profileFormError =
                    null,
                profileMessage =
                    null
            )
    }

    fun onTimezoneChanged(
        value: String
    ) {

        if (
            profileFormLocked()
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                timezone =
                    value,
                profileFormError =
                    null,
                profileMessage =
                    null
            )
    }

    fun onPreferredLanguageChanged(
        value: String
    ) {

        if (
            profileFormLocked()
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                preferredLanguage =
                    value,
                profileFormError =
                    null,
                profileMessage =
                    null
            )
    }

    fun onLoginOtpEnabledChanged(
        enabled: Boolean
    ) {

        if (
            profileFormLocked()
        ) {
            return
        }

        val profile =
            _uiState.value.profile

        /*
         * Backend also enforces this with 409.
         *
         * Prevent the invalid state locally when we
         * already know this is a passwordless account.
         */
        if (
            enabled &&
            profile?.hasPassword == false
        ) {

            _uiState.value =
                _uiState.value.copy(
                    profileFormError =
                        ProfileFormError
                            .LOGIN_OTP_REQUIRES_PASSWORD,
                    profileMessage =
                        null
                )

            return
        }

        _uiState.value =
            _uiState.value.copy(
                loginOtpEnabled =
                    enabled,
                profileFormError =
                    null,
                profileMessage =
                    null
            )
    }

    fun saveProfile() {

        val current =
            _uiState.value

        if (
            current.isBusy ||
            current.profile == null
        ) {
            return
        }

        when {

            current.fullName
                .trim()
                .isBlank() -> {

                _uiState.value =
                    current.copy(
                        profileFormError =
                            ProfileFormError
                                .FULL_NAME_REQUIRED
                    )

                return
            }

            current.timezone
                .trim()
                .isBlank() -> {

                _uiState.value =
                    current.copy(
                        profileFormError =
                            ProfileFormError
                                .TIMEZONE_REQUIRED
                    )

                return
            }

            current.preferredLanguage
                .trim()
                .isBlank() -> {

                _uiState.value =
                    current.copy(
                        profileFormError =
                            ProfileFormError
                                .LANGUAGE_REQUIRED
                    )

                return
            }

            current.loginOtpEnabled &&
                !current.profile
                    .hasPassword -> {

                _uiState.value =
                    current.copy(
                        profileFormError =
                            ProfileFormError
                                .LOGIN_OTP_REQUIRES_PASSWORD
                    )

                return
            }
        }

        _uiState.value =
            current.copy(
                isSavingProfile =
                    true,
                profileFormError =
                    null,
                profileMessage =
                    null
            )

        viewModelScope.launch {

            when (
                val result =
                    profileRepository
                        .updateProfile(
                            ProfileUpdate(
                                fullName =
                                    current
                                        .fullName,
                                birthDate =
                                    current
                                        .birthDate,
                                country =
                                    current
                                        .country,
                                occupation =
                                    current
                                        .occupation,
                                timezone =
                                    current
                                        .timezone,
                                preferredLanguage =
                                    current
                                        .preferredLanguage,
                                loginOtpEnabled =
                                    current
                                        .loginOtpEnabled
                            )
                        )
            ) {

                is ApiResult.Success -> {

                    applyProfile(
                        profile =
                            result.value,
                        saved =
                            true
                    )
                }

                is ApiResult.Failure -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isSavingProfile =
                                false,
                            profileFormError =
                                result.error
                                    .toProfileFormError()
                        )
                }
            }
        }
    }

    fun discardProfileChanges() {

        val profile =
            _uiState.value.profile
                ?: return

        if (
            profileFormLocked()
        ) {
            return
        }

        applyProfile(
            profile =
                profile,
            saved =
                false
        )
    }

    fun refreshProfile() {

        if (
            _uiState.value.isBusy
        ) {
            return
        }

        loadProfile()
    }

    /*
     * -------------------------------------------------
     * Notification preferences
     * -------------------------------------------------
     */

    fun onDailyReportEmailChanged(
        enabled: Boolean
    ) {

        if (
            notificationFormLocked()
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                dailyReportEmail =
                    enabled,
                profileFormError =
                    null,
                profileMessage =
                    null
            )
    }

    fun onWeeklyReportEmailChanged(
        enabled: Boolean
    ) {

        if (
            notificationFormLocked()
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                weeklyReportEmail =
                    enabled,
                profileFormError =
                    null,
                profileMessage =
                    null
            )
    }

    fun onMonthlyReportEmailChanged(
        enabled: Boolean
    ) {

        if (
            notificationFormLocked()
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                monthlyReportEmail =
                    enabled,
                profileFormError =
                    null,
                profileMessage =
                    null
            )
    }

    fun onNewLoginEmailChanged(
        enabled: Boolean
    ) {

        if (
            notificationFormLocked()
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                newLoginEmail =
                    enabled,
                profileFormError =
                    null,
                profileMessage =
                    null
            )
    }

    fun saveNotificationPreferences() {

        val current =
            _uiState.value

        if (
            current.isBusy ||
            current.notificationPreferences ==
                null
        ) {
            return
        }

        _uiState.value =
            current.copy(
                isSavingNotifications =
                    true,
                profileFormError =
                    null,
                profileMessage =
                    null
            )

        viewModelScope.launch {

            when (
                val result =
                    profileRepository
                        .updateNotificationPreferences(
                            ProfileNotificationPreferences(
                            dailyReportEmail =
                                current
                                    .dailyReportEmail,
                                weeklyReportEmail =
                                    current
                                        .weeklyReportEmail,
                                monthlyReportEmail =
                                    current
                                        .monthlyReportEmail,
                                newLoginEmail =
                                    current
                                        .newLoginEmail
                            )
                        )
            ) {

                is ApiResult.Success -> {

                    applyNotificationPreferences(
                        preferences =
                            result.value,
                        saved =
                            true
                    )
                }

                is ApiResult.Failure -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isSavingNotifications =
                                false,
                            profileFormError =
                                result.error
                                    .toProfileFormError()
                        )
                }
            }
        }
    }

    fun discardNotificationChanges() {

        val preferences =
            _uiState.value
                .notificationPreferences
                ?: return

        if (
            notificationFormLocked()
        ) {
            return
        }

        applyNotificationPreferences(
            preferences =
                preferences,
            saved =
                false
        )
    }

    fun refreshNotificationPreferences() {

        if (
            _uiState.value.isBusy
        ) {
            return
        }

        loadNotificationPreferences()
    }

    fun clearProfileFeedback() {

        _uiState.value =
            _uiState.value.copy(
                profileFormError =
                    null,
                profileMessage =
                    null
            )
    }

    /*
     * -------------------------------------------------
     * Sessions
     * -------------------------------------------------
     */

    fun refresh() {

        if (
            _uiState.value.isBusy
        ) {
            return
        }

        loadSessions(
            initial = false
        )
    }

    fun revokeSession(
        sessionId: String
    ) {

        val current =
            _uiState.value

        if (
            current.isBusy
        ) {
            return
        }

        _uiState.value =
            current.copy(
                action =
                    ProfileSessionAction
                        .REVOKING_SESSION,
                actionSessionId =
                    sessionId,
                message =
                    null,
                error =
                    null
            )

        viewModelScope.launch {

            when (
                val result =
                    sessionRepository
                        .revokeSession(
                            sessionId
                        )
            ) {

                is ApiResult.Success -> {

                    /*
                     * If this was the current session,
                     * SessionManager has already emitted
                     * SignedOut and PPISRoot will remove
                     * the Main graph.
                     */
                    _uiState.value =
                        _uiState.value.copy(
                            action =
                                ProfileSessionAction.NONE,
                            actionSessionId =
                                null,
                            message =
                                ProfileMessage
                                    .SESSION_REVOKED,
                            error =
                                null
                        )

                    loadSessions(
                        initial =
                            false
                    )
                }

                is ApiResult.Failure -> {

                    _uiState.value =
                        _uiState.value.copy(
                            action =
                                ProfileSessionAction.NONE,
                            actionSessionId =
                                null,
                            error =
                                result.error
                                    .toProfileError()
                        )
                }
            }
        }
    }

    fun revokeOtherSessions() {

        val current =
            _uiState.value

        if (
            current.isBusy
        ) {
            return
        }

        _uiState.value =
            current.copy(
                action =
                    ProfileSessionAction
                        .REVOKING_OTHERS,
                actionSessionId =
                    null,
                message =
                    null,
                error =
                    null
            )

        viewModelScope.launch {

            when (
                val result =
                    sessionRepository
                        .revokeOtherSessions()
            ) {

                is ApiResult.Success -> {

                    _uiState.value =
                        _uiState.value.copy(
                            action =
                                ProfileSessionAction.NONE,
                            message =
                                ProfileMessage
                                    .OTHER_SESSIONS_REVOKED,
                            error =
                                null
                        )

                    loadSessions(
                        initial =
                            false
                    )
                }

                is ApiResult.Failure -> {

                    _uiState.value =
                        _uiState.value.copy(
                            action =
                                ProfileSessionAction.NONE,
                            error =
                                result.error
                                    .toProfileError()
                        )
                }
            }
        }
    }

    fun logoutCurrent() {

        val current =
            _uiState.value

        if (
            current.isBusy
        ) {
            return
        }

        _uiState.value =
            current.copy(
                action =
                    ProfileSessionAction
                        .LOGGING_OUT,
                message =
                    null,
                error =
                    null
            )

        viewModelScope.launch {

            when (
                val result =
                    sessionRepository
                        .logoutCurrent()
            ) {

                is ApiResult.Success -> {
                    /*
                     * SessionManager -> SignedOut drives
                     * the root transition automatically.
                     */
                }

                is ApiResult.Failure -> {

                    _uiState.value =
                        _uiState.value.copy(
                            action =
                                ProfileSessionAction.NONE,
                            error =
                                result.error
                                    .toProfileError()
                        )
                }
            }
        }
    }

    fun logoutAll() {

        val current =
            _uiState.value

        if (
            current.isBusy
        ) {
            return
        }

        _uiState.value =
            current.copy(
                action =
                    ProfileSessionAction
                        .LOGGING_OUT_ALL,
                message =
                    null,
                error =
                    null
            )

        viewModelScope.launch {

            when (
                val result =
                    sessionRepository
                        .logoutAll()
            ) {

                is ApiResult.Success -> {
                    /*
                     * Root reacts to SessionState.SignedOut.
                     */
                }

                is ApiResult.Failure -> {

                    _uiState.value =
                        _uiState.value.copy(
                            action =
                                ProfileSessionAction.NONE,
                            error =
                                result.error
                                    .toProfileError()
                        )
                }
            }
        }
    }

    fun clearFeedback() {

        _uiState.value =
            _uiState.value.copy(
                message =
                    null,
                error =
                    null
            )
    }

    /*
     * -------------------------------------------------
     * Loading
     * -------------------------------------------------
     */

    private fun loadProfile() {

        _uiState.value =
            _uiState.value.copy(
                isProfileLoading =
                    true,
                profileFormError =
                    null,
                profileMessage =
                    null
            )

        viewModelScope.launch {

            when (
                val result =
                    profileRepository
                        .getProfile()
            ) {

                is ApiResult.Success -> {

                    applyProfile(
                        profile =
                            result.value,
                        saved =
                            false
                    )
                }

                is ApiResult.Failure -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isProfileLoading =
                                false,
                            profileFormError =
                                result.error
                                    .toProfileFormError()
                        )
                }
            }
        }
    }

    private fun loadNotificationPreferences() {

        _uiState.value =
            _uiState.value.copy(
                isNotificationsLoading =
                    true,
                profileFormError =
                    null
            )

        viewModelScope.launch {

            when (
                val result =
                    profileRepository
                        .getNotificationPreferences()
            ) {

                is ApiResult.Success -> {

                    applyNotificationPreferences(
                        preferences =
                            result.value,
                        saved =
                            false
                    )
                }

                is ApiResult.Failure -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isNotificationsLoading =
                                false,
                            profileFormError =
                                result.error
                                    .toProfileFormError()
                        )
                }
            }
        }
    }

    private fun loadSessions(
        initial: Boolean
    ) {

        if (
            !initial &&
            _uiState.value.action !=
                ProfileSessionAction.NONE
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                isLoading =
                    initial,
                action =
                    if (
                        initial
                    ) {
                        ProfileSessionAction.NONE
                    } else {
                        ProfileSessionAction
                            .REFRESHING
                    },
                error =
                    null
            )

        viewModelScope.launch {

            when (
                val result =
                    sessionRepository
                        .getSessions()
            ) {

                is ApiResult.Success -> {

                    val sorted =
                        result.value
                            .sortedWith(
                                compareByDescending<
                                    AuthSessionInfo
                                > {
                                    it.isCurrent
                                }
                                    .thenBy {
                                        it.isRevoked
                                    }
                                    .thenByDescending {
                                        it.lastSeenAt
                                    }
                            )

                    _uiState.value =
                        _uiState.value.copy(
                            sessions =
                                sorted,
                            isLoading =
                                false,
                            action =
                                ProfileSessionAction.NONE,
                            actionSessionId =
                                null,
                            error =
                                null
                        )
                }

                is ApiResult.Failure -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isLoading =
                                false,
                            action =
                                ProfileSessionAction.NONE,
                            actionSessionId =
                                null,
                            error =
                                result.error
                                    .toProfileError()
                        )
                }
            }
        }
    }

    /*
     * -------------------------------------------------
     * State helpers
     * -------------------------------------------------
     */

    private fun applyProfile(
        profile: UserProfile,
        saved: Boolean
    ) {

        _uiState.value =
            _uiState.value.copy(
                profile =
                    profile,
                fullName =
                    profile.fullName,
                birthDate =
                    profile.birthDate
                        .orEmpty(),
                country =
                    profile.country
                        .orEmpty(),
                occupation =
                    profile.occupation
                        .orEmpty(),
                timezone =
                    profile.timezone,
                preferredLanguage =
                    profile.preferredLanguage,
                loginOtpEnabled =
                    profile.loginOtpEnabled,
                isProfileLoading =
                    false,
                isSavingProfile =
                    false,
                profileFormError =
                    null,
                profileMessage =
                    if (
                        saved
                    ) {
                        ProfileSaveMessage
                            .PROFILE_SAVED
                    } else {
                        null
                    }
            )
    }

    private fun applyNotificationPreferences(
        preferences:
            ProfileNotificationPreferences,
        saved: Boolean
    ) {

        _uiState.value =
            _uiState.value.copy(
                notificationPreferences =
                    preferences,
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
                        .newLoginEmail,
                isNotificationsLoading =
                    false,
                isSavingNotifications =
                    false,
                profileFormError =
                    null,
                profileMessage =
                    if (
                        saved
                    ) {
                        ProfileSaveMessage
                            .NOTIFICATIONS_SAVED
                    } else {
                        null
                    }
            )
    }

    private fun profileFormLocked():
        Boolean =
        _uiState.value
            .isProfileLoading ||
            _uiState.value
                .isSavingProfile

    private fun notificationFormLocked():
        Boolean =
        _uiState.value
            .isNotificationsLoading ||
            _uiState.value
                .isSavingNotifications

    private fun AppError.toProfileFormError():
        ProfileFormError =
        when (this) {

            is AppError.Validation ->
                ProfileFormError
                    .INVALID_INPUT

            is AppError.Conflict ->
                ProfileFormError
                    .LOGIN_OTP_REQUIRES_PASSWORD

            AppError.NetworkUnavailable ->
                ProfileFormError
                    .NETWORK_UNAVAILABLE

            AppError.Timeout ->
                ProfileFormError
                    .TIMEOUT

            is AppError.Server ->
                ProfileFormError
                    .SERVER

            else ->
                ProfileFormError
                    .UNKNOWN
        }

    private fun AppError.toProfileError():
        ProfileError =
        when (this) {

            AppError.NetworkUnavailable ->
                ProfileError
                    .NETWORK_UNAVAILABLE

            AppError.Timeout ->
                ProfileError
                    .TIMEOUT

            AppError.Unauthorized ->
                ProfileError
                    .UNAUTHORIZED

            AppError.Forbidden ->
                ProfileError
                    .FORBIDDEN

            is AppError.NotFound ->
                ProfileError
                    .NOT_FOUND

            is AppError.Server ->
                ProfileError
                    .SERVER

            else ->
                ProfileError
                    .UNKNOWN
        }
}
