package com.thevirtualtrust.ppis.core.network.api

import com.thevirtualtrust.ppis.core.network.dto.HealthResponseDto
import retrofit2.http.GET

interface HealthApi {

    @GET("health")
    suspend fun getHealth(): HealthResponseDto
}
