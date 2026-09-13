package com.thevirtualtrust.ppis.data.activity

enum class ActivitySource(
    val apiValue: String
) {

    MANUAL(
        apiValue = "manual"
    ),

    HEALTH_CONNECT(
        apiValue = "health_connect"
    ),

    GOOGLE_HEALTH(
        apiValue = "google_health"
    );

    companion object {

        fun fromApiValue(
            value: String
        ): ActivitySource =
            when (
                value
            ) {

                "health_connect" ->
                    HEALTH_CONNECT

                "google_health" ->
                    GOOGLE_HEALTH

                else ->
                    MANUAL
            }
    }
}
