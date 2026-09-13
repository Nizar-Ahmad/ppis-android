package com.thevirtualtrust.ppis.data.health

import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.core.network.api.HealthApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepository @Inject constructor(
    private val healthApi:
        HealthApi,
    private val apiCallExecutor:
        ApiCallExecutor
) {

    suspend fun checkHealth():
        ApiResult<String> =
        apiCallExecutor.execute {

            healthApi
                .getHealth()
                .status
        }
}
