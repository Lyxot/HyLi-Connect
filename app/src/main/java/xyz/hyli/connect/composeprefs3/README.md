JamalMulla/ComposePrefs3: [https://github.com/JamalMulla/ComposePrefs3/tree/master](https://github.com/JamalMulla/ComposePrefs3/tree/master)

## Modify
* Replace `MaterialTheme.colorScheme` with `HyliConnectColorScheme()`
* Replace `MaterialTheme.typography` with `HyliConnectTypography`
* Remove `import androidx.compose.material3.MaterialTheme`
* Remove TODO
* Show the current value of `EditTextPref`, `ListPref`, `MultiSelectListPref`, `DropDownPref` (when `displayValueAtEnd = true`)
* Use List instead of Map and save value instead of key in `ListPref`, `MultiSelectListPref`, `DropDownPref`
* Add Icon in `ListPref`, `MultiSelectListPref`, `DropDownPref`
* Forbid an empty input in `EditTextPref`
* Add `EditIntPref` (similar with `EditTextPref`)