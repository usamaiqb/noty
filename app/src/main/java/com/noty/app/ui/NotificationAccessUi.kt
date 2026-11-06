package com.noty.app.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.noty.app.R
import com.noty.app.utils.NotificationAccess

/**
 * Compose-side view of [NotificationAccess]: whether notes can be pinned right now, and the
 * one way to fix it. Created once in [NotyApp] and passed down, so every screen reads the
 * same value.
 */
@Stable
class NotificationAccessState internal constructor(initiallyEnabled: Boolean) {
    /** Whether notes can be shown in the status bar. Rechecked every time the app resumes. */
    var enabled by mutableStateOf(initiallyEnabled)
        internal set

    internal var showDialog by mutableStateOf(false)
    internal var turnOnAction: () -> Unit = {}

    /** Explains why pinning is unavailable, with a way to turn notifications on. */
    fun explain() {
        showDialog = true
    }

    /**
     * Asks Android directly while it will still show its permission prompt, and opens the
     * matching settings screen once it won't (or when only the Notes channel is blocked).
     */
    fun turnOn() = turnOnAction()
}

/**
 * @param onEnabled called when notifications come back on, so pinned notes can be re-posted.
 */
@Composable
fun rememberNotificationAccess(onEnabled: () -> Unit): NotificationAccessState {
    val context = LocalContext.current
    val state = remember { NotificationAccessState(NotificationAccess.canPostNotes(context)) }
    val currentOnEnabled by rememberUpdatedState(onEnabled)

    val refresh = {
        val nowEnabled = NotificationAccess.canPostNotes(context)
        if (nowEnabled && !state.enabled) currentOnEnabled()
        state.enabled = nowEnabled
    }

    // Covers every way back into the app: from system settings, from the permission
    // prompt, or after the user changed notification settings while Noty was in the background.
    LifecycleResumeEffect(Unit) {
        refresh()
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh() }

    state.turnOnAction = {
        // The version check sits here rather than only inside the helper so lint can see
        // that POST_NOTIFICATIONS (API 33) is never requested below it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && canStillAskForPermission(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            context.startActivity(NotificationAccess.settingsIntent(context))
        }
    }

    return state
}

/**
 * Android shows the notification permission prompt at most twice. MainActivity asks on
 * first launch, so by the time the user taps a pin control the first answer is in: a
 * rationale flag means they declined once and may still be asked again; otherwise the
 * prompt will not appear and only settings can help.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun canStillAskForPermission(context: Context): Boolean {
    val granted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
    if (granted) return false // Only the Notes channel can be off; that's a settings fix.
    val activity = context.findActivity() ?: return false
    return ActivityCompat.shouldShowRequestPermissionRationale(
        activity, Manifest.permission.POST_NOTIFICATIONS
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Shown when the user reaches for a pin control while notifications are off. */
@Composable
fun NotificationAccessDialog(state: NotificationAccessState) {
    if (!state.showDialog) return
    AlertDialog(
        onDismissRequest = { state.showDialog = false },
        icon = { Icon(Icons.Rounded.NotificationsOff, contentDescription = null) },
        title = { Text(stringResource(R.string.notifications_off_dialog_title)) },
        text = { Text(stringResource(R.string.notifications_off_dialog_message)) },
        confirmButton = {
            TextButton(
                onClick = {
                    state.showDialog = false
                    state.turnOn()
                }
            ) {
                Text(stringResource(R.string.notifications_turn_on))
            }
        },
        dismissButton = {
            TextButton(onClick = { state.showDialog = false }) {
                Text(stringResource(R.string.action_not_now))
            }
        }
    )
}

/**
 * The status while notifications are off, at the top of both Home and Settings. One component
 * so the two screens say the same thing the same way.
 *
 * It has a single action, so the whole card is the click target; "Turn on" is a label that
 * shows the card is tappable rather than a second, nested button.
 */
@Composable
fun NotificationsOffCard(onTurnOn: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onTurnOn,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(Icons.Rounded.NotificationsOff, contentDescription = null)
            Text(
                text = stringResource(R.string.notifications_off_banner),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.notifications_turn_on),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
