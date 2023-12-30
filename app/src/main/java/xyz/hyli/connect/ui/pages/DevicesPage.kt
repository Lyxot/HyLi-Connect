package xyz.hyli.connect.ui.pages

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import xyz.hyli.connect.R
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.ui.HyliConnectViewModel
import xyz.hyli.connect.ui.HyliConnectViewModelFactory
import xyz.hyli.connect.ui.MainActivity

private lateinit var connectedDeviceMap: MutableMap<String, DeviceInfo>
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun devicesScreen(viewModel: HyliConnectViewModel, navController: NavHostController, currentSelect: MutableState<Int>) {
    val context = LocalContext.current
    connectedDeviceMap = viewModel.connectedDeviceMap
    LazyColumn(modifier = Modifier
        .fillMaxSize()
        .padding(start = 12.dp, end = 12.dp)) {
        item {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)) {
                Text(text = stringResource(id = R.string.page_devices),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    letterSpacing = 0.sp)
            }
        }
        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
        item {
            Row(modifier = Modifier
                .animateItemPlacement(
                    animationSpec = tween(400)
                )
            ) {
                Text(text = stringResource(id = R.string.page_connect_connected_devices))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.padding(horizontal = 2.dp).align(Alignment.CenterVertically))
            }
        }
        items(connectedDeviceMap.values.toList()) { deviceInfo ->
            Text(text = deviceInfo.nickname, modifier = Modifier.fillMaxWidth())
        }
    }
}
