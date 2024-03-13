package xyz.hyli.connect.ui.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import xyz.hyli.connect.R
import xyz.hyli.connect.ui.pages.ConnectScreen
import xyz.hyli.connect.ui.pages.DevicesScreen
import xyz.hyli.connect.ui.pages.SettingsScreen
import xyz.hyli.connect.ui.viewmodel.HyLiConnectViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CompactScreen(viewModel: HyLiConnectViewModel, navController: NavHostController) {
    val currentSelect = viewModel.currentSelect
    Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = {
        NavigationBar(modifier = Modifier.navigationBarsPadding()) {
            NavigationBarItem(
                icon = { Icon(Icons.Filled.SettingsEthernet, contentDescription = null) },
                label = { Text(stringResource(id = R.string.page_connect)) },
                selected = currentSelect.intValue == 0,
                onClick = {
                    currentSelect.intValue = 0
                    navController.navigate("connectScreen") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Outlined.Devices, contentDescription = null) },
                label = { Text(stringResource(id = R.string.page_devices)) },
                selected = currentSelect.intValue == 1,
                onClick = {
                    currentSelect.intValue = 1
                    navController.navigate("devicesScreen") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
            NavigationBarItem(
                icon = { Icon(if (currentSelect.intValue != 2) Icons.Outlined.Settings else Icons.Filled.Settings, contentDescription = null) },
                label = { Text(stringResource(id = R.string.page_settings)) },
                selected = currentSelect.intValue == 2,
                onClick = {
                    currentSelect.intValue = 2
                    navController.navigate("settingsScreen") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }, content = { innerPadding ->
        AnimatedNavHost(
            navController = navController,
            startDestination = "connectScreen",
            enterTransition = { fadeIn(animationSpec = tween(400)) },
            exitTransition = { fadeOut(animationSpec = tween(400)) },
            popEnterTransition = { fadeIn(animationSpec = tween(400)) },
            popExitTransition = { fadeOut(animationSpec = tween(400)) }
        ) {
            composable("connectScreen") { ConnectScreen(viewModel, navController, innerPadding) }
            composable("devicesScreen") { DevicesScreen(viewModel, navController, innerPadding) }
            composable("settingsScreen") { SettingsScreen(viewModel, navController, innerPadding) }
        }
    })
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MediumScreen(viewModel: HyLiConnectViewModel, navController: NavHostController) {
    val currentSelect = viewModel.currentSelect
    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail {
            Spacer(modifier = Modifier.weight(1f))
            NavigationRailItem(
                modifier = Modifier.padding(vertical = 8.dp),
                icon = { Icon(Icons.Filled.SettingsEthernet, contentDescription = null) },
                label = { Text(stringResource(id = R.string.page_connect)) },
                selected = currentSelect.intValue == 0,
                onClick = {
                    currentSelect.intValue = 0
                    navController.navigate("connectScreen") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
            NavigationRailItem(
                modifier = Modifier.padding(vertical = 8.dp),
                icon = { Icon(Icons.Outlined.Devices, contentDescription = null) },
                label = { Text(stringResource(id = R.string.page_devices)) },
                selected = currentSelect.intValue == 1,
                onClick = {
                    currentSelect.intValue = 1
                    navController.navigate("devicesScreen") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
            NavigationRailItem(
                modifier = Modifier.padding(vertical = 8.dp),
                icon = { Icon(if (currentSelect.intValue != 2) Icons.Outlined.Settings else Icons.Filled.Settings, contentDescription = null) },
                label = { Text(stringResource(id = R.string.page_settings)) },
                selected = currentSelect.intValue == 2,
                onClick = {
                    currentSelect.intValue = 2
                    navController.navigate("settingsScreen") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            content = { innerPadding ->
                AnimatedNavHost(
                    navController = navController,
                    startDestination = "connectScreen",
                    enterTransition = { fadeIn(animationSpec = tween(400)) },
                    exitTransition = { fadeOut(animationSpec = tween(400)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(400)) },
                    popExitTransition = { fadeOut(animationSpec = tween(400)) }
                ) {
                    composable("connectScreen") { ConnectScreen(viewModel, navController, innerPadding) }
                    composable("devicesScreen") { DevicesScreen(viewModel, navController, innerPadding) }
                    composable("settingsScreen") { SettingsScreen(viewModel, navController, innerPadding) }
                }
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ExpandedScreen(viewModel: HyLiConnectViewModel, navController: NavHostController) {
    val currentSelect = viewModel.currentSelect
    Row(modifier = Modifier.fillMaxSize()) {
        PermanentDrawerSheet {
            Spacer(modifier = Modifier.weight(1f))
            NavigationDrawerItem(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                icon = { Icon(Icons.Filled.SettingsEthernet, contentDescription = null) },
                label = { Text(stringResource(id = R.string.page_connect)) },
                selected = currentSelect.intValue == 0,
                onClick = {
                    currentSelect.intValue = 0
                    navController.navigate("connectScreen") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
            NavigationDrawerItem(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                icon = { Icon(Icons.Outlined.Devices, contentDescription = null) },
                label = { Text(stringResource(id = R.string.page_devices)) },
                selected = currentSelect.intValue == 1,
                onClick = {
                    currentSelect.intValue = 1
                    navController.navigate("devicesScreen") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
            NavigationDrawerItem(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                icon = { Icon(if (currentSelect.intValue != 2) Icons.Outlined.Settings else Icons.Filled.Settings, contentDescription = null) },
                label = { Text(stringResource(id = R.string.page_settings)) },
                selected = currentSelect.intValue == 2,
                onClick = {
                    currentSelect.intValue = 2
                    navController.navigate("settingsScreen") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            content = { innerPadding ->
                AnimatedNavHost(
                    navController = navController,
                    startDestination = "connectScreen",
                    enterTransition = { fadeIn(animationSpec = tween(400)) },
                    exitTransition = { fadeOut(animationSpec = tween(400)) },
                    popEnterTransition = { fadeIn(animationSpec = tween(400)) },
                    popExitTransition = { fadeOut(animationSpec = tween(400)) }
                ) {
                    composable("connectScreen") { ConnectScreen(viewModel, navController, innerPadding) }
                    composable("devicesScreen") { DevicesScreen(viewModel, navController, innerPadding) }
                    composable("settingsScreen") { SettingsScreen(viewModel, navController, innerPadding) }
                }
            }
        )
    }
}
