package com.thevirtualtrust.ppis.data.reminder

data class ReminderTime(
    val hour: Int,
    val minute: Int
) {

    init {

        require(
            hour in 0..23
        )

        require(
            minute in 0..59
        )
    }

    override fun toString():
        String =
        "%02d:%02d"
            .format(
                hour,
                minute
            )
}

fun parseReminderTime(
    value: String
): ReminderTime? {

    val parts =
        value
            .trim()
            .split(":")

    if (
        parts.size != 2
    ) {
        return null
    }

    val hour =
        parts[0]
            .toIntOrNull()
            ?: return null

    val minute =
        parts[1]
            .toIntOrNull()
            ?: return null

    if (
        hour !in 0..23 ||
        minute !in 0..59
    ) {
        return null
    }

    return ReminderTime(
        hour = hour,
        minute = minute
    )
}
