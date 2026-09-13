import java.util.Properties

val ppisLocalProperties =
    Properties().apply {

        val localPropertiesFile =
            rootProject.file(
                "local.properties"
            )

        if (
            localPropertiesFile.exists()
        ) {

            localPropertiesFile
                .inputStream()
                .use {
                    load(it)
                }
        }
    }

val ppisGoogleWebClientId =
    (
        ppisLocalProperties
            .getProperty(
                "PPIS_GOOGLE_WEB_CLIENT_ID"
            )
            ?: System.getenv(
                "PPIS_GOOGLE_WEB_CLIENT_ID"
            )
            ?: ""
    ).trim()


/*
 * Release signing credentials are supplied only through
 * the process environment.
 *
 * Never store signing passwords in this repository.
 */
val ppisReleaseStoreFile =
    System.getenv(
        "PPIS_RELEASE_STORE_FILE"
    )
        ?.trim()
        ?.takeIf {
            it.isNotEmpty()
        }

val ppisReleaseStorePassword =
    System.getenv(
        "PPIS_RELEASE_STORE_PASSWORD"
    )
        ?.takeIf {
            it.isNotEmpty()
        }

val ppisReleaseKeyAlias =
    System.getenv(
        "PPIS_RELEASE_KEY_ALIAS"
    )
        ?.trim()
        ?.takeIf {
            it.isNotEmpty()
        }

val ppisReleaseKeyPassword =
    System.getenv(
        "PPIS_RELEASE_KEY_PASSWORD"
    )
        ?.takeIf {
            it.isNotEmpty()
        }

val ppisReleaseSigningAvailable =
    ppisReleaseStoreFile != null &&
        ppisReleaseStorePassword != null &&
        ppisReleaseKeyAlias != null &&
        ppisReleaseKeyPassword != null

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.thevirtualtrust.ppis"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.thevirtualtrust.ppis"

        minSdk = 28
        targetSdk = 37

        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"https://ppis.thevirtualtrust.com/\""
        )

        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"" +
                ppisGoogleWebClientId +
                "\""
        )
    }

    signingConfigs {

        if (
            ppisReleaseSigningAvailable
        ) {

            create(
                "ppisRelease"
            ) {

                storeFile =
                    file(
                        ppisReleaseStoreFile!!
                    )

                storePassword =
                    ppisReleaseStorePassword

                keyAlias =
                    ppisReleaseKeyAlias

                keyPassword =
                    ppisReleaseKeyPassword
            }
        }
    }

    buildTypes {
        release {

            if (
                ppisReleaseSigningAvailable
            ) {

                signingConfig =
                    signingConfigs
                        .getByName(
                            "ppisRelease"
                        )
            }

            /*
             * Production artifact:
             *
             * AGP 9.3+ optimization enables both
             * R8 code optimization and optimized
             * resource shrinking.
             */
            optimization {
                enable = true
            }

            isDebuggable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")
    implementation("androidx.browser:browser:1.10.0")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    implementation("androidx.health.connect:connect-client:1.2.0-alpha06")
    implementation(
        platform(libs.androidx.compose.bom)
    )

    implementation(libs.androidx.core.ktx)

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    implementation(
        libs.androidx.lifecycle.runtime.compose
    )

    implementation(
        libs.androidx.lifecycle.viewmodel.ktx
    )

    implementation(
        libs.androidx.activity.compose
    )

    implementation(
        libs.androidx.navigation.compose
    )

    implementation(
        libs.androidx.hilt.lifecycle.viewmodel.compose
    )

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(
        libs.androidx.compose.ui.tooling.preview
    )
    implementation(
        libs.androidx.compose.material3
    )

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.retrofit.core)
    implementation(
        libs.retrofit.kotlinx.serialization
    )

    implementation(libs.okhttp.core)

    implementation(
        libs.kotlinx.serialization.json
    )

    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)

    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )
}
