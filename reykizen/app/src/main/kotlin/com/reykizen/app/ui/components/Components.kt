package com.reykizen.app.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.reykizen.app.data.*
import com.reykizen.app.ui.MainViewModel
import com.reykizen.app.ui.UiState
import com.reykizen.app.ui.theme.*

// ── Preset Card ──────────────────────────────────────────────────────────────

@Composable
fun PresetCard(
    preset: ResolutionPreset,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (selected) OrangePrimary else Color(0xFFE8E8E8)
    val bgColor     = if (selected) OrangeContainer else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(98.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onSelect() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Tag badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (selected) OrangePrimary else Color(0xFFEEEEEE))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    preset.tag,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else TextTertiary,
                    letterSpacing = 0.5.sp
                )
            }

            Column {
                Text(
                    preset.label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) OrangeDark else TextPrimary
                )
                Text(
                    "${preset.width}×${preset.height}",
                    fontSize = 11.sp,
                    color = if (selected) OrangePrimary else TextSecondary
                )
            }
        }

        if (selected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.TopEnd)
            )
        }
    }
}

// ── Status Card ──────────────────────────────────────────────────────────────

@Composable
fun StatusCard(
    statusMessage: String,
    isApplied: Boolean,
    currentPreset: ResolutionPreset?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isApplied) GreenActive else Color(0xFFDDDDDD))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isApplied) "Override Active" else "Idle",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isApplied) GreenActive else TextSecondary
                )
                Text(
                    statusMessage,
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
            if (isApplied && currentPreset != null) {
                Text(
                    "${currentPreset.width}×${currentPreset.height}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangePrimary
                )
            }
        }
    }
}

// ── Native Display Card ──────────────────────────────────────────────────────

@Composable
fun NativeDisplayCard(ui: UiState) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = OrangePrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Native Display", fontSize = 12.sp,
                    color = TextSecondary, fontWeight = FontWeight.Medium)
                if (ui.nativeWidth > 0) {
                    Text(
                        "${ui.nativeWidth}×${ui.nativeHeight} · ${ui.nativeDensity}dpi",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                } else {
                    Text("Reading…", fontSize = 12.sp, color = TextTertiary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                PrivilegeBadge(root = ui.rootAvailable, shizuku = ui.shizukuAvailable,
                    adb = ui.prefs.privilegeMode == PrivilegeMode.ADB)
            }
        }
    }
}

@Composable
private fun PrivilegeBadge(root: Boolean, shizuku: Boolean, adb: Boolean) {
    val (label, color) = when {
        root    -> Pair("ROOT", GreenActive)
        shizuku -> Pair("SHIZUKU", BlueInfo)
        adb     -> Pair("ADB", Color(0xFF8B5CF6))
        else    -> Pair("NO PRIV", RedError)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color,
            letterSpacing = 0.5.sp)
    }
}

// ── Permission Warning Card ──────────────────────────────────────────────────

@Composable
fun PermissionWarningCard(
    overlayGranted: Boolean,
    accessibilityEnabled: Boolean,
    hasPrivilege: Boolean,
    onOverlayClick: () -> Unit,
    onAccessibilityClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFFF3E0),
        border = BorderStroke(1.dp, Color(0xFFFFCC80))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null,
                    tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Permissions needed", fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold, color = Color(0xFFE65100))
            }
            Spacer(Modifier.height(8.dp))
            if (!overlayGranted) PermRow("Draw over other apps", onOverlayClick)
            if (!accessibilityEnabled) PermRow("Accessibility (auto-detect game)", onAccessibilityClick)
            if (!hasPrivilege) {
                Text("• No root / Shizuku found — enable ADB mode in Settings",
                    fontSize = 12.sp, color = Color(0xFF5D4037))
            }
        }
    }
}

@Composable
private fun PermRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("• $text", fontSize = 12.sp, color = Color(0xFF5D4037),
            modifier = Modifier.weight(1f))
        Icon(Icons.Default.OpenInNew, contentDescription = null,
            tint = OrangePrimary, modifier = Modifier.size(14.dp))
    }
}

// ── Settings Panel ───────────────────────────────────────────────────────────

@Composable
fun SettingsPanel(ui: UiState, vm: MainViewModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Settings", fontWeight = FontWeight.Bold,
                fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(12.dp))

            // Privilege mode selector
            Text("Privilege mode", fontSize = 12.sp, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrivilegeMode.entries.forEach { mode ->
                    val selected = ui.prefs.privilegeMode == mode
                    FilterChip(
                        selected = selected,
                        onClick  = { vm.setPrivilegeMode(mode) },
                        label    = { Text(mode.name, fontSize = 11.sp) },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OrangePrimary,
                            selectedLabelColor     = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(Modifier.height(10.dp))

            // Auto toggles
            SettingsToggle("Auto apply on BloodStrike launch",
                ui.prefs.autoApply, vm::setAutoApply)
            SettingsToggle("Auto restore on BloodStrike exit",
                ui.prefs.autoRestore, vm::setAutoRestore)
            SettingsToggle("Show floating overlay panel",
                ui.prefs.showFloating, vm::setShowFloating)

            if (ui.prefs.privilegeMode == PrivilegeMode.ADB) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = DividerColor)
                Spacer(Modifier.height(10.dp))
                Text("ADB: connect to ${ui.prefs.adbHost}:${ui.prefs.adbPort}",
                    fontSize = 12.sp, color = TextSecondary)
                Text("Enable Wireless debugging in Developer Options first.",
                    fontSize = 11.sp, color = TextTertiary)
            }
        }
    }
}

@Composable
private fun SettingsToggle(label: String, value: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = value,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White,
                checkedTrackColor = OrangePrimary)
        )
    }
}
