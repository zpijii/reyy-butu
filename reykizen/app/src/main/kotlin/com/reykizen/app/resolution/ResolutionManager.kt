package com.reykizen.app.resolution

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import com.reykizen.app.data.PrivilegeMode
import com.reykizen.app.data.ResolutionPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ResolutionResult {
    object Success : ResolutionResult()
    data class Failure(val reason: String) : ResolutionResult()
}

class ResolutionManager(private val context: Context) {

    private val rootEngine    = RootEngine()
    private val shizukuEngine = ShizukuEngine(context)
    private val adbEngine     = AdbEngine()

    /** Current physical display dimensions in pixels. */
    fun getNativeResolution(): Pair<Int, Int> {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        return Pair(metrics.widthPixels, metrics.heightPixels)
    }

    fun getNativeDensity(): Int {
        val metrics = context.resources.displayMetrics
        return metrics.densityDpi
    }

    suspend fun applyPreset(
        preset: ResolutionPreset,
        mode: PrivilegeMode
    ): ResolutionResult = withContext(Dispatchers.IO) {
        val w = preset.width
        val h = preset.height
        val dpi = if (preset.density > 0) preset.density else getNativeDensity()
        when (mode) {
            PrivilegeMode.ROOT    -> rootEngine.setResolution(w, h, dpi)
            PrivilegeMode.SHIZUKU -> shizukuEngine.setResolution(w, h, dpi)
            PrivilegeMode.ADB     -> adbEngine.setResolution(w, h, dpi)
        }
    }

    suspend fun restoreNative(mode: PrivilegeMode): ResolutionResult =
        withContext(Dispatchers.IO) {
            when (mode) {
                PrivilegeMode.ROOT    -> rootEngine.resetResolution()
                PrivilegeMode.SHIZUKU -> shizukuEngine.resetResolution()
                PrivilegeMode.ADB     -> adbEngine.resetResolution()
            }
        }

    suspend fun probePrivilegeMode(): PrivilegeMode? = withContext(Dispatchers.IO) {
        when {
            rootEngine.isAvailable()    -> PrivilegeMode.ROOT
            shizukuEngine.isAvailable() -> PrivilegeMode.SHIZUKU
            else                        -> null
        }
    }
}
