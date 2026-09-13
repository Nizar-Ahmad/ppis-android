# PPIS Android

Android application for the **Personal Pattern Intelligence System (PPIS)**.

PPIS is a productivity and personal-pattern analysis platform that combines objective device telemetry with optional subjective daily input to generate productivity, stress, activity, sleep, meeting-load, and digital-behavior analytics.

The Android client is built with Kotlin and Jetpack Compose and communicates with the PPIS FastAPI backend hosted at: https://ppis.thevirtualtrust.com/

Project Overview

PPIS is designed to reduce dependence on manual self-reporting by collecting data automatically whenever the Android platform or connected services can provide it.

The application combines:

Health and physical activity data
Screen-time behavior
Calendar and meeting activity
Daily subjective check-ins
Productivity analytics
Stress indicators
Daily, weekly, and monthly reports
Automatic background synchronization

Manual input is used only for information that cannot reliably be obtained automatically, such as mood, energy, personal notes, or subjective focus information.

Technology Stack
Android
Kotlin
Jetpack Compose
Material 3
Android SDK
Hilt Dependency Injection
Retrofit
OkHttp
Kotlin Serialization
Kotlin Coroutines
StateFlow
ViewModel
WorkManager
AlarmManager
DataStore
Android Keystore
Credential Manager
Device Integrations
Health Connect
Android UsageStats
Android Calendar Provider
Android Notification APIs
Android Background Work APIs
Google Integrations
Google Sign-In
Google Credential Manager
Google Calendar
Google Health integration
Backend

The mobile application communicates with the PPIS backend:

https://ppis.thevirtualtrust.com/

Backend repository:

https://github.com/Nizar-Ahmad/ppis-backend

The backend is responsible for:

Authentication
Session management
OTP verification
Google authentication
User profiles
Telemetry storage
Calendar synchronization
Google Health synchronization
Analytics
Reports
Notification preferences
Email delivery
Scheduled backend jobs
Application ID
com.thevirtualtrust.ppis
Features
Authentication

PPIS supports a complete authentication lifecycle.

Email Authentication
User registration
Email/password login
OTP verification
Optional login OTP
Password reset
Session restoration
Access-token refresh
Refresh-token rotation
Google Sign-In

Google Sign-In is implemented using Android Credential Manager.

Flow:

Google Account
      ↓
Google Credential Manager
      ↓
Google ID Token
      ↓
POST /auth/google
      ↓
PPIS Session
      ↓
Secure local token storage

The Google ID token is used only for authentication and is not persisted by the Android application.

Session Management

PPIS maintains secure authenticated sessions.

Features include:

Secure token persistence
Automatic session restoration
Access-token refresh
Refresh-token rotation
Current session logout
Logout from all sessions
Revoke other sessions
View authenticated sessions

Authentication credentials are stored using Android secure storage backed by the Android Keystore.

Daily Tracking

PPIS supports optional subjective daily tracking.

Users can record:

Mood
Sleep information
Energy
Focused work
Personal notes

These values complement automatically collected telemetry.

Manual values are not required when the corresponding metric can be derived reliably from device or integration data.

Health Tracking
Health Connect

Health Connect is the primary Android health-data source.

The application can automatically read:

Steps
Activity data
Activity duration

Health Connect data is synchronized with the PPIS backend.

Health Connect remains the preferred device source when multiple health providers contain overlapping information.

Google Health

PPIS also supports optional Google Health synchronization.

Google Health acts as an additional cloud-based source.

The Android application never stores Google Health OAuth access tokens.

OAuth credentials are handled server-side by the PPIS backend.

Screen Time Tracking

PPIS uses Android UsageStats to analyze digital behavior.

The application can collect:

Total daily screen time
Night screen time
Top used applications

Example:

Total Screen Time
Night Screen Time
Top Used Apps

Usage Access must be granted by the user through Android system settings.

PPIS does not require Accessibility Service access for screen-time tracking.

Calendar Tracking

PPIS supports two calendar sources.

Google Calendar

Users can connect their Google Calendar account.

The PPIS backend handles OAuth and calendar synchronization.

Android Local Calendar

The Android application can also read calendar events available through the Android Calendar Provider.

The synchronization layer includes duplicate-event handling so that the same event is not counted twice when it exists through multiple calendar sources.

Calendar data contributes to metrics such as:

Meeting duration
Meeting load
Daily schedule intensity
Productivity analysis
Automatic Telemetry Synchronization

PPIS is designed to synchronize automatically.

The user should not need to manually refresh telemetry during normal use.

Startup Synchronization

When the authenticated application starts, PPIS performs a rolling synchronization window covering:

Today + previous 7 days

This allows missed dates to be backfilled automatically.

Foreground Refresh

When the user returns to the application after sufficient time, today's telemetry is refreshed.

This allows the Home screen to display current productivity information before the day has ended.

Background Synchronization

WorkManager is used as a recovery and background synchronization mechanism.

The application schedules periodic telemetry synchronization approximately every:

6 hours

Background work requires network connectivity when server synchronization is required.

End-of-Day Processing

PPIS performs an automatic end-of-day workflow.

The Android scheduler targets approximately:

00:15

in the user's configured PPIS profile timezone.

The alarm is intentionally inexact to avoid requiring exact-alarm permission.

When the end-of-day workflow executes:

AlarmManager
      ↓
EndOfDayReceiver
      ↓
WorkManager
      ↓
Restore PPIS Session
      ↓
Synchronize Previous Day + Current Day
      ↓
Fetch Previous-Day Analytics
      ↓
Display Daily Report Notification

The receiver immediately schedules the next end-of-day alarm.

End-of-Day Notification

After successful end-of-day analytics retrieval, PPIS can display a local Android notification containing information such as:

Report date
Productivity score
Stress status

The Android notification system is independent from backend email reports.

Reminder System

PPIS also includes an optional user-configurable daily check-in reminder.

This reminder is separate from the automatic end-of-day synchronization.

Users can choose a local reminder time for optional manual daily input.

The reminder system supports restoration after:

Device reboot
Time change
Timezone change
Timezone Handling

PPIS uses the timezone stored in the user's PPIS profile as the canonical timezone for daily boundaries.

This is important for:

Daily telemetry
Activity data
Screen-time data
Calendar data
Analytics
End-of-day reports

The application avoids relying on the device timezone when PPIS profile timezone information is available.

Analytics

PPIS provides daily, weekly, and monthly analytics.

Daily Analytics

Daily analytics can include:

Productivity score
Stress index
Sleep score
Activity score
Meeting-load score
Distraction score
Data coverage
Weekly Analytics

Weekly reports aggregate daily information and provide broader behavioral insights.

Examples include:

Weekly productivity
Weekly stress
Best day
Worst day
Activity patterns
Meeting-load patterns
Screen-time patterns
Weekly insights
Monthly Analytics

Monthly analytics provide longer-term trend summaries.

The backend prevents future dates from incorrectly influencing current weekly or monthly analytics.

Home Dashboard

The Home screen presents the latest daily analytics and synchronization state.

Telemetry can be refreshed automatically when:

The application starts
The application returns to foreground
A synchronization completes

This keeps daily productivity information aligned with recently collected telemetry.

Reports

The Reports area supports:

Weekly reports
Monthly reports
Historical navigation
Current-period automatic refresh

Current reports automatically refresh after successful telemetry synchronization.

Historical reports remain stable unless explicitly reloaded.

Notifications and Email Reports

PPIS supports several notification mechanisms.

Android Notifications

Examples:

Daily check-in reminder
Daily report notification
Backend Email Reports

The backend independently processes report-email preferences.

Email delivery can include scheduled daily, weekly, or monthly communication depending on user preferences.

Android background execution is not required for the backend to send server-generated email reports.

Architecture

The Android application follows a layered architecture.

┌──────────────────────────────┐
│        Jetpack Compose       │
│             UI               │
└──────────────┬───────────────┘
               │
               ↓
┌──────────────────────────────┐
│          ViewModels          │
│       StateFlow / Events     │
└──────────────┬───────────────┘
               │
               ↓
┌──────────────────────────────┐
│     Repositories / Sync      │
│         Coordinators         │
└──────────────┬───────────────┘
               │
       ┌───────┴─────────┐
       ↓                 ↓
┌───────────────┐ ┌────────────────┐
│ Android APIs  │ │ Retrofit APIs  │
└───────┬───────┘ └───────┬────────┘
        │                 │
        ↓                 ↓
┌───────────────┐ ┌────────────────┐
│ Device Data   │ │ PPIS Backend   │
└───────────────┘ └────────────────┘
Main Source Structure

The project is organized around several major packages:

app/src/main/java/com/thevirtualtrust/ppis/
│
├── core/
├── data/
├── feature/
├── sync/
└── MainActivity.kt
core

Contains shared infrastructure such as:

Networking
Session management
Secure token storage
Device information
Error handling
data

Contains:

Repositories
Retrofit APIs
DTOs
Health Connect data sources
UsageStats data sources
Calendar data sources
Google integration repositories
feature

Contains UI and ViewModel logic for application features such as:

Authentication
Home
Tracking
Reports
Profile
Startup
sync

Contains automatic synchronization infrastructure including:

TelemetrySyncCoordinator
WorkManager workers
Periodic synchronization
End-of-day synchronization
Alarm scheduling
Telemetry Architecture

Automatic telemetry collection is coordinated centrally.

Conceptually:

TelemetrySyncCoordinator
        │
        ├── Health Connect
        │
        ├── UsageStats
        │
        ├── Google Health
        │
        ├── Google Calendar
        │
        └── Local Calendar
                │
                ↓
           PPIS Backend
                │
                ↓
             Analytics

A synchronization mutex prevents overlapping telemetry synchronization runs.

Data Source Priority

When multiple sources provide similar information, PPIS avoids blindly overwriting higher-priority data.

For health activity:

Health Connect
      ↓
Google Health

Health Connect is treated as the primary device source.

Google Health acts as an optional secondary cloud source.

Security

Security considerations include:

Secure PPIS token persistence
Android Keystore-backed credential storage
Google ID tokens are not persisted
Google integration OAuth tokens remain server-side
Release signing credentials are not committed
local.properties is excluded from Git
Build outputs are excluded from Git
Environment files are excluded from Git

Never commit:

local.properties
*.jks
*.keystore
.env
API secrets
Signing passwords
Private keys
Configuration
Google Web Client ID

Create or modify:

local.properties

and add:

PPIS_GOOGLE_WEB_CLIENT_ID=YOUR_GOOGLE_WEB_CLIENT_ID

The Android Google Web Client ID must match the client ID expected by the PPIS backend.

Release Signing

Release signing credentials are supplied through environment variables.

PPIS_RELEASE_STORE_FILE
PPIS_RELEASE_STORE_PASSWORD
PPIS_RELEASE_KEY_ALIAS
PPIS_RELEASE_KEY_PASSWORD

Signing credentials must never be stored in the repository.

Build Requirements

The application uses the Gradle wrapper included in the repository.

Build from the project root.

Compile Kotlin
./gradlew compileDebugKotlin
Build Debug APK
./gradlew assembleDebug
Install Debug Build

With an Android device connected through ADB:

./gradlew installDebug
Build Release
./gradlew assembleRelease
Testing
Unit Tests
./gradlew testDebugUnitTest
Android Lint
./gradlew lintDebug
Connected Android Tests

With an Android device connected:

./gradlew connectedDebugAndroidTest
Complete Mobile QA

The repository includes a consolidated QA script:

./scripts/final_qa.sh

The final mobile QA covered:

Kotlin compilation
Unit tests
Android lint
Debug build
Release build
Real-device installation
Startup validation
Android permission validation
Health Connect validation
Usage Access validation
Automatic telemetry synchronization
End-of-day alarm scheduling
Connected Android tests
Permissions

PPIS may use the following Android capabilities depending on enabled features:

Internet access
Notification permission
Calendar read permission
Usage Access
Health Connect permissions
Boot completed receiver
Alarm scheduling
Background WorkManager execution

Some capabilities such as Usage Access are special Android permissions and must be enabled by the user through system settings.

Health Connect Permissions

Health Connect permissions are requested only for health data needed by PPIS.

The application can operate with partial telemetry when some health information is unavailable.

Usage Access

Screen-time collection requires Android Usage Access.

The application checks access through AppOpsManager and supports Android API 28 compatibility.

UsageStats are used instead of an Accessibility Service.

Background Reliability

Android background execution is best-effort.

Synchronization reliability is improved through multiple mechanisms:

Foreground synchronization
        +
Startup backfill
        +
Periodic WorkManager
        +
End-of-day WorkManager
        +
Backend scheduled jobs

This reduces dependence on any single Android scheduling mechanism.

Force-stopping the application can prevent Android-scheduled work until the application is opened again.

Current Project Status

The PPIS Android implementation is feature-complete for the current project scope.

Final validation has successfully covered:

Kotlin Compilation              PASS
Unit Tests                      PASS
Android Lint                    PASS
Debug Build                     PASS
Release Build                   PASS
Real Device Installation        PASS
Startup Crash Check             PASS
Usage Access                    PASS
Calendar Permission             PASS
Notification Permission         PASS
Health Connect Permission       PASS
Automatic Telemetry Sync        PASS
End-of-Day Scheduling           PASS
Connected Android Tests         PASS
Google Sign-In                  PASS
Repository

Android repository:

https://github.com/Nizar-Ahmad/ppis-android

Backend repository:

https://github.com/Nizar-Ahmad/ppis-backend
PPIS

Personal Pattern Intelligence System

A platform for combining personal telemetry, behavioral patterns, calendar information, health information, and subjective daily signals into practical productivity and wellbeing analytics.