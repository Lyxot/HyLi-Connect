JamalMulla/ComposePrefs3: [https://github.com/JamalMulla/ComposePrefs3](https://github.com/JamalMulla/ComposePrefs3)

## Modify
* Replace `MaterialTheme.colorScheme` with `HyliConnectColorScheme()`
* Replace `MaterialTheme.typography` with `HyliConnectTypography`
* Remove `import androidx.compose.material3.MaterialTheme`
* Remove TODO
* Show the current value of `EditTextPref`, `ListPref`, `MultiSelectListPref`, `DropDownPref` (when `displayValueAtEnd = true`)
* Store Int value in `ListPref`, `DropDownPref`
* Add Icon in `ListPref`, `MultiSelectListPref`, `DropDownPref`
* Forbid an empty input in `EditTextPref`
* Add `EditIntPref` (similar with `EditTextPref`)