package com.thevirtualtrust.ppis.core.network

import com.thevirtualtrust.ppis.core.error.AppError

sealed interface ApiResult<out T> {

    data class Success<T>(
        val value: T
    ) : ApiResult<T>

    data class Failure(
        val error: AppError
    ) : ApiResult<Nothing>
}
