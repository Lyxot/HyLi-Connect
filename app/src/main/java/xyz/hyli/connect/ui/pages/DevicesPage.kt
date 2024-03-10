package xyz.hyli.connect.ui.pages

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import xyz.hyli.connect.R
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.ui.viewmodel.HyliConnectViewModel

private lateinit var connectedDeviceMap: MutableMap<String, DeviceInfo>

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DevicesScreen(
    viewModel: HyliConnectViewModel,
    navController: NavHostController,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val currentSelect = viewModel.currentSelect
    currentSelect.intValue = 1
    connectedDeviceMap = viewModel.connectedDeviceMap
    Column(modifier = Modifier.padding(paddingValues)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.page_devices),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        letterSpacing = 0.sp
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {
                Row(
                    modifier = Modifier
                        .animateItemPlacement(
                            animationSpec = tween(400)
                        )
                ) {
                    Text(text = stringResource(id = R.string.page_connect_connected_devices))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.padding(horizontal = 2.dp).align(Alignment.CenterVertically)
                    )
                }
            }
            items(connectedDeviceMap.values.toList()) { deviceInfo ->
                Text(text = deviceInfo.nickname, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
