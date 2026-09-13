package com.thevirtualtrust.ppis.core.security.di

import com.thevirtualtrust.ppis.core.security.KeystoreSecureTokenStore
import com.thevirtualtrust.ppis.core.security.SecureTokenStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(
    SingletonComponent::class
)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindSecureTokenStore(
        implementation:
            KeystoreSecureTokenStore
    ): SecureTokenStore
}
