package xyz.hyli.connect.ui.pages

import android.os.Build
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import compose.icons.CssGgIcons
import compose.icons.LineAwesomeIcons
import compose.icons.cssggicons.AppleWatch
import compose.icons.cssggicons.GlobeAlt
import compose.icons.cssggicons.Laptop
import compose.icons.lineawesomeicons.DesktopSolid
import compose.icons.lineawesomeicons.MobileAltSolid
import compose.icons.lineawesomeicons.TvSolid
import xyz.hyli.connect.R
import xyz.hyli.connect.composeprefs3.PrefsScreen
import xyz.hyli.connect.composeprefs3.prefs.DropDownPref
import xyz.hyli.connect.composeprefs3.prefs.EditIntPref
import xyz.hyli.connect.composeprefs3.prefs.EditTextPref
import xyz.hyli.connect.composeprefs3.prefs.ListPref
import xyz.hyli.connect.composeprefs3.prefs.SwitchPref
import xyz.hyli.connect.datastore.PreferencesDataStore
import xyz.hyli.connect.ui.HyliConnectViewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun settingsScreen(viewModel: HyliConnectViewModel, navController: NavHostController, currentSelect: MutableState<Int>) {
    PrefsScreen(dataStore = PreferencesDataStore.dataStore,
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 12.dp, end = 12.dp)) {
        prefsItem {
            Row(modifier = Modifier
                .fillMaxWidth()) {
                Text(text = stringResource(id = R.string.page_settings),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    letterSpacing = 0.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        prefsGroup("Basic") {
            prefsItem {
                EditTextPref(
                    title = stringResource(id = R.string.page_settings_nickname),
                    dialogTitle = stringResource(id = R.string.page_settings_nickname),
                    dialogMessage = stringResource(id = R.string.page_settings_nickname_dialog_message),
                    key = "nickname",
                    displayValueAtEnd = true
            ) }
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
                    entries = listOf("Android Phone", "Android TV", "Android Wear", "Windows", "Linux", "Mac", "Web"),
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
        prefsGroup("Functions") {
            prefsItem {
                SwitchPref(key = "is_stream", title = stringResource(id = R.string.page_settings_stream))
            }
            prefsItem {
                if (PreferencesDataStore.is_stream.asFlow()
                        .collectAsState(initial = false).value == true
                ) {
                    DropDownPref(
                        title = stringResource(id = R.string.page_settings_stream_method),
                        summary = stringResource(id =  R.string.page_settings_stream_method_summary),
                        key = "stream_method",
                        displayValueAtEnd = true,
                        entries = listOf("Shizuku", "Root")
                    )
                    DropDownPref(
                        title = stringResource(id =  R.string.page_settings_refuse_fullscreen_method),
                        summary = stringResource(id = R.string.page_settings_refuse_fullscreen_method_summary),
                        key = "refuse_fullscreen_method",
                        displayValueAtEnd = true,
                        entries = if (Build.VERSION_CODES.S <= Build.VERSION.SDK_INT) {
                            listOf("Xposed", "Shizuku")
                        } else {
                            listOf("Xposed")
                        }
                    )
                }
            }
            prefsItem {
                SwitchPref(
                    key = "notification_forward",
                    title = stringResource(id =  R.string.page_settings_notification_forward),
                    summary = stringResource(id = R.string.page_settings_notification_forward_summary)
                )
            }
        }
    }
}