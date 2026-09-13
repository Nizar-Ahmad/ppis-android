package com.thevirtualtrust.ppis.core.network

import com.thevirtualtrust.ppis.core.error.AppError
import com.thevirtualtrust.ppis.core.network.error.ApiErrorParser
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

@Singleton
class ApiCallExecutor @Inject constructor(
    private val errorParser:
        ApiErrorParser
) {

    suspend fun <T> execute(
        block: suspend () -> T
    ): ApiResult<T> {

        return try {

            ApiResult.Success(
                value = block()
            )

        } catch (
            exception: CancellationException
        ) {

            throw exception

        } catch (
            exception: SocketTimeoutException
        ) {

            ApiResult.Failure(
                AppError.Timeout
            )

        } catch (
            exception: UnknownHostException
        ) {

            ApiResult.Failure(
                AppError.NetworkUnavailable
            )

        } catch (
            exception: ConnectException
        ) {

            ApiResult.Failure(
                AppError.NetworkUnavailable
            )

        } catch (
            exception: HttpException
        ) {

            mapHttpException(
                exception
            )

        } catch (
            exception: SerializationException
        ) {

            ApiResult.Failure(
                AppError.Serialization(
                    causeMessage =
                        exception.message
                )
            )

        } catch (
            exception: IOException
        ) {

            ApiResult.Failure(
                AppError.NetworkUnavailable
            )

        } catch (
            exception: Exception
        ) {

            ApiResult.Failure(
                AppError.Unknown(
                    causeMessage =
                        exception.message
                )
            )
        }
    }

    private fun mapHttpException(
        exception: HttpException
    ): ApiResult.Failure {

        val statusCode =
            exception.code()

        val rawBody =
            try {
                exception
                    .response()
                    ?.errorBody()
                    ?.string()
            } catch (
                ignored: Exception
            ) {
                null
            }

        val parsed =
            errorParser.parse(
                rawBody
            )

        val error =
            when (statusCode) {

                401 ->
                    AppError.Unauthorized

                403 ->
                    AppError.Forbidden

                404 ->
                    AppError.NotFound(
                        detail =
                            parsed.detail
                    )

                409 ->
                    AppError.Conflict(
                        detail =
                            parsed.detail
                    )

                422 ->
                    AppError.Validation(
                        detail =
                            parsed.detail,
                        fieldErrors =
                            parsed.fieldErrors
                    )

                429 ->
                    AppError.RateLimited(
                        detail =
                            parsed.detail
                    )

                in 500..599 ->
                    AppError.Server(
                        statusCode =
                            statusCode,
                        detail =
                            parsed.detail
                    )

                else ->
                    AppError.Http(
                        statusCode =
                            statusCode,
                        detail =
                            parsed.detail
                    )
            }

        return ApiResult.Failure(
            error = error
        )
    }
}
