package com.reykizen.app.resolution

import com.topjohnwu.superuser.Shell

class RootEngine {

    init {
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )
    }

    fun isAvailable(): Boolean =
        runCatching { Shell.getShell().isRoot }.getOrDefault(false)

    /**
     * Sets forced display size and density.
     * Uses `wm size WxH` and `wm density DPI` — available without IWindowManager AIDL.
     * Both commands are atomic enough for display projection purposes.
     */
    fun setResolution(width: Int, height: Int, density: Int): ResolutionResult {
        val result = Shell.cmd(
            "wm size ${width}x${height}",
            "wm density $density"
        ).exec()
        return if (result.isSuccess) ResolutionResult.Success
        else ResolutionResult.Failure(result.out.joinToString("\n").ifBlank { "wm command failed" })
    }

    /**
     * Resets display to system defaults.
     * `wm size reset` and `wm density reset` clear any forced override.
     */
    fun resetResolution(): ResolutionResult {
        val result = Shell.cmd(
            "wm size reset",
            "wm density reset"
        ).exec()
        return if (result.isSuccess) ResolutionResult.Success
        else ResolutionResult.Failure(result.out.joinToString("\n").ifBlank { "wm reset failed" })
    }

    /** Samsung-specific: additionally reset Samsung Game Booster display lock if present. */
    fun resetSamsungGameBoosterLock(): ResolutionResult {
        // Samsung Game Booster may hold a resolution lock separately.
        // Sending a broadcast to release it, then reset wm.
        val result = Shell.cmd(
            "am broadcast -a com.samsung.android.game.gamebooster.action.RESOLUTION_RESET " +
                    "--ez force_reset true 2>/dev/null || true",
            "wm size reset",
            "wm density reset"
        ).exec()
        return if (result.isSuccess) ResolutionResult.Success
        else ResolutionResult.Failure("Samsung booster reset partial: ${result.out.joinToString()}")
    }
}
