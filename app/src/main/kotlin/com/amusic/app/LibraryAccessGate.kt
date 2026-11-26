package com.amusic.app

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.amusic.core.design.ScreenScaffold
import com.amusic.data.AudioPermission

/** Shows [content] only with audio access; otherwise explains why it is needed. */
@Composable
fun LibraryAccessGate(onGranted: () -> Unit, content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(AudioPermission.isGranted(context)) }
    val request = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted = it }

    // The user may change the permission in system settings and come back.
    LifecycleResumeEffect(Unit) {
        granted = AudioPermission.isGranted(context)
        onPauseOrDispose {}
    }

    if (granted) {
        LaunchedEffect(Unit) { onGranted() }
        content()
    } else {
        ScreenScaffold(stringResource(R.string.app_name)) {
            Text(stringResource(R.string.access_reason))
            Button(onClick = { request.launch(AudioPermission.name) }) {
                Text(stringResource(R.string.access_allow))
            }
            // After repeated refusals Android stops showing the dialog.
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                },
            ) { Text(stringResource(R.string.access_settings)) }
        }
    }
}
