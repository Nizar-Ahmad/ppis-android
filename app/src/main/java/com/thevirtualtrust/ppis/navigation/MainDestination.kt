package com.thevirtualtrust.ppis.navigation

enum class MainDestination(
    val route: String,
    val label: String,
    val shortLabel: String
) {
    HOME(
        route = "home",
        label = "Home",
        shortLabel = "H"
    ),

    TRACK(
        route = "track",
        label = "Track",
        shortLabel = "T"
    ),

    REPORTS(
        route = "reports",
        label = "Reports",
        shortLabel = "R"
    ),

    PROFILE(
        route = "profile",
        label = "Profile",
        shortLabel = "P"
    )
}
