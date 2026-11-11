package com.reykizen.app.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.reykizen.app.data.*
import com.reykizen.app.ui.MainViewModel
import com.reykizen.app.ui.UiState
import com.reykizen.app.ui.theme.*
import com.reykizen.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel) {
    val ui by vm.ui.collectAsState()
    val ctx = LocalContext.current
    var settingsOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Rey Kizen",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = OrangePrimary
                        )
                        Text(
                            "BloodStrike · Samsung",
                            fontSize = 11.sp,
                            color = TextTertiary,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { settingsOpen = !settingsOpen }) {
                        Icon(
                            if (settingsOpen) Icons.Default.ExpandLess
                            else Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = OrangePrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    scrolledContainerColor = Color.White
                )
            )
        },
        containerColor = OffWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Settings panel (collapsible)
            AnimatedVisibility(
                visible = settingsOpen,
                enter = expandVertically() + fadeIn(),
                exit  = shrinkVertically() + fadeOut()
            ) {
                SettingsPanel(ui = ui, vm = vm)
            }

            // Permission warnings
            if (!ui.overlayGranted || !ui.accessibilityEnabled || !hasPrivilege(ui)) {
                PermissionWarningCard(
                    overlayGranted       = ui.overlayGranted,
                    accessibilityEnabled = ui.accessibilityEnabled,
                    hasPrivilege         = hasPrivilege(ui),
                    onOverlayClick = {
                        ctx.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${ctx.packageName}"))
                        )
                    },
                    onAccessibilityClick = {
                        ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                )
            }

            // Native display info
            NativeDisplayCard(ui = ui)

            Spacer(Modifier.height(4.dp))

            // Status card
            StatusCard(
                statusMessage = ui.statusMessage,
                isApplied     = ui.isApplied,
                currentPreset = ui.selectedPreset
            )

            Spacer(Modifier.height(4.dp))

            // Preset grid header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Presets",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    "BloodStrike optimised",
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }

            // Preset grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(((BLOODSTRIKE_PRESETS.size / 2 + BLOODSTRIKE_PRESETS.size % 2) * 108).dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ui.presets) { preset ->
                    PresetCard(
                        preset     = preset,
                        selected   = preset.id == ui.selectedPreset?.id,
                        onSelect   = { vm.selectPreset(preset) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick  = { vm.applySelectedPreset() },
                    enabled  = !ui.isApplied && ui.selectedPreset != null,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Apply", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick  = { vm.restoreNative() },
                    enabled  = ui.isApplied,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    border   = BorderStroke(1.5.dp,
                        if (ui.isApplied) OrangePrimary else Color(0xFFDDDDDD))
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (ui.isApplied) OrangePrimary else Color(0xFFCCCCCC))
                    Spacer(Modifier.width(6.dp))
                    Text("Restore",
                        color = if (ui.isApplied) OrangePrimary else Color(0xFFCCCCCC))
                }
            }

            // Floating panel toggle
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Button(
                    onClick = {
                        if (ui.floatingRunning) vm.stopFloatingPanel()
                        else vm.startFloatingPanel()
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (ui.floatingRunning) Color(0xFFEF4444) else Color(0xFF1A1A1A)
                    )
                ) {
                    Icon(
                        if (ui.floatingRunning) Icons.Default.VisibilityOff
                        else Icons.Default.Layers,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (ui.floatingRunning) "Stop Overlay" else "Start Overlay",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun hasPrivilege(ui: UiState) = ui.rootAvailable || ui.shizukuAvailable ||
        (ui.prefs.privilegeMode == PrivilegeMode.ADB)
