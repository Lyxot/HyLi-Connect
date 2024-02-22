package xyz.hyli.connect.composeprefs3.prefs

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.launch
import xyz.hyli.connect.composeprefs3.LocalPrefsDataStore
import xyz.hyli.connect.ui.theme.HyliConnectColorScheme
import xyz.hyli.connect.ui.theme.HyliConnectTypography

/**
 * Preference that shows a list of entries in a Dialog where multiple entries can be selected at one time.
 *
 * @param key Key used to identify this Pref in the DataStore
 * @param title Main text which describes the Pref. Shown above the summary and in the Dialog.
 * @param modifier Modifier applied to the Text aspect of this Pref
 * @param summary Used to give some more information about what this Pref is for
 * @param defaultValue Default selected key if this Pref hasn't been saved already. Otherwise the value from the dataStore is used.
 * @param onValuesChange Will be called with the [Set] of selected keys when an item is selected/unselected
 * @param displayValueAtEnd If true, the current value will be displayed at the end of the Pref.
 * @param dialogBackgroundColor Background color of the Dialog
 * @param textColor Text colour of the [title] and [summary]
 * @param enabled If false, this Pref cannot be clicked and the Dialog cannot be shown.
 * @param entries Map of keys to values for entries that should be store in the database and shown in the dialog.
 * @param icons List of icons to be shown at the end of each entry
 */
@ExperimentalComposeUiApi
@Composable
fun MultiSelectListPref(
    key: String,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    defaultValue: Set<String> = setOf(),
    onValuesChange: ((Set<String>) -> Unit)? = null,
    displayValueAtEnd: Boolean = false,
    dialogBackgroundColor: Color = HyliConnectColorScheme().surface,
    textColor: Color = HyliConnectColorScheme().onBackground,
    enabled: Boolean = true,
    entries: LinkedHashMap<String, String> = linkedMapOf(),
    icons: List<@Composable (() -> Unit)?> = List(entries.size) { null }
) {

    var showDialog by rememberSaveable { mutableStateOf(false) }
    val selectionKey = stringSetPreferencesKey(key)
    val scope = rememberCoroutineScope()

    val datastore = LocalPrefsDataStore.current
    val prefs by remember { datastore.data }.collectAsState(initial = null)

    var selected = defaultValue
    prefs?.get(selectionKey)?.also { selected = it } // starting value if it exists in datastore

    fun edit(isSelected: Boolean, current: Pair<String, String>) = run {
        scope.launch {
            try {
                val result = when (!isSelected) {
                    true -> selected + current.first
                    false -> selected - current.first
                }
                datastore.edit { preferences ->
                    preferences[selectionKey] = result
                }
                onValuesChange?.invoke(result)
                selected = result
            } catch (e: Exception) {
                Log.e(
                    "MultiSelectListPref",
                    "Could not write pref $key to database. ${e.printStackTrace()}"
                )
            }
        }
    }

    TextPref(
        title = title,
        modifier = modifier,
        summary = summary,
        endText = if (displayValueAtEnd) selected.joinToString(", ") else null,
        textColor = textColor,
        enabled = true,
        onClick = { if (enabled) showDialog = !showDialog },
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            text = {
                Column {
                    Text(modifier = Modifier.padding(vertical = 16.dp), text = title, style = HyliConnectTypography.titleLarge)
                    LazyColumn {
                        val entriesList = entries.toList()
                        items(entriesList) { current ->
                            val isSelected = selected.contains(current.first)
                            val onSelectionChanged = {
                                edit(isSelected, current)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = isSelected,
                                        onClick = { onSelectionChanged() }
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onSelectionChanged() },
                                    colors = CheckboxDefaults.colors(checkedColor = HyliConnectColorScheme().primary)
                                )
                                Text(
                                    text = current.second,
                                    style = HyliConnectTypography.bodyMedium,
                                    color = textColor
                                )
                                if (icons.isNotEmpty() && icons.size == entries.size) {
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                        icons[entriesList.indexOf(current)]?.invoke()
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showDialog = false },
                ) {
                    Text(text = "Select", style = HyliConnectTypography.bodyLarge)
                }
            },
            containerColor = dialogBackgroundColor,
            properties = DialogProperties(
                usePlatformDefaultWidth = true
            )
        )
    }
}