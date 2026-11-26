package com.amusic.app

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.amusic.features.home.HomeScreen
import com.amusic.features.library.LibraryScreen
import com.amusic.features.player.MiniPlayer
import com.amusic.features.player.NowPlayingScreen
import com.amusic.features.settings.SettingsScreen

private const val PLAYER_ROUTE = "player"

private enum class Destination(val route: String, @param:StringRes val label: Int, val icon: ImageVector) {
    HOME("home", R.string.nav_home, Icons.Filled.Home),
    LIBRARY("library", R.string.nav_library, Icons.AutoMirrored.Filled.List),
    SETTINGS("settings", R.string.nav_settings, Icons.Filled.Settings),
}

@Composable
fun AMusicApp(onLibraryAccess: () -> Unit) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()

    Scaffold(
        bottomBar = {
            if (entry?.destination?.route != PLAYER_ROUTE) {
                Column {
                    MiniPlayer(onOpen = {
                        navController.navigate(PLAYER_ROUTE) { launchSingleTop = true }
                    })
                    NavigationBar {
                        Destination.entries.forEach { destination ->
                            NavigationBarItem(
                                selected = entry?.destination?.hierarchy
                                    ?.any { it.route == destination.route } == true,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(stringResource(destination.label)) },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
            NavHost(navController, startDestination = Destination.HOME.route) {
                composable(Destination.HOME.route) {
                    LibraryAccessGate(onLibraryAccess) { HomeScreen() }
                }
                composable(Destination.LIBRARY.route) {
                    LibraryAccessGate(onLibraryAccess) { LibraryScreen() }
                }
                composable(Destination.SETTINGS.route) { SettingsScreen() }
                composable(PLAYER_ROUTE) {
                    NowPlayingScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
