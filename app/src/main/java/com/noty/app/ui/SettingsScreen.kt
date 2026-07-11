package com.noty.app.ui

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.noty.app.utils.ThemeManager

private const val GithubRepoUrl = "https://github.com/usamaiqb/noty"

// ─── Reusable settings components ─────────────────────────────────────────────

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        content()
    }
}

@Composable
private fun SettingsSurface(
    position: SegmentPosition,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = segmentShape(position)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun RowLeadingIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .padding(end = 16.dp)
            .size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun SwitchSettingsRow(
    position: SegmentPosition,
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val toggle: (Boolean) -> Unit = { newValue ->
        haptics.performHapticFeedback(
            if (newValue) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
        )
        onCheckedChange(newValue)
    }

    SettingsSurface(position = position, onClick = { toggle(!checked) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowLeadingIcon(icon)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = toggle,
                thumbContent = {
                    AnimatedContent(
                        targetState = checked,
                        transitionSpec = {
                            fadeIn(tween(100)) togetherWith fadeOut(tween(100))
                        },
                        label = "switchThumbIcon"
                    ) { isChecked ->
                        Icon(
                            imageVector = if (isChecked) Icons.Rounded.Check else Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun SegmentedSettingsRow(
    position: SegmentPosition,
    icon: ImageVector,
    title: String,
    subtitle: String,
    control: @Composable () -> Unit
) {
    SettingsSurface(position = position) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RowLeadingIcon(icon)
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            control()
        }
    }
}

// ─── Main screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NotyViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentTheme by viewModel.themeFlow.collectAsState(initial = ThemeManager.ThemeMode.SYSTEM)
    val dynamicColors by viewModel.dynamicColorsFlow.collectAsState(initial = true)
    val defaultPin by viewModel.defaultPinFlow.collectAsState(initial = true)
    val versionLabel = remember(context) {
        runCatching {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            "Version ${packageInfo.versionName ?: "1.0"} ($versionCode)"
        }.getOrDefault("Version unknown")
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            LargeTopAppBar(
                title = { Text(text = "Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Appearance ─────────────────────────────────────────────
            Column {
                SectionHeader("Appearance")
                val supportsDynamicColors = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                SettingsGroup {
                    SegmentedSettingsRow(
                        position = if (supportsDynamicColors) SegmentPosition.First else SegmentPosition.Single,
                        icon = Icons.Rounded.Palette,
                        title = "App theme",
                        subtitle = "Overall look of the app"
                    ) {
                        ThemeModeControl(
                            selected = currentTheme,
                            onSelect = viewModel::setTheme
                        )
                    }
                    if (supportsDynamicColors) {
                        SwitchSettingsRow(
                            position = SegmentPosition.Last,
                            icon = Icons.Rounded.Wallpaper,
                            title = "Use dynamic colors",
                            subtitle = "Tint the app from your wallpaper",
                            checked = dynamicColors,
                            onCheckedChange = viewModel::setDynamicColors
                        )
                    }
                }
            }

            // ── Notes ──────────────────────────────────────────────────
            Column {
                SectionHeader("Notes")
                SettingsGroup {
                    SwitchSettingsRow(
                        position = SegmentPosition.Single,
                        icon = Icons.Rounded.PushPin,
                        title = "Pin new notes by default",
                        subtitle = "New notes start pinned as notifications",
                        checked = defaultPin,
                        onCheckedChange = viewModel::setDefaultPin
                    )
                }
            }

            // ── About ──────────────────────────────────────────────────
            Column {
                SectionHeader("About")
                SettingsGroup {
                    val uriHandler = LocalUriHandler.current
                    SettingsSurface(
                        position = SegmentPosition.First,
                        onClick = { uriHandler.openUri(GithubRepoUrl) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Code,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "GitHub",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = GithubRepoUrl.removePrefix("https://"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    SettingsSurface(position = SegmentPosition.Last) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Notes,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Noty",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = versionLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Simple notes, pinned to your notifications.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Preference controls ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeModeControl(
    selected: ThemeManager.ThemeMode,
    onSelect: (ThemeManager.ThemeMode) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onSelect(ThemeManager.ThemeMode.SYSTEM)
            },
            selected = selected == ThemeManager.ThemeMode.SYSTEM,
            icon = { Icon(Icons.Rounded.BrightnessAuto, contentDescription = null) }
        ) {
            Text("System")
        }
        SegmentedButton(
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onSelect(ThemeManager.ThemeMode.LIGHT)
            },
            selected = selected == ThemeManager.ThemeMode.LIGHT,
            icon = { Icon(Icons.Rounded.LightMode, contentDescription = null) }
        ) {
            Text("Light")
        }
        SegmentedButton(
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                onSelect(ThemeManager.ThemeMode.DARK)
            },
            selected = selected == ThemeManager.ThemeMode.DARK,
            icon = { Icon(Icons.Rounded.DarkMode, contentDescription = null) }
        ) {
            Text("Dark")
        }
    }
}
