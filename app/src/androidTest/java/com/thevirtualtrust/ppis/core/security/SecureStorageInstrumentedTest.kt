package com.thevirtualtrust.ppis.core.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thevirtualtrust.ppis.core.session.SessionCredentials
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SecureStorageInstrumentedTest {

    private val context =
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext

    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    @Test
    fun tokenStore_roundTripsAndClears():
        Unit =
        runBlocking {

            val store =
                KeystoreSecureTokenStore(
                    context = context,
                    json = json
                )

            store.clear()

            val credentials =
                SessionCredentials(
                    accessToken =
                        "test-access-token",
                    refreshToken =
                        "test-refresh-token",
                    sessionId =
                        "test-session-id",
                    accessExpiresAtEpochSeconds =
                        1_900_000_000L,
                    refreshExpiresAtEpochSeconds =
                        1_902_592_000L
                )

            store.write(
                credentials
            )

            val restored =
                store.read()

            assertEquals(
                credentials,
                restored
            )

            store.clear()

            assertNull(
                store.read()
            )
        }

    @Test
    fun tokenStore_replacesEntireCredentialSet():
        Unit =
        runBlocking {

            val store =
                KeystoreSecureTokenStore(
                    context = context,
                    json = json
                )

            store.clear()

            val first =
                SessionCredentials(
                    accessToken = "access-1",
                    refreshToken = "refresh-1",
                    sessionId = "session-1",
                    accessExpiresAtEpochSeconds = 100L,
                    refreshExpiresAtEpochSeconds = 200L
                )

            val second =
                SessionCredentials(
                    accessToken = "access-2",
                    refreshToken = "refresh-2",
                    sessionId = "session-1",
                    accessExpiresAtEpochSeconds = 300L,
                    refreshExpiresAtEpochSeconds = 400L
                )

            store.write(first)
            store.write(second)

            assertEquals(
                second,
                store.read()
            )

            store.clear()
        }

    @Test
    fun installationId_isStableAndValid():
        Unit =
        runBlocking {

            val firstProvider =
                InstallationIdProvider(
                    context
                )

            val first =
                firstProvider
                    .getOrCreate()

            val secondProvider =
                InstallationIdProvider(
                    context
                )

            val second =
                secondProvider
                    .getOrCreate()

            assertEquals(
                first,
                second
            )

            assertTrue(
                runCatching {
                    UUID.fromString(first)
                }.isSuccess
            )
        }
}
