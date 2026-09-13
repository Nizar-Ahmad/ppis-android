package com.thevirtualtrust.ppis.navigation

import com.thevirtualtrust.ppis.sync.TelemetrySyncLifecycle

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.thevirtualtrust.ppis.feature.home.HomeScreen
import com.thevirtualtrust.ppis.feature.profile.ProfileScreen
import com.thevirtualtrust.ppis.feature.reports.ReportsScreen
import com.thevirtualtrust.ppis.feature.track.TrackScreen
import com.thevirtualtrust.ppis.R

@Composable
fun PPISNavigation() {

    TelemetrySyncLifecycle()

    val navController = rememberNavController()

    val backStackEntry by
        navController.currentBackStackEntryAsState()

    val currentRoute =
        backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(
                                destination.route
                            ) {
                                popUpTo(
                                    navController
                                        .graph
                                        .findStartDestination()
                                        .id
                                ) {
                                    saveState = true
                                }

                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(
                                    when (destination) {
                                        MainDestination.HOME -> R.drawable.ic_nav_home
                                        MainDestination.TRACK -> R.drawable.ic_nav_track
                                        MainDestination.REPORTS -> R.drawable.ic_nav_reports
                                        MainDestination.PROFILE -> R.drawable.ic_nav_profile
                                    }
                                ),
                                contentDescription = destination.label
                            )
                        },
                        label = {
                            Text(
                                text = destination.label
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination =
                MainDestination.HOME.route,
            modifier = Modifier.padding(
                innerPadding
            )
        ) {
            composable(
                MainDestination.HOME.route
            ) {
                HomeScreen()
            }

            composable(
                MainDestination.TRACK.route
            ) {
                TrackScreen()
            }

            composable(
                MainDestination.REPORTS.route
            ) {
                ReportsScreen()
            }

            composable(
                MainDestination.PROFILE.route
            ) {
                ProfileScreen()
            }
        }
    }
}
