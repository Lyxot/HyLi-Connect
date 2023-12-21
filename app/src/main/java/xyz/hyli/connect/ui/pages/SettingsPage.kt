package xyz.hyli.connect.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.hyli.connect.R

@Composable
fun settingsScreen() {
    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .padding(start = 12.dp, end = 12.dp)) {
        item {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)) {
                Text(text = stringResource(id = R.string.page_settings),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    letterSpacing = 0.sp)
            }
        }
        item {
            Text(text = "Settings")
        }
    }
}