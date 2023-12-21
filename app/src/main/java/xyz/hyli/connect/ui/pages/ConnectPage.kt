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
fun connectScreen() {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(12.dp)) {
        Row {
            Text(text = stringResource(id = R.string.app_name),
                modifier = Modifier
//                .padding(16.dp)
                    .fillMaxWidth(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                letterSpacing = 0.sp)
        }
        Text(text = "Connect")
    }
}