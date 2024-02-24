package xyz.hyli.connect.ui.pages

import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.core.content.ContextCompat.getString
import androidx.navigation.NavHostController
import compose.icons.CssGgIcons
import compose.icons.LineAwesomeIcons
import compose.icons.cssggicons.AppleWatch
import compose.icons.cssggicons.CornerRightUp
import compose.icons.cssggicons.GlobeAlt
import compose.icons.cssggicons.Laptop
import compose.icons.lineawesomeicons.DesktopSolid
import compose.icons.lineawesomeicons.Github
import compose.icons.lineawesomeicons.MobileAltSolid
import compose.icons.lineawesomeicons.TvSolid
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.HyliConnect
import xyz.hyli.connect.R
import xyz.hyli.connect.composeprefs3.PrefsScreen
import xyz.hyli.connect.composeprefs3.prefs.DropDownPref
import xyz.hyli.connect.composeprefs3.prefs.EditIntPref
import xyz.hyli.connect.composeprefs3.prefs.EditTextPref
import xyz.hyli.connect.composeprefs3.prefs.ListPref
import xyz.hyli.connect.composeprefs3.prefs.SwitchPref
import xyz.hyli.connect.datastore.PreferencesDataStore
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
                    listOf(
                        LineAwesomeIcons.MobileAltSolid,
                        LineAwesomeIcons.TvSolid,
                        CssGgIcons.AppleWatch,
                        LineAwesomeIcons.DesktopSolid,
                        LineAwesomeIcons.DesktopSolid,
                        CssGgIcons.Laptop,
                        CssGgIcons.GlobeAlt
                    ).forEach {
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
            }
            prefsGroup(getString(context, R.string.page_settings_connect)) {
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
                    SwitchPref(key = "is_stream", title = stringResource(id = R.string.page_settings_stream))
                }
                prefsItem {
                    if (PreferencesDataStore.is_stream.asFlow()
                            .collectAsState(initial = false).value == true
                    ) {
                        DropDownPref(
                            title = stringResource(id = R.string.page_settings_app_stream_method),
                            summary = stringResource(id = R.string.page_settings_app_stream_method_summary),
                            key = "app_stream_method",
                            displayValueAtEnd = true,
                            entries = if (Build.VERSION_CODES.S <= Build.VERSION.SDK_INT || BuildConfig.DEBUG) {
                                PreferencesDataStore.appStreamMethodMap
                            } else {
                                linkedMapOf(
                                    0 to "Root + Xposed",
                                    1 to "Shizuku + Xposed"
                                )
                            },
                            onValueChange = {
                                if (it.contains("Shizuku")) {
                                    HyliConnect.me.initShizuku()
                                }
                            }
                        )
                    }
                }
                prefsItem {
                    SwitchPref(
                        key = "notification_forward",
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
                                            data = Uri.parse(getString(context, R.string.url_github))
                                        }
                                    )
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                LineAwesomeIcons.Github,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(text = "Lyxot/HyliConnect", modifier = Modifier.padding(horizontal = 6.dp))
                            Icon(
                                CssGgIcons.CornerRightUp,
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
