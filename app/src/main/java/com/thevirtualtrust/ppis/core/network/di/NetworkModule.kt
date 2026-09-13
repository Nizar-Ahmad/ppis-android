package com.thevirtualtrust.ppis.core.network.di

import com.thevirtualtrust.ppis.BuildConfig
import com.thevirtualtrust.ppis.core.network.AuthorizationInterceptor
import com.thevirtualtrust.ppis.core.network.TokenRefreshAuthenticator
import com.thevirtualtrust.ppis.core.network.api.HealthApi
import com.thevirtualtrust.ppis.data.auth.remote.AuthApi
import com.thevirtualtrust.ppis.data.auth.remote.AuthRefreshApi
import com.thevirtualtrust.ppis.data.calendar.remote.CalendarApi
import com.thevirtualtrust.ppis.data.googlecalendar.remote.GoogleCalendarApi
import com.thevirtualtrust.ppis.data.googlehealth.remote.GoogleHealthApi
import com.thevirtualtrust.ppis.data.auth.remote.PublicAuthApi
import com.thevirtualtrust.ppis.data.profile.remote.ProfileApi
import com.thevirtualtrust.ppis.data.daily.remote.DailyInputApi
import com.thevirtualtrust.ppis.data.activity.remote.ActivityApi
import com.thevirtualtrust.ppis.data.screentime.remote.ScreenTimeApi
import com.thevirtualtrust.ppis.data.analytics.remote.AnalyticsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            isLenient = false
        }

    @Provides
    @Singleton
    @PublicClient
    fun providePublicOkHttpClient():
        OkHttpClient =
        baseOkHttpBuilder()
            .build()

    @Provides
    @Singleton
    @AuthenticatedClient
    fun provideAuthenticatedOkHttpClient(
        authorizationInterceptor:
            AuthorizationInterceptor,
        tokenRefreshAuthenticator:
            TokenRefreshAuthenticator
    ): OkHttpClient =
        baseOkHttpBuilder()
            .addInterceptor(
                authorizationInterceptor
            )
            .authenticator(
                tokenRefreshAuthenticator
            )
            .build()

    @Provides
    @Singleton
    @PublicRetrofit
    fun providePublicRetrofit(
        json: Json,
        @PublicClient
        client: OkHttpClient
    ): Retrofit =
        createRetrofit(
            json = json,
            client = client
        )

    @Provides
    @Singleton
    @AuthenticatedRetrofit
    fun provideAuthenticatedRetrofit(
        json: Json,
        @AuthenticatedClient
        client: OkHttpClient
    ): Retrofit =
        createRetrofit(
            json = json,
            client = client
        )

    @Provides
    @Singleton
    fun provideHealthApi(
        @PublicRetrofit
        retrofit: Retrofit
    ): HealthApi =
        retrofit.create(
            HealthApi::class.java
        )


    @Provides
    @Singleton
    fun providePublicAuthApi(
        @PublicRetrofit
        retrofit: Retrofit
    ): PublicAuthApi =
        retrofit.create(
            PublicAuthApi::class.java
        )

    @Provides
    @Singleton
    fun provideAuthRefreshApi(
        @PublicRetrofit
        retrofit: Retrofit
    ): AuthRefreshApi =
        retrofit.create(
            AuthRefreshApi::class.java
        )

    @Provides
    @Singleton
    fun provideAuthApi(
        @AuthenticatedRetrofit
        retrofit: Retrofit
    ): AuthApi =
        retrofit.create(
            AuthApi::class.java
        )


    @Provides
    @Singleton
    fun provideProfileApi(
        @AuthenticatedRetrofit
        retrofit: Retrofit
    ): ProfileApi =
        retrofit.create(
            ProfileApi::class.java
        )


    @Provides
    @Singleton
    fun provideDailyInputApi(
        @AuthenticatedRetrofit
        retrofit: Retrofit
    ): DailyInputApi =
        retrofit.create(
            DailyInputApi::class.java
        )


    @Provides
    @Singleton
    fun provideActivityApi(
        @AuthenticatedRetrofit
        retrofit: Retrofit
    ): ActivityApi =
        retrofit.create(
            ActivityApi::class.java
        )



    @Provides
    @Singleton
    fun provideAnalyticsApi(
        @AuthenticatedRetrofit
        retrofit: Retrofit
    ): AnalyticsApi =
        retrofit.create(
            AnalyticsApi::class.java
        )

    @Provides
    @Singleton
    fun provideScreenTimeApi(
        @AuthenticatedRetrofit
        retrofit: Retrofit
    ): ScreenTimeApi =
        retrofit.create(
            ScreenTimeApi::class.java
        )

    @Provides
    @Singleton
    fun provideCalendarApi(
        @AuthenticatedRetrofit
        retrofit: Retrofit
    ): CalendarApi =
        retrofit.create(
            CalendarApi::class.java
        )

    @Provides
    @Singleton
    fun provideGoogleCalendarApi(
        @AuthenticatedRetrofit
        retrofit: Retrofit
    ): GoogleCalendarApi =
        retrofit.create(
            GoogleCalendarApi::class.java
        )

    @Provides
    @Singleton
    fun provideGoogleHealthApi(
        @AuthenticatedRetrofit
        retrofit:
            Retrofit
    ): GoogleHealthApi =
        retrofit.create(
            GoogleHealthApi::class.java
        )


    private fun baseOkHttpBuilder():
        OkHttpClient.Builder =
        OkHttpClient.Builder()
            .connectTimeout(
                20,
                TimeUnit.SECONDS
            )
            .readTimeout(
                20,
                TimeUnit.SECONDS
            )
            .writeTimeout(
                20,
                TimeUnit.SECONDS
            )
            .retryOnConnectionFailure(
                false
            )

    private fun createRetrofit(
        json: Json,
        client: OkHttpClient
    ): Retrofit {

        val contentType =
            "application/json"
                .toMediaType()

        return Retrofit.Builder()
            .baseUrl(
                BuildConfig.API_BASE_URL
            )
            .client(
                client
            )
            .addConverterFactory(
                json.asConverterFactory(
                    contentType
                )
            )
            .build()
    }
}
