package com.thevirtualtrust.ppis.core.session

import com.thevirtualtrust.ppis.core.security.SecureTokenStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {

    @Test
    fun initialize_withoutStoredCredentials_becomesSignedOut() =
        runBlocking {

            val store =
                FakeSecureTokenStore()

            val manager =
                SessionManager(store)

            manager.initialize()

            assertEquals(
                SessionState.SignedOut,
                manager.state.value
            )

            assertNull(
                manager.currentCredentials()
            )

            assertTrue(
                manager.isInitialized()
            )
        }

    @Test
    fun initialize_withStoredCredentials_restoresSession() =
        runBlocking {

            val credentials =
                credentials(
                    access = "access-1",
                    refresh = "refresh-1"
                )

            val store =
                FakeSecureTokenStore(
                    initial =
                        credentials
                )

            val manager =
                SessionManager(store)

            manager.initialize()

            assertEquals(
                SessionState.Authenticated(
                    sessionId =
                        "session-id"
                ),
                manager.state.value
            )

            assertEquals(
                credentials,
                manager.currentCredentials()
            )
        }

    @Test
    fun setAuthenticated_persistsAndPublishesSession() =
        runBlocking {

            val store =
                FakeSecureTokenStore()

            val manager =
                SessionManager(store)

            val credentials =
                credentials(
                    access = "access-new",
                    refresh = "refresh-new"
                )

            manager.setAuthenticated(
                credentials
            )

            assertEquals(
                credentials,
                store.value
            )

            assertEquals(
                credentials,
                manager.currentCredentials()
            )

            assertEquals(
                SessionState.Authenticated(
                    sessionId =
                        "session-id"
                ),
                manager.state.value
            )
        }

    @Test
    fun replacingCredentials_replacesAccessAndRefreshTogether() =
        runBlocking {

            val store =
                FakeSecureTokenStore()

            val manager =
                SessionManager(store)

            val first =
                credentials(
                    access = "access-1",
                    refresh = "refresh-1"
                )

            val second =
                credentials(
                    access = "access-2",
                    refresh = "refresh-2"
                )

            manager.setAuthenticated(
                first
            )

            manager.setAuthenticated(
                second
            )

            assertEquals(
                second,
                manager.currentCredentials()
            )

            assertEquals(
                "access-2",
                manager.currentAccessToken()
            )

            assertEquals(
                "refresh-2",
                manager.currentRefreshToken()
            )

            assertEquals(
                second,
                store.value
            )
        }

    @Test
    fun clearSession_clearsDiskMemoryAndState() =
        runBlocking {

            val credentials =
                credentials(
                    access = "access-1",
                    refresh = "refresh-1"
                )

            val store =
                FakeSecureTokenStore(
                    initial =
                        credentials
                )

            val manager =
                SessionManager(store)

            manager.initialize()

            manager.clearSession()

            assertNull(
                store.value
            )

            assertNull(
                manager.currentCredentials()
            )

            assertEquals(
                SessionState.SignedOut,
                manager.state.value
            )
        }

    @Test
    fun initialize_isIdempotent() =
        runBlocking {

            val credentials =
                credentials(
                    access = "access-1",
                    refresh = "refresh-1"
                )

            val store =
                FakeSecureTokenStore(
                    initial =
                        credentials
                )

            val manager =
                SessionManager(store)

            manager.initialize()

            val readCountAfterFirst =
                store.readCount

            manager.initialize()

            assertEquals(
                readCountAfterFirst,
                store.readCount
            )
        }

    private fun credentials(
        access: String,
        refresh: String
    ): SessionCredentials =
        SessionCredentials(
            accessToken =
                access,
            refreshToken =
                refresh,
            sessionId =
                "session-id",
            accessExpiresAtEpochSeconds =
                1_900_000_000L,
            refreshExpiresAtEpochSeconds =
                1_902_592_000L
        )

    private class FakeSecureTokenStore(
        initial:
            SessionCredentials? = null
    ) : SecureTokenStore {

        var value:
            SessionCredentials? =
            initial

        var readCount =
            0

        override suspend fun read():
            SessionCredentials? {

            readCount += 1

            return value
        }

        override suspend fun write(
            credentials:
                SessionCredentials
        ) {
            value =
                credentials
        }

        override suspend fun clear() {
            value =
                null
        }
    }
}
