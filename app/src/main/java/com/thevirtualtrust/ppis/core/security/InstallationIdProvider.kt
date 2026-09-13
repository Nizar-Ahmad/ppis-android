package com.thevirtualtrust.ppis.core.security

import android.content.Context
import android.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class InstallationIdProvider @Inject constructor(
    @ApplicationContext
    context: Context
) {

    private val mutex =
        Mutex()

    private var cachedId:
        String? = null

    private val atomicFile =
        AtomicFile(
            File(
                context.noBackupFilesDir,
                INSTALLATION_ID_FILE
            )
        )

    suspend fun getOrCreate():
        String =
        mutex.withLock {

            cachedId?.let {
                return@withLock it
            }

            val value =
                withContext(
                    Dispatchers.IO
                ) {
                    readOrCreateInternal()
                }

            cachedId =
                value

            value
        }

    private fun readOrCreateInternal():
        String {

        if (
            atomicFile.baseFile.exists()
        ) {

            val existing =
                runCatching {
                    atomicFile
                        .openRead()
                        .bufferedReader()
                        .use {
                            it.readText()
                                .trim()
                        }
                }
                    .getOrNull()

            if (
                existing != null &&
                isValidUuid(existing)
            ) {
                return existing
            }
        }

        val generated =
            UUID.randomUUID()
                .toString()

        writeInternal(
            generated
        )

        return generated
    }

    private fun writeInternal(
        value: String
    ) {

        var output =
            atomicFile.startWrite()

        try {

            output.write(
                value.toByteArray(
                    Charsets.UTF_8
                )
            )

            atomicFile.finishWrite(
                output
            )

        } catch (
            exception: Exception
        ) {

            atomicFile.failWrite(
                output
            )

            throw exception
        }
    }

    private fun isValidUuid(
        value: String
    ): Boolean =
        runCatching {
            UUID.fromString(
                value
            )
        }.isSuccess

    private companion object {

        const val INSTALLATION_ID_FILE =
            "installation_id"
    }
}
