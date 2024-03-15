package xyz.hyli.connect.ui.composeprefs3.prefs

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import xyz.hyli.connect.R
import xyz.hyli.connect.ui.composeprefs3.LocalPrefsDataStore
import xyz.hyli.connect.ui.theme.HyLiConnectColorScheme
import xyz.hyli.connect.ui.theme.HyLiConnectTypography

/**
 * Preference which shows a TextField in a Dialog
 *
 * @param key Key used to identify this Pref in the DataStore
 * @param title Main text which describes the Pref
 * @param modifier Modifier applied to the Text aspect of this Pref
 * @param summary Used to give some more information about what this Pref is for
 * @param dialogTitle Title shown in the dialog. No title if null.
 * @param dialogMessage Summary shown underneath [dialogTitle]. No summary if null.
 * @param defaultValue Default value that will be set in the TextField when the dialog is shown for the first time.
 * @param onValueSaved Will be called with new TextField value when the confirm button is clicked. It is NOT called every time the value changes. Use [onValueChange] for that.
 * @param onValueChange Will be called every time the TextField value is changed.
 * @param valueRange Int range of the TextField value. If null, there is no range.
 * @param displayValueAtEnd If true, the current value will be displayed at the end of the Pref.
 * @param dialogBackgroundColor Color of the dropdown menu
 * @param textColor Text colour of the [title] and [summary]
 * @param enabled If false, this Pref cannot be clicked.
 */
@ExperimentalComposeUiApi
@Composable
fun EditIntPref(
    key: String,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    dialogTitle: String? = null,
    dialogMessage: String? = null,
    defaultValue: Int = 0,
    onValueSaved: ((Int) -> Unit) = {},
    onValueChange: ((Int) -> Unit) = {},
    valueRange: ClosedRange<Int>? = null,
    displayValueAtEnd: Boolean = false,
    dialogBackgroundColor: Color = HyLiConnectColorScheme().background,
    textColor: Color = HyLiConnectColorScheme().onBackground,
    enabled: Boolean = true,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val selectionKey = intPreferencesKey(key)
    val scope = rememberCoroutineScope()

    val datastore = LocalPrefsDataStore.current
    val prefs by remember { datastore.data }.collectAsState(initial = null)

    // value should only change when save button is clicked
    var value by remember { mutableIntStateOf(defaultValue) }
    // value of the TextField which changes every time the text is modified
    var textVal by remember { mutableStateOf(value.toString()) }
    // int value of the TextField which changes every time the text is modified
    var intVal by remember { mutableIntStateOf(value) }

    var dialogSize by remember { mutableStateOf(Size.Zero) }

    // Set value initially if it exists in datastore
    LaunchedEffect(Unit) {
        prefs?.get(selectionKey)?.also {
            value = it
        }
    }

    LaunchedEffect(datastore.data) {
        datastore.data.collectLatest { pref ->
            pref[selectionKey]?.also {
                value = it
            }
        }
    }

    fun edit() = run {
        scope.launch {
            try {
                datastore.edit { preferences ->
                    preferences[selectionKey] = intVal
                }
                onValueSaved(intVal)
            } catch (e: Exception) {
                Log.e(
                    "EditIntPref",
                    "Could not write pref $key to database. ${e.printStackTrace()}"
                )
            }
        }
    }

    TextPref(
        title = title,
        modifier = modifier,
        summary = summary,
        textColor = textColor,
        enabled = enabled,
        onClick = { if (enabled) showDialog = !showDialog },
        endText = if (displayValueAtEnd) value.toString() else null,
    )

    if (showDialog) {
        // reset
        LaunchedEffect(null) {
            textVal = value.toString()
        }
        AlertDialog(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .onGloballyPositioned {
                    dialogSize = it.size.toSize()
                },
            onDismissRequest = { showDialog = false },
            title = { DialogHeader(dialogTitle, dialogMessage) },
            text = {
                OutlinedTextField(
                    value = textVal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    onValueChange = {
                        textVal = it
                        try {
                            intVal = it.toInt()
                            onValueChange(intVal)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = if (textVal.isEmpty()) {
                        false
                    } else {
                        try {
                            textVal.toInt() in valueRange!!
                        } catch (e: Exception) {
                            false
                        }
                    },
                    modifier = Modifier.padding(end = 16.dp),
                    onClick = {
                        try {
                            if (textVal.isNotEmpty() && textVal.toInt() in valueRange!!) {
                                edit()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        showDialog = false
                    }
                ) {
                    Text(stringResource(id = R.string.composeprefs_save), style = HyLiConnectTypography.bodyLarge)
                }
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier.padding(end = 16.dp),
                    onClick = { showDialog = false }
                ) {
                    Text(stringResource(R.string.composeprefs_cancel), style = HyLiConnectTypography.bodyLarge)
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            containerColor = dialogBackgroundColor,
        )
    }
}
