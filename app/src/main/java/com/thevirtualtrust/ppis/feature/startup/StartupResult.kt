package com.thevirtualtrust.ppis.feature.startup

sealed interface StartupResult {

    data object SignedOut :
        StartupResult

    data object Authenticated :
        StartupResult

    data object UnsupportedAccount :
        StartupResult

    data object TemporarilyUnavailable :
        StartupResult
}
