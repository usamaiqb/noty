package com.noty.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noty.app.R
import com.noty.app.data.backup.NoteBackup
import com.noty.app.ui.expressive.ActionRow
import com.noty.app.ui.expressive.AppIdentityRow
import com.noty.app.ui.expressive.CollapsingLargeTopBar
import com.noty.app.ui.expressive.ControlSettingsRow
import com.noty.app.ui.expressive.ExternalLinkRow
import com.noty.app.ui.expressive.SectionGap
import com.noty.app.ui.expressive.SectionHeader
import com.noty.app.ui.expressive.SegmentPosition
import com.noty.app.ui.expressive.SegmentedGroup
import com.noty.app.ui.expressive.SwitchSettingsRow
import com.noty.app.ui.expressive.rememberAppVersionLabel
import com.noty.app.ui.expressive.rememberExpressiveHaptics
import com.noty.app.ui.expressive.rememberExpressiveScrollBehavior
import com.noty.app.utils.ThemeManager
import java.time.LocalDate

private const val GITHUB_REPO_URL = "https://github.com/usamaiqb/noty"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NotyViewModel,
    backupViewModel: BackupViewModel,
    notificationsEnabled: Boolean,
    onTurnOnNotifications: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit
) {
    val currentTheme by viewModel.themeFlow.collectAsStateWithLifecycle(ThemeManager.ThemeMode.SYSTEM)
    val dynamicColors by viewModel.dynamicColorsFlow.collectAsStateWithLifecycle(true)
    val defaultPin by viewModel.defaultPinFlow.collectAsStateWithLifecycle(true)
    val context = LocalContext.current
    val scrollBehavior = rememberExpressiveScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        // Export and import report back here, where they were started.
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CollapsingLargeTopBar(
                title = stringResource(R.string.settings_title),
                scrollBehavior = scrollBehavior,
                onNavigateBack = onNavigateBack,
                navigateBackDescription = stringResource(R.string.cd_back)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(SectionGap)
        ) {
            // The same card as Home, and only while notifications are off: it's a problem
            // to fix, not a preference to browse.
            if (!notificationsEnabled) {
                NotificationsOffCard(onTurnOn = onTurnOnNotifications)
            }

            // ── Appearance ─────────────────────────────────────────────
            Column {
                SectionHeader(stringResource(R.string.settings_section_appearance))
                val supportsDynamicColors = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                // Below 13 there is no per-app language; the app follows the device locale.
                val supportsAppLanguage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                SegmentedGroup {
                    ControlSettingsRow(
                        position = if (supportsDynamicColors) {
                            SegmentPosition.First
                        } else {
                            SegmentPosition.Single
                        },
                        title = stringResource(R.string.settings_theme_title),
                        icon = Icons.Rounded.Palette,
                        subtitle = stringResource(R.string.settings_theme_subtitle)
                    ) {
                        ThemeModeControl(
                            selected = currentTheme,
                            onSelect = viewModel::setTheme
                        )
                    }
                    if (supportsDynamicColors) {
                        SwitchSettingsRow(
                            position = if (supportsAppLanguage) {
                                SegmentPosition.Middle
                            } else {
                                SegmentPosition.Last
                            },
                            title = stringResource(R.string.settings_dynamic_color_title),
                            checked = dynamicColors,
                            onCheckedChange = viewModel::setDynamicColors,
                            icon = Icons.Rounded.Wallpaper,
                            subtitle = stringResource(R.string.settings_dynamic_color_subtitle)
                        )
                    }
                    // 13+ implies 12+, so the dynamic colour row is always above this one.
                    if (supportsAppLanguage) {
                        ActionRow(
                            position = SegmentPosition.Last,
                            title = stringResource(R.string.settings_language_title),
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APP_LOCALE_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                )
                            },
                            icon = Icons.Rounded.Language,
                            subtitle = stringResource(R.string.settings_language_subtitle),
                            trailing = {
                                // Opens system settings, so it gets the leaves-the-app glyph
                                // ExternalLinkRow uses.
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }

            // ── Notes ──────────────────────────────────────────────────
            Column {
                SectionHeader(stringResource(R.string.settings_section_notes))
                SegmentedGroup {
                    SwitchSettingsRow(
                        position = SegmentPosition.Single,
                        title = stringResource(R.string.settings_pin_default_title),
                        checked = defaultPin,
                        onCheckedChange = viewModel::setDefaultPin,
                        icon = Icons.Rounded.PushPin,
                        subtitle = stringResource(
                            if (notificationsEnabled) {
                                R.string.settings_pin_default_subtitle
                            } else {
                                R.string.settings_pin_default_needs_notifications
                            }
                        ),
                        enabled = notificationsEnabled
                    )
                }
            }

            // ── Backup ─────────────────────────────────────────────────
            BackupSection(
                onExport = backupViewModel::exportNotes,
                onImport = backupViewModel::importNotes
            )

            // ── About ──────────────────────────────────────────────────
            Column {
                SectionHeader(stringResource(R.string.settings_section_about))
                SegmentedGroup {
                    ExternalLinkRow(
                        position = SegmentPosition.First,
                        label = stringResource(R.string.settings_github),
                        url = GITHUB_REPO_URL,
                        icon = Icons.Rounded.Code
                    )
                    AppIdentityRow(
                        position = SegmentPosition.Last,
                        appName = stringResource(R.string.app_name),
                        versionLabel = rememberAppVersionLabel(
                            prefix = stringResource(R.string.settings_version_prefix),
                            unknown = stringResource(R.string.settings_version_unknown_name)
                        ),
                        icon = Icons.AutoMirrored.Rounded.Notes,
                        tagline = stringResource(R.string.app_tagline)
                    )
                }
            }
        }
    }
}

/**
 * Export and import through the system file picker, so the user chooses where the file goes
 * (Downloads, a cloud drive, a USB stick) and the app needs no storage permission.
 */
@Composable
private fun BackupSection(onExport: (Uri) -> Unit, onImport: (Uri) -> Unit) {
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(NoteBackup.MIME_TYPE)
    ) { uri -> uri?.let(onExport) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onImport) }

    Column {
        SectionHeader(stringResource(R.string.settings_section_backup))
        SegmentedGroup {
            ActionRow(
                position = SegmentPosition.First,
                title = stringResource(R.string.settings_export_title),
                onClick = { exportLauncher.launch("noty-backup-${LocalDate.now()}.json") },
                icon = Icons.Rounded.FileUpload,
                subtitle = stringResource(R.string.settings_export_subtitle)
            )
            ActionRow(
                position = SegmentPosition.Last,
                title = stringResource(R.string.settings_import_title),
                // Some file providers label .json files as generic binary or text, so a
                // strict JSON filter would hide the user's own backup from them.
                onClick = {
                    importLauncher.launch(
                        arrayOf(NoteBackup.MIME_TYPE, "application/octet-stream", "text/plain")
                    )
                },
                icon = Icons.Rounded.FileDownload,
                subtitle = stringResource(R.string.settings_import_subtitle)
            )
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
    val haptics = rememberExpressiveHaptics()
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
            onClick = {
                haptics.confirm()
                onSelect(ThemeManager.ThemeMode.SYSTEM)
            },
            selected = selected == ThemeManager.ThemeMode.SYSTEM,
            icon = { Icon(Icons.Rounded.BrightnessAuto, contentDescription = null) }
        ) {
            Text(stringResource(R.string.theme_system))
        }
        SegmentedButton(
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
            onClick = {
                haptics.confirm()
                onSelect(ThemeManager.ThemeMode.LIGHT)
            },
            selected = selected == ThemeManager.ThemeMode.LIGHT,
            icon = { Icon(Icons.Rounded.LightMode, contentDescription = null) }
        ) {
            Text(stringResource(R.string.theme_light))
        }
        SegmentedButton(
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
            onClick = {
                haptics.confirm()
                onSelect(ThemeManager.ThemeMode.DARK)
            },
            selected = selected == ThemeManager.ThemeMode.DARK,
            icon = { Icon(Icons.Rounded.DarkMode, contentDescription = null) }
        ) {
            Text(stringResource(R.string.theme_dark))
        }
    }
}
