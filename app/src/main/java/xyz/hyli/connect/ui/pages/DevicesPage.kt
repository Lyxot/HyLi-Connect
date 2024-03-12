package xyz.hyli.connect.ui.pages

import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ks.chan.c2pinyin.LetterCase
import org.ks.chan.c2pinyin.pinyin
import xyz.hyli.connect.HyLiConnect
import xyz.hyli.connect.R
import xyz.hyli.connect.bean.ApplicationInfo
import xyz.hyli.connect.bean.DeviceInfo
import xyz.hyli.connect.proto.ApplicationProto
import xyz.hyli.connect.proto.SocketMessage
import xyz.hyli.connect.socket.SocketUtils
import xyz.hyli.connect.ui.viewmodel.HyLiConnectViewModel
import xyz.hyli.connect.utils.PackageUtils

private lateinit var connectedDeviceMap: MutableMap<String, DeviceInfo>

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DevicesScreen(
    viewModel: HyLiConnectViewModel,
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
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .align(Alignment.CenterVertically)
                    )
                }
            }
            items(connectedDeviceMap.values.toList()) { deviceInfo ->
                val ip = HyLiConnect.uuidMap.filterValues { it == deviceInfo.uuid }.keys.first()
                val applicationInfoList = remember { mutableStateListOf<ApplicationInfo>() }
                LaunchedEffect(deviceInfo) {
                    SocketUtils.sendRequest(ip, SocketMessage.COMMAND.GET_APPLICATION_LIST)
                    SocketUtils.registerReceiveMessageListener(
                        ip,
                        "DevicesPage",
                        SocketMessage.TYPE.RESPONSE,
                        SocketMessage.COMMAND.SEND_APPLICATION_INFO) { messageBody ->
                        ApplicationProto.ApplicationInfo.parseFrom(messageBody.data).let {
                            applicationInfoList.add(ApplicationInfo(
                                it.packageName,
                                it.appName,
                                it.versionName,
                                it.mainActivity,
                                it.icon
                            ))
                        }
                        applicationInfoList.sortBy { it.appName.pinyin(LetterCase.Lower).joinToString().lowercase() }
                    }.let { messageReceiveReceiver ->
                        SocketUtils.registerReceiveMessageListener(ip,
                            "DevicesPage",
                            SocketMessage.TYPE.RESPONSE,
                            SocketMessage.COMMAND.GET_APPLICATION_LIST_FINISHED,
                            true
                        ) {
                            MainScope().launch {
                                delay(500)
                                SocketUtils.unregisterReceiveMessageListener(ip, messageReceiveReceiver)
                                Log.i("DevicesPage", "Application list: ${applicationInfoList.toList()}")
                            }
                        }
                    }
                }
                Text(text = deviceInfo.nickname, modifier = Modifier.fillMaxWidth())
                applicationInfoList.forEach {
                    Row {
                        Text(text = it.appName, modifier = Modifier.fillMaxWidth())
                        Log.i("DevicesPage", "Icon: ${it.icon}")
                        val bitmap = PackageUtils.byteStringToBitmap(it.icon!!)
                        Log.i("DevicesPage", "Bitmap: $bitmap")
                        Image(
                            bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                }
            }
        }
    }
}
