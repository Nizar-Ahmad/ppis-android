package com.thevirtualtrust.ppis.core.network.error

import com.thevirtualtrust.ppis.core.error.FieldError
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ParsedApiError(
    val detail: String?,
    val fieldErrors: List<FieldError>
)

@Singleton
class ApiErrorParser @Inject constructor(
    private val json: Json
) {

    fun parse(
        rawBody: String?
    ): ParsedApiError {

        if (rawBody.isNullOrBlank()) {
            return ParsedApiError(
                detail = null,
                fieldErrors = emptyList()
            )
        }

        return try {

            val root =
                json.parseToJsonElement(
                    rawBody
                )

            val rootObject =
                root as? JsonObject

            val detailElement =
                rootObject?.get("detail")

            when (detailElement) {

                is JsonPrimitive -> {
                    ParsedApiError(
                        detail =
                            detailElement
                                .contentOrNull,
                        fieldErrors =
                            emptyList()
                    )
                }

                is JsonArray -> {

                    val fieldErrors =
                        detailElement
                            .mapNotNull { item ->

                                val itemObject =
                                    item as? JsonObject
                                        ?: return@mapNotNull null

                                val message =
                                    itemObject["msg"]
                                        ?.jsonPrimitive
                                        ?.contentOrNull
                                        ?: return@mapNotNull null

                                val location =
                                    itemObject["loc"]
                                        as? JsonArray

                                val field =
                                    location
                                        ?.mapNotNull {
                                            (
                                                it
                                                    as? JsonPrimitive
                                            )
                                                ?.contentOrNull
                                        }
                                        ?.dropWhile {
                                            it == "body" ||
                                                it == "query" ||
                                                it == "path"
                                        }
                                        ?.joinToString(
                                            separator = "."
                                        )
                                        ?.takeIf {
                                            it.isNotBlank()
                                        }

                                FieldError(
                                    field = field,
                                    message = message
                                )
                            }

                    ParsedApiError(
                        detail =
                            fieldErrors
                                .firstOrNull()
                                ?.message,
                        fieldErrors =
                            fieldErrors
                    )
                }

                else -> {
                    ParsedApiError(
                        detail = null,
                        fieldErrors =
                            emptyList()
                    )
                }
            }

        } catch (
            exception: Exception
        ) {

            ParsedApiError(
                detail = null,
                fieldErrors = emptyList()
            )
        }
    }
}
