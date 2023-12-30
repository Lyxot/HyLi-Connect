package xyz.hyli.connect.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import xyz.hyli.connect.R
import xyz.hyli.connect.ui.HyliConnectViewModel

@Composable
fun settingsScreen(viewModel: HyliConnectViewModel, navController: NavHostController, currentSelect: MutableState<Int>) {
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