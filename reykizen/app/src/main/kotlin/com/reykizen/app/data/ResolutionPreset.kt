package com.reykizen.app.data

data class ResolutionPreset(
    val id: String,
    val label: String,
    val width: Int,
    val height: Int,
    val density: Int,         // DPI override; 0 = keep current
    val tag: String = "",     // e.g. "PERFORMANCE", "BALANCE", "QUALITY"
    val isCustom: Boolean = false
)

/** Physical resolution of Samsung Galaxy S-series flagships used as the native baseline. */
object SamsungNative {
    val S24_ULTRA   = ResolutionPreset("native_s24u",    "Native (S24 Ultra)",  1440, 3088, 505, "NATIVE")
    val S24_PLUS    = ResolutionPreset("native_s24p",    "Native (S24+)",       1080, 2340, 393, "NATIVE")
    val S24         = ResolutionPreset("native_s24",     "Native (S24)",        1080, 2340, 416, "NATIVE")
    val A_SERIES    = ResolutionPreset("native_a",       "Native (A-Series)",   1080, 2400, 420, "NATIVE")
}

/**
 * BloodStrike-tuned presets for Samsung.
 * BloodStrike package: com.netease.bl
 * Lower resolution → higher FPS; Samsung renders at physical res then scales.
 */
val BLOODSTRIKE_PRESETS: List<ResolutionPreset> = listOf(
    ResolutionPreset(
        id = "bs_ultra_perf",
        label = "Ultra Performance",
        width = 480,
        height = 960,
        density = 213,
        tag = "MAX FPS"
    ),
    ResolutionPreset(
        id = "bs_performance",
        label = "Performance",
        width = 540,
        height = 1080,
        density = 240,
        tag = "HIGH FPS"
    ),
    ResolutionPreset(
        id = "bs_balance",
        label = "Balanced",
        width = 720,
        height = 1440,
        density = 320,
        tag = "BALANCE"
    ),
    ResolutionPreset(
        id = "bs_quality",
        label = "Quality",
        width = 1080,
        height = 2160,
        density = 393,
        tag = "QUALITY"
    ),
    ResolutionPreset(
        id = "bs_hd_stretch",
        label = "HD Stretch",
        width = 720,
        height = 1600,
        density = 320,
        tag = "STRETCH"
    ),
    ResolutionPreset(
        id = "bs_fhd_stretch",
        label = "FHD Stretch",
        width = 1080,
        height = 2400,
        density = 393,
        tag = "STRETCH"
    ),
)

const val BLOODSTRIKE_PACKAGE = "com.netease.bl"
const val SAMSUNG_GAME_BOOSTER_PACKAGE = "com.samsung.android.game.gamebooster"
const val SAMSUNG_GAMETOOLS_PACKAGE = "com.samsung.android.gametools"
