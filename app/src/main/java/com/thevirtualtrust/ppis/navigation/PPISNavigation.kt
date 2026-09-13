package com.thevirtualtrust.ppis.navigation

import com.thevirtualtrust.ppis.sync.TelemetrySyncLifecycle

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.thevirtualtrust.ppis.feature.home.HomeScreen
import com.thevirtualtrust.ppis.feature.profile.ProfileScreen
import com.thevirtualtrust.ppis.feature.reports.ReportsScreen
import com.thevirtualtrust.ppis.feature.track.TrackScreen

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
                            Text(
                                text = destination.shortLabel
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
