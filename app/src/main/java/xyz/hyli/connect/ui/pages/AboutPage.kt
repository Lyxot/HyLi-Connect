package xyz.hyli.connect.ui.pages

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import xyz.hyli.connect.BuildConfig
import xyz.hyli.connect.R
import xyz.hyli.connect.ui.theme.HyliConnectTheme
import xyz.hyli.connect.ui.theme.HyliConnectTypography

class AboutPage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            HyliConnectTheme {
                Scaffold {
                    Column(modifier = Modifier.fillMaxSize().padding(it)) {
                        Row(horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier
                                    .clickable { finish() }
                                    .padding(12.dp)
                                    .size(24.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.page_settings_about) + " " + stringResource(id = R.string.app_name),
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                        var scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .verticalScroll(scrollState)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(48.dp))
                                Box {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_launcher_background),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .size(144.dp),
                                        tint = Color.Unspecified
                                    )
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                        contentDescription = null,
                                        modifier = Modifier.size(144.dp),
                                        tint = Color.Unspecified
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(id = R.string.app_name),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 34.sp,
                                    lineHeight = 40.sp,
                                    letterSpacing = 0.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(id = R.string.page_settings_version) + ": " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")"
                                )
                                Text(
                                    text = stringResource(id = R.string.author),
                                    style = HyliConnectTypography.bodyLarge
                                )
                                Text(
                                    text = stringResource(id = R.string.uri_homepage),
                                    modifier = Modifier
                                        .padding(top = 6.dp)
                                        .clickable {
                                            startActivity(
                                                Intent().apply {
                                                    action = Intent.ACTION_VIEW
                                                    data =
                                                        Uri.parse("https://" + getString(R.string.uri_homepage))
                                                }
                                            )
                                        },
                                    style = HyliConnectTypography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                /* TODO: description */
                            }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(id = R.string.page_about_homepage),
                                    modifier = Modifier
                                        .clickable {
                                            startActivity(
                                                Intent().apply {
                                                    action = Intent.ACTION_VIEW
                                                    data =
                                                        Uri.parse("https://" + getString(R.string.uri_homepage))
                                                }
                                            )
                                        },
                                    style = HyliConnectTypography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(id = R.string.page_about_source_code),
                                    modifier = Modifier
                                        .clickable {
                                            startActivity(
                                                Intent().apply {
                                                    action = Intent.ACTION_VIEW
                                                    data =
                                                        Uri.parse(getString(R.string.url_github))
                                                }
                                            )
                                        },
                                    style = HyliConnectTypography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(id = R.string.page_about_donate),
                                    modifier = Modifier
                                        .clickable {
                                            /* TODO: donate */
                                        },
                                    style = HyliConnectTypography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(id = R.string.page_about_feedback),
                                    modifier = Modifier
                                        .clickable {
                                            startActivity(
                                                Intent().apply {
                                                    action = Intent.ACTION_VIEW
                                                    data =
                                                        Uri.parse(getString(R.string.url_github_issues))
                                                }
                                            )
                                        },
                                    style = HyliConnectTypography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(id = R.string.page_about_changelog),
                                    modifier = Modifier
                                        .clickable {
                                            /* TODO: change log */
                                        },
                                    style = HyliConnectTypography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(id = R.string.page_about_license),
                                    modifier = Modifier
                                        .clickable {
                                            startActivity(
                                                Intent().apply {
                                                    action = Intent.ACTION_VIEW
                                                    data =
                                                        Uri.parse(getString(R.string.url_license_agpl3_0))
                                                }
                                            )
                                        },
                                    style = HyliConnectTypography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(id = R.string.page_about_license_notice),
                                    modifier = Modifier
                                        .clickable {
                                            /* TODO: license notice */
                                        },
                                    style = HyliConnectTypography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
