package com.thevirtualtrust.ppis.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thevirtualtrust.ppis.R
import com.thevirtualtrust.ppis.data.auth.session.AuthSessionInfo
import com.thevirtualtrust.ppis.ui.components.SectionHeader

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel:
        ProfileViewModel =
        hiltViewModel()
) {

    val state by
        viewModel.uiState
            .collectAsStateWithLifecycle()

    val activeOtherSessions =
        state.sessions.count {
            !it.isCurrent &&
                !it.isRevoked
        }

    LazyColumn(
        modifier =
            modifier.padding(
                horizontal = 20.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(
                14.dp
            )
    ) {

        item {

            Column(
                modifier =
                    Modifier.padding(
                        top = 20.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        6.dp
                    )
            ) {

                SectionHeader(
                    title = stringResource(R.string.profile_title),
                    description = stringResource(R.string.profile_description)
                )
            }
        }

        /*
         * ---------------------------------------------
         * Account summary
         * ---------------------------------------------
         */

        item {

            HorizontalDivider()
        }

        item {

            Text(
                stringResource(R.string.profile_account_title),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge
            )
        }

        if (
            state.isProfileLoading
        ) {

            item {
                CircularProgressIndicator()
            }

        } else {

            state.profile?.let { profile ->

                item {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(
                                    16.dp
                                ),
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {

                            ReadOnlyValue(
                                label =
                                    stringResource(
                                        R.string.profile_email
                                    ),
                                value =
                                    profile.email
                            )

                            ReadOnlyValue(
                                label =
                                    stringResource(
                                        R.string.profile_role
                                    ),
                                value =
                                    profile.role
                            )

                            ReadOnlyValue(
                                label =
                                    stringResource(
                                        R.string.profile_google
                                    ),
                                value =
                                    stringResource(
                                        if (
                                            profile.googleConnected
                                        ) {
                                            R.string
                                                .profile_connected
                                        } else {
                                            R.string
                                                .profile_not_connected
                                        }
                                    )
                            )

                            ReadOnlyValue(
                                label =
                                    stringResource(
                                        R.string.profile_password
                                    ),
                                value =
                                    stringResource(
                                        if (
                                            profile.hasPassword
                                        ) {
                                            R.string
                                                .profile_password_set
                                        } else {
                                            R.string
                                                .profile_password_not_set
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }

        /*
         * ---------------------------------------------
         * Editable profile
         * ---------------------------------------------
         */

        item {

            Text(
                stringResource(R.string.profile_personal_title),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge
            )
        }

        item {

            OutlinedTextField(
                modifier =
                    Modifier.fillMaxWidth(),
                value =
                    state.fullName,
                onValueChange =
                    viewModel::onFullNameChanged,
                enabled =
                    !state.isProfileLoading &&
                        !state.isSavingProfile,
                singleLine = true,
                label = {
                    Text(
                        stringResource(
                            R.string.profile_full_name
                        )
                    )
                }
            )
        }

        item {

            OutlinedTextField(
                modifier =
                    Modifier.fillMaxWidth(),
                value =
                    state.birthDate,
                onValueChange =
                    viewModel::onBirthDateChanged,
                enabled =
                    !state.isProfileLoading &&
                        !state.isSavingProfile,
                singleLine = true,
                label = {
                    Text(
                        stringResource(
                            R.string.profile_birth_date
                        )
                    )
                },
                placeholder = {
                    Text(
                        stringResource(
                            R.string.profile_birth_date_hint
                        )
                    )
                }
            )
        }

        item {

            OutlinedTextField(
                modifier =
                    Modifier.fillMaxWidth(),
                value =
                    state.country,
                onValueChange =
                    viewModel::onCountryChanged,
                enabled =
                    !state.isProfileLoading &&
                        !state.isSavingProfile,
                singleLine = true,
                label = {
                    Text(
                        stringResource(
                            R.string.profile_country
                        )
                    )
                }
            )
        }

        item {

            OutlinedTextField(
                modifier =
                    Modifier.fillMaxWidth(),
                value =
                    state.occupation,
                onValueChange =
                    viewModel::onOccupationChanged,
                enabled =
                    !state.isProfileLoading &&
                        !state.isSavingProfile,
                singleLine = true,
                label = {
                    Text(
                        stringResource(
                            R.string.profile_occupation
                        )
                    )
                }
            )
        }

        item {

            OutlinedTextField(
                modifier =
                    Modifier.fillMaxWidth(),
                value =
                    state.timezone,
                onValueChange =
                    viewModel::onTimezoneChanged,
                enabled =
                    !state.isProfileLoading &&
                        !state.isSavingProfile,
                singleLine = true,
                label = {
                    Text(
                        stringResource(
                            R.string.profile_timezone
                        )
                    )
                }
            )
        }

        item {

            OutlinedTextField(
                modifier =
                    Modifier.fillMaxWidth(),
                value =
                    state.preferredLanguage,
                onValueChange =
                    viewModel::
                        onPreferredLanguageChanged,
                enabled =
                    !state.isProfileLoading &&
                        !state.isSavingProfile,
                singleLine = true,
                label = {
                    Text(
                        stringResource(
                            R.string.profile_language
                        )
                    )
                }
            )
        }

        item {

            SettingSwitch(
                title =
                    stringResource(
                        R.string.profile_login_otp
                    ),
                description =
                    stringResource(
                        R.string
                            .profile_login_otp_description
                    ),
                checked =
                    state.loginOtpEnabled,
                enabled =
                    !state.isProfileLoading &&
                        !state.isSavingProfile &&
                        state.profile
                            ?.hasPassword == true,
                onCheckedChange =
                    viewModel::
                        onLoginOtpEnabledChanged
            )
        }

        if (
            state.profile?.hasPassword == false
        ) {

            item {

                Text(
                    stringResource(
                        R.string
                            .profile_login_otp_password_required
                    )
                )
            }
        }

        item {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                Button(
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    enabled =
                        state.isProfileDirty &&
                            !state.isBusy,
                    onClick =
                        viewModel::saveProfile
                ) {

                    Text(
                        stringResource(
                            if (
                                state.isSavingProfile
                            ) {
                                R.string.profile_saving
                            } else {
                                R.string
                                    .profile_save_changes
                            }
                        )
                    )
                }

                OutlinedButton(
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    enabled =
                        state.isProfileDirty &&
                            !state.isBusy,
                    onClick =
                        viewModel::
                            discardProfileChanges
                ) {

                    Text(
                        stringResource(
                            R.string
                                .profile_discard_changes
                        )
                    )
                }
            }
        }

        /*
         * ---------------------------------------------
         * Notification preferences
         * ---------------------------------------------
         */

        item {

            HorizontalDivider()
        }

        item {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        6.dp
                    )
            ) {

                Text(
                    stringResource(
                        R.string
                            .profile_notifications_title
                    )
                )

                Text(
                    stringResource(
                        R.string
                            .profile_notifications_description
                    )
                )
            }
        }

        if (
            state.isNotificationsLoading
        ) {

            item {
                CircularProgressIndicator()
            }

        } else {

            item {

                SettingSwitch(
                    title =
                        stringResource(
                            R.string.profile_daily_report
                        ),
                    description =
                        stringResource(
                            R.string
                                .profile_daily_report_description
                        ),
                    checked =
                        state.dailyReportEmail,
                    enabled =
                        !state.isSavingNotifications,
                    onCheckedChange =
                        viewModel::
                            onDailyReportEmailChanged
                )
            }

            item {

                SettingSwitch(
                    title =
                        stringResource(
                            R.string.profile_weekly_report
                        ),
                    description =
                        stringResource(
                            R.string
                                .profile_weekly_report_description
                        ),
                    checked =
                        state.weeklyReportEmail,
                    enabled =
                        !state.isSavingNotifications,
                    onCheckedChange =
                        viewModel::
                            onWeeklyReportEmailChanged
                )
            }

            item {

                SettingSwitch(
                    title =
                        stringResource(
                            R.string.profile_monthly_report
                        ),
                    description =
                        stringResource(
                            R.string
                                .profile_monthly_report_description
                        ),
                    checked =
                        state.monthlyReportEmail,
                    enabled =
                        !state.isSavingNotifications,
                    onCheckedChange =
                        viewModel::
                            onMonthlyReportEmailChanged
                )
            }

            item {

                SettingSwitch(
                    title =
                        stringResource(
                            R.string.profile_new_login
                        ),
                    description =
                        stringResource(
                            R.string
                                .profile_new_login_description
                        ),
                    checked =
                        state.newLoginEmail,
                    enabled =
                        !state.isSavingNotifications,
                    onCheckedChange =
                        viewModel::
                            onNewLoginEmailChanged
                )
            }

            item {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    Button(
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        enabled =
                            state.areNotificationsDirty &&
                                !state.isBusy,
                        onClick =
                            viewModel::
                                saveNotificationPreferences
                    ) {

                        Text(
                            stringResource(
                                if (
                                    state.isSavingNotifications
                                ) {
                                    R.string
                                        .profile_saving_notifications
                                } else {
                                    R.string
                                        .profile_save_notifications
                                }
                            )
                        )
                    }

                    OutlinedButton(
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        enabled =
                            state.areNotificationsDirty &&
                                !state.isBusy,
                        onClick =
                            viewModel::
                                discardNotificationChanges
                    ) {

                        Text(
                            stringResource(
                                R.string
                                    .profile_discard_notifications
                            )
                        )
                    }
                }
            }
        }

        state.profileMessage?.let {
                message ->

            item {

                Text(
                    stringResource(
                        when (message) {

                            ProfileSaveMessage
                                .PROFILE_SAVED ->
                                R.string
                                    .profile_profile_saved

                            ProfileSaveMessage
                                .NOTIFICATIONS_SAVED ->
                                R.string
                                    .profile_notifications_saved
                        }
                    )
                )
            }
        }

        state.profileFormError?.let {
                error ->

            item {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            4.dp
                        )
                ) {

                    Text(
                        profileFormErrorText(
                            error
                        )
                    )

                    TextButton(
                        onClick =
                            viewModel::
                                clearProfileFeedback
                    ) {

                        Text(
                            stringResource(
                                R.string.profile_dismiss
                            )
                        )
                    }
                }
            }
        }

        /*
         * ---------------------------------------------
         * Sessions
         * ---------------------------------------------
         */

        item {

            HorizontalDivider()
        }

        item {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        6.dp
                    )
            ) {

                Text(
                    stringResource(
                        R.string.profile_sessions_title
                    )
                )

                Text(
                    stringResource(
                        R.string
                            .profile_sessions_description
                    )
                )
            }
        }

        if (
            state.isLoading
        ) {

            item {
                CircularProgressIndicator()
            }

        } else if (
            state.sessions.isEmpty()
        ) {

            item {

                Text(
                    stringResource(
                        R.string.profile_no_sessions
                    )
                )
            }

        } else {

            items(
                items =
                    state.sessions,
                key = {
                    it.id
                }
            ) { session ->

                SessionCard(
                    session =
                        session,
                    busy =
                        state.isBusy,
                    revoking =
                        state.action ==
                            ProfileSessionAction
                                .REVOKING_SESSION &&
                            state.actionSessionId ==
                                session.id,
                    onRevoke = {
                        viewModel
                            .revokeSession(
                                session.id
                            )
                    }
                )
            }
        }

        item {

            OutlinedButton(
                modifier =
                    Modifier.fillMaxWidth(),
                enabled =
                    !state.isBusy,
                onClick =
                    viewModel::refresh
            ) {

                if (
                    state.action ==
                        ProfileSessionAction
                            .REFRESHING
                ) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        stringResource(
                            R.string.profile_refresh
                        )
                    )
                }
            }
        }

        item {

            OutlinedButton(
                modifier =
                    Modifier.fillMaxWidth(),
                enabled =
                    !state.isBusy &&
                        activeOtherSessions > 0,
                onClick =
                    viewModel::
                        revokeOtherSessions
            ) {

                Text(
                    stringResource(
                        if (
                            state.action ==
                                ProfileSessionAction
                                    .REVOKING_OTHERS
                        ) {
                            R.string
                                .profile_revoke_others_running
                        } else {
                            R.string
                                .profile_revoke_others
                        }
                    )
                )
            }
        }

        state.message?.let {
                message ->

            item {

                Text(
                    stringResource(
                        when (message) {

                            ProfileMessage
                                .SESSION_REVOKED ->
                                R.string
                                    .profile_session_revoked

                            ProfileMessage
                                .OTHER_SESSIONS_REVOKED ->
                                R.string
                                    .profile_other_sessions_revoked
                        }
                    )
                )
            }
        }

        state.error?.let {
                error ->

            item {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            4.dp
                        )
                ) {

                    Text(
                        profileErrorText(
                            error
                        )
                    )

                    TextButton(
                        onClick =
                            viewModel::
                                clearFeedback
                    ) {

                        Text(
                            stringResource(
                                R.string.profile_dismiss
                            )
                        )
                    }
                }
            }
        }

        /*
         * ---------------------------------------------
         * Sign out
         * ---------------------------------------------
         */

        item {

            HorizontalDivider(
                modifier =
                    Modifier.padding(
                        vertical = 8.dp
                    )
            )
        }

        item {

            Text(
                stringResource(
                    R.string.profile_security_title
                )
            )
        }

        item {

            Button(
                modifier =
                    Modifier.fillMaxWidth(),
                enabled =
                    !state.isBusy,
                onClick =
                    viewModel::logoutCurrent
            ) {

                Text(
                    stringResource(
                        if (
                            state.action ==
                                ProfileSessionAction
                                    .LOGGING_OUT
                        ) {
                            R.string.profile_logging_out
                        } else {
                            R.string.profile_logout
                        }
                    )
                )
            }
        }

        item {

            Text(
                stringResource(
                    R.string
                        .profile_logout_all_warning
                )
            )
        }

        item {

            OutlinedButton(
                modifier =
                    Modifier.fillMaxWidth(),
                enabled =
                    !state.isBusy,
                onClick =
                    viewModel::logoutAll
            ) {

                Text(
                    stringResource(
                        if (
                            state.action ==
                                ProfileSessionAction
                                    .LOGGING_OUT_ALL
                        ) {
                            R.string
                                .profile_logging_out_all
                        } else {
                            R.string.profile_logout_all
                        }
                    )
                )
            }
        }

        item {

            Text(
                modifier =
                    Modifier.padding(
                        bottom = 24.dp
                    ),
                text = ""
            )
        }
    }
}

@Composable
private fun ReadOnlyValue(
    label: String,
    value: String
) {

    Column(
        verticalArrangement =
            Arrangement.spacedBy(
                2.dp
            )
    ) {

        Text(
            text =
                label
        )

        Text(
            text =
                value
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange:
        (Boolean) -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Row(
            modifier =
                Modifier.padding(
                    16.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )
        ) {

            Column(
                modifier =
                    Modifier.weight(
                        1f
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        4.dp
                    )
            ) {

                Text(
                    text =
                        title
                )

                Text(
                    text =
                        description
                )
            }

            Switch(
                checked =
                    checked,
                enabled =
                    enabled,
                onCheckedChange =
                    onCheckedChange
            )
        }
    }
}

@Composable
private fun SessionCard(
    session: AuthSessionInfo,
    busy: Boolean,
    revoking: Boolean,
    onRevoke: () -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    6.dp
                )
        ) {

            Text(
                when {

                    session.isCurrent ->
                        stringResource(
                            R.string.profile_current_device
                        )

                    session.deviceName
                        ?.isNotBlank() == true ->
                        session.deviceName

                    else ->
                        stringResource(
                            R.string.profile_unknown_device
                        )
                }
            )

            if (
                session.isCurrent &&
                !session.deviceName
                    .isNullOrBlank()
            ) {

                Text(
                    session.deviceName
                )
            }

            Text(
                stringResource(
                    if (
                        session.isRevoked
                    ) {
                        R.string.profile_revoked_session
                    } else {
                        R.string.profile_active_session
                    }
                )
            )

            Text(
                stringResource(
                    R.string.profile_client,
                    session.clientType
                )
            )

            session.appVersion
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                        appVersion ->

                    Text(
                        stringResource(
                            R.string.profile_app_version,
                            appVersion
                        )
                    )
                }

            Text(
                stringResource(
                    R.string.profile_last_seen,
                    session.lastSeenAt
                )
            )

            Text(
                stringResource(
                    R.string.profile_created,
                    session.createdAt
                )
            )

            if (
                !session.isCurrent &&
                !session.isRevoked
            ) {

                OutlinedButton(
                    enabled =
                        !busy,
                    onClick =
                        onRevoke
                ) {

                    Text(
                        stringResource(
                            if (
                                revoking
                            ) {
                                R.string.profile_revoking
                            } else {
                                R.string
                                    .profile_revoke_session
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun profileFormErrorText(
    error: ProfileFormError
): String =
    stringResource(
        when (error) {

            ProfileFormError.FULL_NAME_REQUIRED ->
                R.string.profile_full_name_required

            ProfileFormError.TIMEZONE_REQUIRED ->
                R.string.profile_timezone_required

            ProfileFormError.LANGUAGE_REQUIRED ->
                R.string.profile_language_required

            ProfileFormError.INVALID_INPUT ->
                R.string.profile_invalid_input

            ProfileFormError
                .LOGIN_OTP_REQUIRES_PASSWORD ->
                R.string
                    .profile_login_otp_password_required

            ProfileFormError.NETWORK_UNAVAILABLE ->
                R.string.profile_network_error

            ProfileFormError.TIMEOUT ->
                R.string.profile_timeout_error

            ProfileFormError.SERVER ->
                R.string.profile_server_error

            ProfileFormError.UNKNOWN ->
                R.string.profile_unknown_error
        }
    )

@Composable
private fun profileErrorText(
    error: ProfileError
): String =
    stringResource(
        when (error) {

            ProfileError.NETWORK_UNAVAILABLE ->
                R.string.profile_network_error

            ProfileError.TIMEOUT ->
                R.string.profile_timeout_error

            ProfileError.UNAUTHORIZED ->
                R.string.profile_unauthorized_error

            ProfileError.FORBIDDEN ->
                R.string.profile_forbidden_error

            ProfileError.NOT_FOUND ->
                R.string.profile_not_found_error

            ProfileError.SERVER ->
                R.string.profile_server_error

            ProfileError.UNKNOWN ->
                R.string.profile_unknown_error
        }
    )
