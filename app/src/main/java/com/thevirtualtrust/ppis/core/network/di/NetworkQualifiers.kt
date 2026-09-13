package com.thevirtualtrust.ppis.core.network.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedRetrofit
