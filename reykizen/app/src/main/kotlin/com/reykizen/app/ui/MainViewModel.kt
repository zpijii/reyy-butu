package com.reykizen.app.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.lifecycle.*
import com.reykizen.app.data.*
import com.reykizen.app.resolution.*
import com.reykizen.app.service.FloatingPanelService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import rikka.shizuku.Shizuku

data class UiState(
    val prefs:          ReyKizenPrefs     = ReyKizenPrefs(),
    val presets:        List<ResolutionPreset> = BLOODSTRIKE_PRESETS,
    val selectedPreset: ResolutionPreset? = null,
    val isApplied:      Boolean           = false,
    val statusMessage:  String            = "Ready",
    val nativeWidth:    Int               = 0,
    val nativeHeight:   Int               = 0,
    val nativeDensity:  Int               = 0,
    val rootAvailable:  Boolean           = false,
    val shizukuAvailable: Boolean         = false,
    val overlayGranted: Boolean           = false,
    val accessibilityEnabled: Boolean     = false,
    val floatingRunning: Boolean          = false,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx: Context get() = getApplication()
    private val resManager = ResolutionManager(ctx)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            // Collect prefs
            ctx.prefsFlow().collect { p ->
                val preset = BLOODSTRIKE_PRESETS.find { it.id == p.selectedPresetId }
                _ui.update { it.copy(prefs = p, selectedPreset = preset) }
            }
        }
        viewModelScope.launch { probeCapabilities() }
    }

    // ── Capability probing ───────────────────────────────────────────────────

    suspend fun probeCapabilities() = withContext(Dispatchers.IO) {
        val (nW, nH) = resManager.getNativeResolution()
        val nDpi     = resManager.getNativeDensity()
        val hasRoot  = resManager.probePrivilegeMode() == PrivilegeMode.ROOT
        val hasShizuku = runCatching {
            Shizuku.pingBinder() &&
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

        val overlayOk = Settings.canDrawOverlays(ctx)
        val a11yOk    = isAccessibilityEnabled()

        _ui.update {
            it.copy(
                nativeWidth          = nW,
                nativeHeight         = nH,
                nativeDensity        = nDpi,
                rootAvailable        = hasRoot,
                shizukuAvailable     = hasShizuku,
                overlayGranted       = overlayOk,
                accessibilityEnabled = a11yOk
            )
        }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.contains("${ctx.packageName}/.service.GameMonitorService",
            ignoreCase = true)
    }

    // ── Preset selection ─────────────────────────────────────────────────────

    fun selectPreset(preset: ResolutionPreset) {
        viewModelScope.launch {
            ctx.updatePrefs {
                this[PrefsKeys.SELECTED_PRESET_ID] = preset.id
            }
        }
    }

    // ── Apply / Restore ──────────────────────────────────────────────────────

    fun applySelectedPreset() {
        val preset = _ui.value.selectedPreset ?: return
        val mode   = _ui.value.prefs.privilegeMode
        viewModelScope.launch {
            _ui.update { it.copy(statusMessage = "Applying ${preset.label}…") }
            when (val r = resManager.applyPreset(preset, mode)) {
                is ResolutionResult.Success -> _ui.update {
                    it.copy(isApplied = true,
                        statusMessage = "${preset.width}×${preset.height} · ${preset.density}dpi active")
                }
                is ResolutionResult.Failure -> _ui.update {
                    it.copy(statusMessage = "Error: ${r.reason.take(60)}")
                }
            }
        }
    }

    fun restoreNative() {
        val mode = _ui.value.prefs.privilegeMode
        viewModelScope.launch {
            _ui.update { it.copy(statusMessage = "Restoring…") }
            when (val r = resManager.restoreNative(mode)) {
                is ResolutionResult.Success -> _ui.update {
                    it.copy(isApplied = false, statusMessage = "Restored to native")
                }
                is ResolutionResult.Failure -> _ui.update {
                    it.copy(statusMessage = "Restore failed: ${r.reason.take(50)}")
                }
            }
        }
    }

    // ── Settings updates ─────────────────────────────────────────────────────

    fun setPrivilegeMode(mode: PrivilegeMode) {
        viewModelScope.launch {
            ctx.updatePrefs { this[PrefsKeys.PRIVILEGE_MODE] = mode.name }
        }
    }

    fun setAutoApply(v: Boolean) {
        viewModelScope.launch { ctx.updatePrefs { this[PrefsKeys.AUTO_APPLY] = v } }
    }

    fun setAutoRestore(v: Boolean) {
        viewModelScope.launch { ctx.updatePrefs { this[PrefsKeys.AUTO_RESTORE] = v } }
    }

    fun setShowFloating(v: Boolean) {
        viewModelScope.launch { ctx.updatePrefs { this[PrefsKeys.SHOW_FLOATING] = v } }
    }

    // ── Floating service ─────────────────────────────────────────────────────

    fun startFloatingPanel() {
        val intent = Intent(ctx, FloatingPanelService::class.java)
        ctx.startForegroundService(intent)
        _ui.update { it.copy(floatingRunning = true) }
    }

    fun stopFloatingPanel() {
        ctx.startService(
            Intent(ctx, FloatingPanelService::class.java)
                .setAction(FloatingPanelService.ACTION_STOP)
        )
        _ui.update { it.copy(floatingRunning = false) }
    }
}
