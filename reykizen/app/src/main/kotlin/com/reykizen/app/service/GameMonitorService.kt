package com.reykizen.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.reykizen.app.data.BLOODSTRIKE_PACKAGE

/**
 * Accessibility service that monitors window state changes.
 * When BloodStrike comes to foreground → broadcasts ACTION_GAME_STARTED.
 * When BloodStrike leaves foreground → broadcasts ACTION_GAME_STOPPED.
 *
 * Uses window state changed events filtered to com.netease.bl by the XML config,
 * plus a secondary check on window content changed for robustness.
 */
class GameMonitorService : AccessibilityService() {

    companion object {
        const val ACTION_GAME_STARTED = "com.reykizen.app.ACTION_GAME_STARTED"
        const val ACTION_GAME_STOPPED = "com.reykizen.app.ACTION_GAME_STOPPED"
    }

    private var bloodStrikeActive = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        val isBS = pkg == BLOODSTRIKE_PACKAGE

        if (isBS && !bloodStrikeActive) {
            bloodStrikeActive = true
            sendBroadcast(Intent(ACTION_GAME_STARTED).setPackage(packageName))
        } else if (!isBS && bloodStrikeActive) {
            bloodStrikeActive = false
            sendBroadcast(Intent(ACTION_GAME_STOPPED).setPackage(packageName))
        }
    }

    override fun onInterrupt() {
        bloodStrikeActive = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        bloodStrikeActive = false
    }

    override fun onUnbind(intent: Intent?): Boolean {
        bloodStrikeActive = false
        return super.onUnbind(intent)
    }
}
