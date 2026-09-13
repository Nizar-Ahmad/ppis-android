package com.thevirtualtrust.ppis.core.device.di

import com.thevirtualtrust.ppis.core.device.AndroidClientInfoProvider
import com.thevirtualtrust.ppis.core.device.ClientInfoProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClientInfoModule {

    @Binds
    @Singleton
    abstract fun bindClientInfoProvider(
        implementation:
            AndroidClientInfoProvider
    ): ClientInfoProvider
}
