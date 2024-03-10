package xyz.hyli.connect.ui.pages

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat.getString
import androidx.navigation.NavHostController
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.HyliConnect
import xyz.hyli.connect.R
import xyz.hyli.connect.composeprefs3.PrefsScreen
import xyz.hyli.connect.composeprefs3.prefs.DropDownPref
import xyz.hyli.connect.composeprefs3.prefs.EditIntPref
import xyz.hyli.connect.composeprefs3.prefs.EditTextPref
import xyz.hyli.connect.composeprefs3.prefs.ListPref
import xyz.hyli.connect.composeprefs3.prefs.SwitchPref
import xyz.hyli.connect.composeprefs3.prefs.TextPref
import xyz.hyli.connect.datastore.PreferencesDataStore
import xyz.hyli.connect.ui.icon.ExternalLink
import xyz.hyli.connect.ui.icon.Github
import xyz.hyli.connect.ui.theme.HyliConnectColorScheme
import xyz.hyli.connect.ui.theme.HyliConnectTypography
import xyz.hyli.connect.ui.viewmodel.HyliConnectViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SettingsScreen(
    viewModel: HyliConnectViewModel,
    navController: NavHostController,
    paddingValues: PaddingValues = PaddingValues(
        0.dp
    )
) {
    val context = LocalContext.current
    val currentSelect = viewModel.currentSelect
    currentSelect.intValue = 2
    Column(modifier = Modifier.padding(paddingValues)) {
        PrefsScreen(
            dataStore = PreferencesDataStore.dataStore,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            prefsItem {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.page_settings),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        letterSpacing = 0.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            prefsGroup(getString(context, R.string.page_settings_basic)) {
                prefsItem {
                    EditTextPref(
                        title = stringResource(id = R.string.page_settings_nickname),
                        dialogTitle = stringResource(id = R.string.page_settings_nickname),
                        dialogMessage = stringResource(id = R.string.page_settings_nickname_dialog_message),
                        key = "nickname",
                        displayValueAtEnd = true
                    )
                }
                prefsItem {
                    val iconList = mutableListOf<@Composable (() -> Unit)?>()
                    viewModel.platformMap.toList().map { it.second }.forEach {
                        iconList.add {
                            Icon(
                                imageVector = it,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    ListPref(
                        title = stringResource(id = R.string.page_settings_device_type),
                        key = "platform",
                        displayValueAtEnd = true,
                        entries = PreferencesDataStore.platformMap,
                        icons = iconList
                    )
                }
            }
            prefsGroup(getString(context, R.string.page_settings_connect)) {
                prefsItem {
                    EditIntPref(
                        title = stringResource(id = R.string.page_settings_server_port),
                        dialogTitle = stringResource(id = R.string.page_settings_server_port),
                        dialogMessage = stringResource(id = R.string.page_settings_server_port_dialog_message),
                        valueRange = 1024..65535,
                        key = "server_port",
                        displayValueAtEnd = true
                    )
                }
                prefsItem {
                    SwitchPref(
                        key = "nsd_service",
                        title = stringResource(id = R.string.page_settings_nsd_service),
                        summary = stringResource(id = R.string.page_settings_nsd_service_summary)
                    )
                }
            }
            prefsGroup(getString(context, R.string.page_settings_functions)) {
                prefsItem {
                    DropDownPref(
                        title = stringResource(id = R.string.page_settings_working_mode),
                        summary = stringResource(id = R.string.page_settings_working_mode_summary),
                        key = "working_mode",
                        displayValueAtEnd = true,
                        entries = PreferencesDataStore.workingModeMap,
                        onValueChange = {
                            if (it.contains("Shizuku")) {
                                HyliConnect.me.initShizuku()
                            }
                            if (!it.contains("Shizuku") && !it.contains("Root")) {
                                MainScope().launch { PreferencesDataStore.function_app_streaming.reset() }
                            }
                        }
                    )
                }
                prefsItem {
                    SwitchPref(
                        key = "function_app_streaming",
                        enabled = PreferencesDataStore.working_mode.asFlow().collectAsState(initial = 0).value != 0,
                        title = stringResource(id = R.string.page_settings_app_streaming),
                        summary = stringResource(id = R.string.page_settings_app_streaming_summary))
                }
                prefsItem {
                    SwitchPref(
                        key = "function_notification_forward",
                        title = stringResource(id = R.string.page_settings_notification_forward),
                        summary = stringResource(id = R.string.page_settings_notification_forward_summary)
                    )
                }
            }
            prefsGroup(getString(context, R.string.page_settings_other)) {
                prefsItem {
                    SwitchPref(
                        key = "connect_to_myself",
                        title = stringResource(id = R.string.page_settings_connect_to_myself)
                    )
                }
                prefsItem {
                    val showResetDialog = remember { mutableStateOf(false) }
                    TextPref(
                        title = stringResource(id = R.string.page_settings_reset_all_settings),
                        enabled = true,
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        onClick = {
                            showResetDialog.value = true
                        }
                    )
                    if (showResetDialog.value) {
                        AlertDialog(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            onDismissRequest = { showResetDialog.value = false },
                            title = { Text(stringResource(id = R.string.page_settings_reset_all_settings_confirm), style = HyliConnectTypography.titleLarge, modifier = Modifier.padding(16.dp)) },
                            confirmButton = {
                                TextButton(
                                    modifier = Modifier.padding(end = 16.dp),
                                    onClick = {
                                        MainScope().launch { 
                                            PreferencesDataStore.resetAll()
                                        }
                                        showResetDialog.value = false
                                    }
                                ) {
                                    Text(stringResource(id = R.string.composeprefs_confirm), style = HyliConnectTypography.bodyLarge)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    modifier = Modifier.padding(end = 16.dp),
                                    onClick = { showResetDialog.value = false }
                                ) {
                                    Text(stringResource(id = R.string.composeprefs_cancel), style = HyliConnectTypography.bodyLarge)
                                }
                            },
                            properties = DialogProperties(usePlatformDefaultWidth = false),
                            containerColor = HyliConnectColorScheme().background
                        )
                    }
                }
            }
            prefsGroup(getString(context, R.string.page_settings_about)) {
                prefsItem {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
//                    implementation 'com.google.accompanist:accompanist-drawablepainter:0.32.0'
//                    val icon = context.packageManager.getApplicationIcon(BuildConfig.APPLICATION_ID)
//                    Icon(painter = DrawablePainter(icon), contentDescription = null, modifier = Modifier.size(144.dp), tint = Color.Unspecified)
                        Box {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_launcher_background),
                                contentDescription = null,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .size(144.dp),
                                tint = Color.Unspecified
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                modifier = Modifier.size(144.dp),
                                tint = Color.Unspecified
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(id = R.string.app_name),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 34.sp,
                            lineHeight = 40.sp,
                            letterSpacing = 0.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(id = R.string.page_settings_version) + ": " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")"
                        )
                        Text(text = stringResource(id = R.string.author), style = HyliConnectTypography.bodyLarge)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .clickable {
                                    context.startActivity(
                                        Intent(
                                            context,
                                            AboutPage::class.java
                                        )
                                    )
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(24.dp))
                            Text(
                                text = stringResource(id = R.string.page_settings_about) + " " + stringResource(id = R.string.app_name),
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .clickable {
                                    context.startActivity(
                                        Intent().apply {
                                            action = Intent.ACTION_VIEW
                                            data =
                                                Uri.parse(getString(context, R.string.url_github))
                                        }
                                    )
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Github,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(text = "Lyxot/HyliConnect", modifier = Modifier.padding(horizontal = 6.dp))
                            Icon(
                                ExternalLink,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}
