package com.thevirtualtrust.ppis.data.reminder

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton

data class LocalReminderSettings(
    val setupCompleted: Boolean,
    val enabled: Boolean,
    val hour: Int,
    val minute: Int
)

@Singleton
class ReminderPreferences @Inject constructor(
    @param:ApplicationContext
    private val context: Context
) {

    private val lock =
        Any()

    private val file:
        File
        get() =
            File(
                context.noBackupFilesDir,
                FILE_NAME
            )

    fun read():
        LocalReminderSettings {

        return synchronized(
            lock
        ) {

            if (
                !file.exists()
            ) {

                defaultSettings()

            } else {

                readExistingFile()
            }
        }
    }

    fun saveEnabled(
        time: ReminderTime
    ) {

        write(
            LocalReminderSettings(
                setupCompleted =
                    true,
                enabled =
                    true,
                hour =
                    time.hour,
                minute =
                    time.minute
            )
        )
    }

    fun saveSkipped() {

        val current =
            read()

        write(
            current.copy(
                setupCompleted =
                    true,
                enabled =
                    false
            )
        )
    }

    private fun readExistingFile():
        LocalReminderSettings {

        return try {

            val properties =
                Properties()

            FileInputStream(
                file
            ).use {
                    input ->

                properties.load(
                    input
                )
            }

            val completed =
                properties
                    .getProperty(
                        KEY_SETUP_COMPLETED
                    )
                    ?.toBooleanStrictOrNull()
                    ?: false

            val enabled =
                properties
                    .getProperty(
                        KEY_ENABLED
                    )
                    ?.toBooleanStrictOrNull()
                    ?: false

            val hour =
                properties
                    .getProperty(
                        KEY_HOUR
                    )
                    ?.toIntOrNull()
                    ?.coerceIn(
                        0,
                        23
                    )
                    ?: DEFAULT_HOUR

            val minute =
                properties
                    .getProperty(
                        KEY_MINUTE
                    )
                    ?.toIntOrNull()
                    ?.coerceIn(
                        0,
                        59
                    )
                    ?: DEFAULT_MINUTE

            LocalReminderSettings(
                setupCompleted =
                    completed,
                enabled =
                    enabled,
                hour =
                    hour,
                minute =
                    minute
            )

        } catch (
            exception:
                Exception
        ) {

            defaultSettings()
        }
    }

    private fun defaultSettings():
        LocalReminderSettings =
        LocalReminderSettings(
            setupCompleted =
                false,
            enabled =
                false,
            hour =
                DEFAULT_HOUR,
            minute =
                DEFAULT_MINUTE
        )

    private fun write(
        settings:
            LocalReminderSettings
    ) {

        synchronized(
            lock
        ) {

            context
                .noBackupFilesDir
                .mkdirs()

            val properties =
                Properties()

            properties.setProperty(
                KEY_SETUP_COMPLETED,
                settings
                    .setupCompleted
                    .toString()
            )

            properties.setProperty(
                KEY_ENABLED,
                settings
                    .enabled
                    .toString()
            )

            properties.setProperty(
                KEY_HOUR,
                settings
                    .hour
                    .toString()
            )

            properties.setProperty(
                KEY_MINUTE,
                settings
                    .minute
                    .toString()
            )

            val temporary =
                File(
                    context.noBackupFilesDir,
                    "$FILE_NAME.tmp"
                )

            FileOutputStream(
                temporary
            ).use {
                    output ->

                properties.store(
                    output,
                    null
                )
            }

            if (
                !temporary.renameTo(
                    file
                )
            ) {

                temporary.copyTo(
                    target =
                        file,
                    overwrite =
                        true
                )

                temporary.delete()
            }
        }
    }

    companion object {

        private const val FILE_NAME =
            "local_reminder.properties"

        private const val
            KEY_SETUP_COMPLETED =
            "setup_completed"

        private const val KEY_ENABLED =
            "enabled"

        private const val KEY_HOUR =
            "hour"

        private const val KEY_MINUTE =
            "minute"

        private const val DEFAULT_HOUR =
            20

        private const val DEFAULT_MINUTE =
            0
    }
}
