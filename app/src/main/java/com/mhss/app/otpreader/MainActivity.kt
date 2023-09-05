package com.mhss.app.otpreader

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mhss.app.otpreader.model.InstalledApp
import com.mhss.app.otpreader.ui.theme.OTPReaderTheme
import dagger.hilt.android.AndroidEntryPoint

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OTPReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val notificationPermissionEnabled by remember {
                        mutableStateOf(
                            Settings.Secure
                                .getString(this.contentResolver, "enabled_notification_listeners")
                                .contains(applicationContext.packageName)
                        )
                    }
                    var bottomSheetOpen by remember { mutableStateOf(false) }
                    val installedApps by viewModel.apps.collectAsStateWithLifecycle()
                    val mustContainText by viewModel.mustContainText.collectAsStateWithLifecycle()

                    if (notificationPermissionEnabled) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    stringResource(R.string.included_apps),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Button(onClick = { bottomSheetOpen = true }) {
                                    Text(
                                        stringResource(R.string.select_apps),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            Divider(Modifier.padding(vertical = 12.dp))
                            Text(
                                text = stringResource(R.string.extract_if_message_contains),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            OutlinedTextField(
                                value = mustContainText,
                                onValueChange = {
                                    viewModel.saveContains(it)
                                },
                                modifier = Modifier
                                    .fillMaxWidth(),
                                label = {
                                    Text(
                                        stringResource(R.string.extract_if_message_contains_hint),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        if (bottomSheetOpen) {
                            ModalBottomSheet(
                                onDismissRequest = { bottomSheetOpen = false },
                                windowInsets = WindowInsets(top = 80.dp)
                            ) {
                                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.searchApp(it) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    label = { Text(stringResource(R.string.search)) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent
                                    )
                                )
                                LazyColumn(
                                    contentPadding = PaddingValues(8.dp)
                                ) {
                                    items(installedApps, key = { it.packageName }) { app ->
                                        InstalledAppCard(
                                            installedApp = app,
                                            selected = app.included,
                                            onSelected = {
                                                viewModel.onAppSelected(app, it)
                                            }
                                        )
                                    }
                                    item(key = "s") {
                                        Spacer(Modifier.height(100.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        NotificationPermissionMessage()
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationPermissionMessage() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.notification_access_message),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                context.startActivity(
                    Intent(
                        "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
                    )
                )
            }
        ) {
            Text(
                text = stringResource(R.string.enable),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun InstalledAppCard(
    installedApp: InstalledApp,
    selected: Boolean,
    onSelected: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            AsyncImage(
                model = installedApp.iconUri,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = installedApp.name,
                style = MaterialTheme.typography.bodyMedium,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Checkbox(
            checked = selected,
            onCheckedChange = {
                onSelected(it)
            },
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}