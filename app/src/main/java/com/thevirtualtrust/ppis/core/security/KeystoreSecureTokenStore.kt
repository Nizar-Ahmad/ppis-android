package com.thevirtualtrust.ppis.core.security

import android.content.Context
import android.util.AtomicFile
import android.util.Base64
import com.thevirtualtrust.ppis.core.session.SessionCredentials
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class KeystoreSecureTokenStore @Inject constructor(
    @ApplicationContext
    context: Context,
    private val json: Json
) : SecureTokenStore {

    private val mutex =
        Mutex()

    private val securityDirectory =
        File(
            context.noBackupFilesDir,
            SECURITY_DIRECTORY
        ).apply {
            mkdirs()
        }

    private val atomicFile =
        AtomicFile(
            File(
                securityDirectory,
                SESSION_FILE
            )
        )

    override suspend fun read():
        SessionCredentials? =
        mutex.withLock {

            withContext(
                Dispatchers.IO
            ) {
                readInternal()
            }
        }

    override suspend fun write(
        credentials: SessionCredentials
    ) {
        mutex.withLock {

            withContext(
                Dispatchers.IO
            ) {
                writeInternal(
                    credentials
                )
            }
        }
    }

    override suspend fun clear() {
        mutex.withLock {

            withContext(
                Dispatchers.IO
            ) {
                atomicFile.delete()
            }
        }
    }

    private fun readInternal():
        SessionCredentials? {

        if (
            !atomicFile.baseFile.exists()
        ) {
            return null
        }

        return try {

            val encodedEnvelope =
                atomicFile
                    .openRead()
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            val envelope =
                json.decodeFromString<
                    EncryptedSessionEnvelope
                >(
                    encodedEnvelope
                )

            require(
                envelope.version ==
                    ENVELOPE_VERSION
            ) {
                "Unsupported credential envelope version"
            }

            val iv =
                Base64.decode(
                    envelope.iv,
                    Base64.NO_WRAP
                )

            val ciphertext =
                Base64.decode(
                    envelope.ciphertext,
                    Base64.NO_WRAP
                )

            val cipher =
                Cipher.getInstance(
                    TRANSFORMATION
                )

            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(
                    GCM_TAG_LENGTH_BITS,
                    iv
                )
            )

            cipher.updateAAD(
                ASSOCIATED_DATA
            )

            val plaintext =
                cipher.doFinal(
                    ciphertext
                )

            json.decodeFromString<
                SessionCredentials
            >(
                plaintext
                    .toString(
                        Charsets.UTF_8
                    )
            )

        } catch (
            exception: Exception
        ) {

            recoverFromUnreadableState()

            null
        }
    }

    private fun writeInternal(
        credentials: SessionCredentials
    ) {

        val plaintext =
            json.encodeToString(
                credentials
            ).toByteArray(
                Charsets.UTF_8
            )

        val cipher =
            Cipher.getInstance(
                TRANSFORMATION
            )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateSecretKey()
        )

        cipher.updateAAD(
            ASSOCIATED_DATA
        )

        val ciphertext =
            cipher.doFinal(
                plaintext
            )

        val envelope =
            EncryptedSessionEnvelope(
                version =
                    ENVELOPE_VERSION,
                iv =
                    Base64.encodeToString(
                        cipher.iv,
                        Base64.NO_WRAP
                    ),
                ciphertext =
                    Base64.encodeToString(
                        ciphertext,
                        Base64.NO_WRAP
                    )
            )

        val encodedEnvelope =
            json.encodeToString(
                envelope
            ).toByteArray(
                Charsets.UTF_8
            )

        var output = atomicFile.startWrite()

        try {

            output.write(
                encodedEnvelope
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

    private fun getOrCreateSecretKey():
        SecretKey {

        val keyStore =
            KeyStore.getInstance(
                ANDROID_KEYSTORE
            ).apply {
                load(null)
            }

        val existing =
            keyStore.getKey(
                KEY_ALIAS,
                null
            ) as? SecretKey

        if (existing != null) {
            return existing
        }

        val keyGenerator =
            KeyGenerator.getInstance(
                KEY_ALGORITHM,
                ANDROID_KEYSTORE
            )

        val specification =
            android.security.keystore
                .KeyGenParameterSpec
                .Builder(
                    KEY_ALIAS,
                    android.security.keystore
                        .KeyProperties
                        .PURPOSE_ENCRYPT or
                        android.security.keystore
                            .KeyProperties
                            .PURPOSE_DECRYPT
                )
                .setKeySize(
                    KEY_SIZE_BITS
                )
                .setBlockModes(
                    android.security.keystore
                        .KeyProperties
                        .BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    android.security.keystore
                        .KeyProperties
                        .ENCRYPTION_PADDING_NONE
                )
                .setRandomizedEncryptionRequired(
                    true
                )
                .build()

        keyGenerator.init(
            specification
        )

        return keyGenerator
            .generateKey()
    }

    private fun recoverFromUnreadableState() {

        atomicFile.delete()

        runCatching {

            val keyStore =
                KeyStore.getInstance(
                    ANDROID_KEYSTORE
                ).apply {
                    load(null)
                }

            if (
                keyStore.containsAlias(
                    KEY_ALIAS
                )
            ) {
                keyStore.deleteEntry(
                    KEY_ALIAS
                )
            }
        }
    }

    private companion object {

        const val SECURITY_DIRECTORY =
            "security"

        const val SESSION_FILE =
            "session.enc"

        const val ANDROID_KEYSTORE =
            "AndroidKeyStore"

        const val KEY_ALIAS =
            "ppis_session_aes_v1"

        const val KEY_ALGORITHM =
            "AES"

        const val TRANSFORMATION =
            "AES/GCM/NoPadding"

        const val KEY_SIZE_BITS =
            256

        const val GCM_TAG_LENGTH_BITS =
            128

        const val ENVELOPE_VERSION =
            1

        val ASSOCIATED_DATA =
            "com.thevirtualtrust.ppis:session:v1"
                .toByteArray(
                    Charsets.UTF_8
                )
    }
}
