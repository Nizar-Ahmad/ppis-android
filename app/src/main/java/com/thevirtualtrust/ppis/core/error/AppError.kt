package com.thevirtualtrust.ppis.core.error

sealed interface AppError {

    data object NetworkUnavailable :
        AppError

    data object Timeout :
        AppError

    data object Unauthorized :
        AppError

    data object Forbidden :
        AppError

    data class NotFound(
        val detail: String?
    ) : AppError

    data class Conflict(
        val detail: String?
    ) : AppError

    data class Validation(
        val detail: String?,
        val fieldErrors: List<FieldError>
    ) : AppError

    data class RateLimited(
        val detail: String?
    ) : AppError

    data class Server(
        val statusCode: Int,
        val detail: String?
    ) : AppError

    data class Http(
        val statusCode: Int,
        val detail: String?
    ) : AppError

    data class Serialization(
        val causeMessage: String?
    ) : AppError

    data class Unknown(
        val causeMessage: String?
    ) : AppError
}

data class FieldError(
    val field: String?,
    val message: String
)
