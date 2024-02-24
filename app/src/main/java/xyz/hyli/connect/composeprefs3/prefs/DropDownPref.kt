package xyz.hyli.connect.composeprefs3.prefs

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.launch
import xyz.hyli.connect.composeprefs3.LocalPrefsDataStore
import xyz.hyli.connect.ui.theme.HyliConnectColorScheme
import xyz.hyli.connect.ui.theme.HyliConnectTypography

/**
 * Preference that shows a list of entries in a DropDown
 *
 * @param key Key used to identify this Pref in the DataStore
 * @param title Main text which describes the Pref
 * @param modifier Modifier applied to the Text aspect of this Pref
 * @param summary Used to give some more information about what this Pref is for
 * @param defaultValue Default selected key if this Pref hasn't been saved already. Otherwise the value from the dataStore is used.
 * @param onValueChange Will be called with the selected key when an item is selected
 * @param useSelectedAsSummary If true, uses the current selected item as the summary. Equivalent of useSimpleSummaryProvider in androidx.
 * @param displayValueAtEnd If true, the current value will be displayed at the end of the Pref.
 * @param dropdownBackgroundColor Color of the dropdown menu
 * @param textColor Text colour of the [title] and [summary]
 * @param enabled If false, this Pref cannot be clicked and the dropdown menu will not show.
 * @param entries Map of keys to values for entries that should be store in the database and shown in the dropdown menu.
 * @param icons List of icons to be shown at the end of each entry
 */
@Composable
fun DropDownPref(
    key: String,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    defaultValue: String? = null,
    onValueChange: ((String) -> Unit)? = null,
    useSelectedAsSummary: Boolean = false,
    displayValueAtEnd: Boolean = false,
    dropdownBackgroundColor: Color? = null,
    textColor: Color = HyliConnectColorScheme().onBackground,
    enabled: Boolean = true,
    entries: LinkedHashMap<Int, String> = linkedMapOf(),
    icons: List<@Composable (() -> Unit)?> = List(entries.size) { null }
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectionKey = intPreferencesKey(key)
    val scope = rememberCoroutineScope()

    val datastore = LocalPrefsDataStore.current
    val prefs by remember { datastore.data }.collectAsState(initial = null)

    var value = defaultValue
    prefs?.get(selectionKey)?.also { value = entries[it] } // starting value if it exists in datastore

    fun edit(item: Pair<Int, String>) = run {
        scope.launch {
            try {
                datastore.edit { preferences ->
                    preferences[selectionKey] = item.first
                }
                expanded = false
                onValueChange?.invoke(item.second)
            } catch (e: Exception) {
                Log.e("DropDownPref", "Could not write pref $key to database. ${e.printStackTrace()}")
            }
        }
    }

    Column {
        TextPref(
            title = title,
            modifier = modifier,
            summary = when {
                useSelectedAsSummary && value != null -> value
                useSelectedAsSummary && value == null -> "Not Set"
                else -> summary
            },
            endText = if (displayValueAtEnd) value else null,
            textColor = textColor,
            enabled = enabled,
            onClick = {
                expanded = true
            },
        )

        Box(
            modifier = Modifier
                .padding(start = 16.dp)
        ) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = if (dropdownBackgroundColor != null) {
                    Modifier.background(
                        dropdownBackgroundColor
                    )
                } else {
                    Modifier
                }
            ) {
                entries.toList().forEachIndexed { index, item ->
                    DropdownMenuItem(
                        onClick = {
                            edit(item)
                        },
                        text = {
                            Text(
                                text = item.second,
                                style = HyliConnectTypography.bodyLarge
                            )
                            if (icons.isNotEmpty() && icons.size == entries.size) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                    icons[index]?.invoke()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
