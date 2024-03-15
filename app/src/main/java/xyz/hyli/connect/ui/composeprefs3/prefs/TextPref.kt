package xyz.hyli.connect.ui.composeprefs3.prefs

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import xyz.hyli.connect.ui.composeprefs3.PrefsListItem
import xyz.hyli.connect.ui.composeprefs3.ifNotNullThen
import xyz.hyli.connect.ui.theme.HyLiConnectColorScheme

/**
 * Simple Text with title and summary.
 * Used to show some information to the user and is the basis of all other preferences.
 *
 * @param title Main text which describes the Pref
 * @param modifier Modifier applied to the Text aspect of this Pref
 * @param summary Used to give some more information about what this Pref is for
 * @param endText Text shown at the end of the Pref. No text if null.
 * @param darkenOnDisable If true, text colors have lower opacity when the Pref is not [enabled]
 * @param minimalHeight If true, the height of the pref is reduced, such that other content can be more easily included in the pref.
 * Mostly for internal use with custom Prefs
 * @param onClick Callback for when this Pref is clicked. Will not be called if Pref is not [enabled]
 * @param textColor Text colour of the [title] and [summary]
 * @param enabled If false, this Pref cannot be checked/unchecked
 * @param leadingIcon Icon which is positioned at the start of the Pref
 * @param trailingContent Composable content which is positioned at the end of the Pref
 */
@Composable
fun TextPref(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    endText: String? = null,
    darkenOnDisable: Boolean = false,
    minimalHeight: Boolean = false,
    onClick: () -> Unit = {},
    textColor: Color = HyLiConnectColorScheme().onBackground,
    enabled: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    PrefsListItem(
        text = { Text(title) },
        modifier = if (enabled) modifier.clickable { onClick() } else modifier,
        enabled = enabled,
        darkenOnDisable = darkenOnDisable,
        textColor = textColor,
        minimalHeight = minimalHeight,
        icon = leadingIcon,
        secondaryText = summary.ifNotNullThen { Text(summary!!) },
        trailing = trailingContent,
        endText = endText.ifNotNullThen { Text(endText!!) }
    )
}
