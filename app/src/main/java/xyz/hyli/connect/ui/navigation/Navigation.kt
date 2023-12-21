package xyz.hyli.connect.ui.navigation


import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import xyz.hyli.connect.R
import xyz.hyli.connect.ui.pages.connectScreen
import xyz.hyli.connect.ui.pages.devicesScreen
import xyz.hyli.connect.ui.pages.settingsScreen

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun compactScreen(navController: NavHostController, currentSelect: MutableState<Int>) {
    Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = {
        NavigationBar {
            NavigationBarItem(
                icon = { Icon(Icons.Filled.Share, contentDescription = null) },
                label = { Text(stringResource(id = R.string.page_connect)) },
                selected = currentSelect.value == 0,
                onClick = {
                    currentSelect.value = 0
                    navController.navigate("connectScreen") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
            NavigationBarItem(
                icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                label = { Text(stringResource(id = R.string.page_devices)) },
                selected = currentSelect.value == 1,
                onClick = {
                    currentSelect.value = 1
                    navController.navigate("devicesScreen") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                label = { Text(stringResource(id = R.string.page_settings)) },
                selected = currentSelect.value == 2,
                onClick = {
                    currentSelect.value = 2
                    navController.navigate("settingsScreen") {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }, content = {
        innerPadding -> println(innerPadding)
        AnimatedNavHost(navController = navController, startDestination = "connectScreen",
            enterTransition = { slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(500)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(500)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(500)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(500)) }) {
            composable("connectScreen") { connectScreen() }
            composable("devicesScreen") { devicesScreen() }
            composable("settingsScreen") { settingsScreen() }
        }
    })
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun mediumScreen(navController: NavHostController, currentSelect: MutableState<Int>) {
    Scaffold(modifier = Modifier.fillMaxSize(),
        content = {
            innerPadding -> println(innerPadding)
            Row {
                NavigationRail {
                    Spacer(modifier = Modifier.weight(1f))
                    NavigationRailItem(
                        icon = { Icon(Icons.Filled.Share, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.page_connect)) },
                        selected = currentSelect.value == 0,
                        onClick = {
                            currentSelect.value = 0
                            navController.navigate("connectScreen") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationRailItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.page_devices)) },
                        selected = currentSelect.value == 1,
                        onClick = {
                            currentSelect.value = 1
                            navController.navigate("devicesScreen") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationRailItem(
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.page_settings)) },
                        selected = currentSelect.value == 2,
                        onClick = {
                            currentSelect.value = 2
                            navController.navigate("settingsScreen") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
                AnimatedNavHost(navController = navController, startDestination = "connectScreen",
                    enterTransition = { slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(500)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(500)) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(500)) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(500)) }) {
                    composable("connectScreen") { connectScreen() }
                    composable("devicesScreen") { devicesScreen() }
                    composable("settingsScreen") { settingsScreen() }
                }
            }
        }
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun expandedScreen(navController: NavHostController, currentSelect: MutableState<Int>) {
    Scaffold(modifier = Modifier.fillMaxSize(),
        content = {
            innerPadding -> println(innerPadding)
            Row {
                PermanentDrawerSheet {
                    Spacer(modifier = Modifier.weight(1f))
                    NavigationDrawerItem(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        icon = { Icon(Icons.Filled.Share, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.page_connect)) },
                        selected = currentSelect.value == 0,
                        onClick = {
                            currentSelect.value = 0
                            navController.navigate("connectScreen") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationDrawerItem(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.page_devices)) },
                        selected = currentSelect.value == 1,
                        onClick = {
                            currentSelect.value = 1
                            navController.navigate("devicesScreen") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationDrawerItem(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text(stringResource(id = R.string.page_settings)) },
                        selected = currentSelect.value == 2,
                        onClick = {
                            currentSelect.value = 2
                            navController.navigate("settingsScreen") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
                AnimatedNavHost(navController = navController, startDestination = "connectScreen",
                    enterTransition = { slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(500)) },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(500)) },
                    popEnterTransition = { slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }, animationSpec = tween(500)) },
                    popExitTransition = { slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }, animationSpec = tween(500)) }) {
                    composable("connectScreen") { connectScreen() }
                    composable("devicesScreen") { devicesScreen() }
                    composable("settingsScreen") { settingsScreen() }
                }
            }
        }
    )
}