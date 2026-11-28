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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.Color
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
import com.amusic.core.theming.LocalPlayerTheme
import com.amusic.core.theming.NavItem
import com.amusic.features.home.HomeScreen
import com.amusic.features.library.LibraryScreen
import com.amusic.features.player.MiniPlayer
import com.amusic.features.player.NowPlayingScreen
import com.amusic.features.player.PlayerBackdrop
import com.amusic.features.settings.SettingsScreen
import com.amusic.features.settings.ThemesScreen
import com.amusic.features.studio.StudioScreen

private const val PLAYER_ROUTE = "player"
private const val THEMES_ROUTE = "themes"
private const val STUDIO_ROUTE = "studio"

private enum class Destination(val route: String, @param:StringRes val label: Int, val icon: ImageVector) {
    HOME("home", R.string.nav_home, Icons.Filled.Home),
    LIBRARY("library", R.string.nav_library, Icons.AutoMirrored.Filled.List),
    SETTINGS("settings", R.string.nav_settings, Icons.Filled.Settings),
}

@Composable
fun AMusicApp(onLibraryAccess: () -> Unit) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()

    val theme = LocalPlayerTheme.current
    val items = Destination.entries.map { NavItem(stringResource(it.label), it.icon) }
    // The themes screen belongs to settings.
    val selected = Destination.entries.indexOfFirst { destination ->
        entry?.destination?.hierarchy?.any { it.route == destination.route } == true
    }.let { if (it == -1) Destination.SETTINGS.ordinal else it }

    Box(Modifier.fillMaxSize()) {
    theme.Background(Modifier.fillMaxSize())
    if (entry?.destination?.route == PLAYER_ROUTE) PlayerBackdrop()
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (entry?.destination?.route !in listOf(PLAYER_ROUTE, STUDIO_ROUTE)) {
                Column {
                    MiniPlayer(onOpen = {
                        navController.navigate(PLAYER_ROUTE) { launchSingleTop = true }
                    })
                    theme.NavigationBar(items, selected) { index ->
                        navController.navigate(Destination.entries[index].route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
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
                composable(Destination.SETTINGS.route) {
                    SettingsScreen(
                        onOpenThemes = { navController.navigate(THEMES_ROUTE) },
                        onOpenStudio = { navController.navigate(STUDIO_ROUTE) },
                    )
                }
                composable(THEMES_ROUTE) {
                    ThemesScreen(PlayerThemes, onBack = { navController.popBackStack() })
                }
                composable(PLAYER_ROUTE) {
                    NowPlayingScreen(
                        onBack = { navController.popBackStack() },
                        onOpenStudio = { navController.navigate(STUDIO_ROUTE) { launchSingleTop = true } },
                    )
                }
                composable(STUDIO_ROUTE) {
                    StudioScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
    }
}
