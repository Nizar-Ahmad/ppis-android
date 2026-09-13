package com.thevirtualtrust.ppis.core.device

import android.os.Build
import com.thevirtualtrust.ppis.BuildConfig
import com.thevirtualtrust.ppis.core.security.InstallationIdProvider
import com.thevirtualtrust.ppis.data.auth.remote.dto.ClientInfoDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidClientInfoProvider @Inject constructor(
    private val installationIdProvider:
        InstallationIdProvider
) : ClientInfoProvider {

    override suspend fun get():
        ClientInfoDto {

        val installationId =
            installationIdProvider
                .getOrCreate()

        val manufacturer =
            Build.MANUFACTURER
                .trim()

        val model =
            Build.MODEL
                .trim()

        val deviceName =
            listOf(
                manufacturer,
                model
            )
                .filter {
                    it.isNotBlank()
                }
                .distinct()
                .joinToString(" ")
                .take(255)
                .ifBlank {
                    "Android device"
                }

        return ClientInfoDto(
            clientType =
                "android",
            deviceId =
                installationId,
            deviceName =
                deviceName,
            appVersion =
                BuildConfig.VERSION_NAME
        )
    }
}
