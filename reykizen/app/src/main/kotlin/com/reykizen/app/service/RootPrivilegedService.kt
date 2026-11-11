package com.reykizen.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.topjohnwu.superuser.ipc.RootService

/**
 * Root daemon process, runs as root via libsu RootService.
 * Keeps a persistent root shell alive for the app lifetime to avoid
 * repeated root grant prompts and reduce su handshake latency.
 *
 * Individual commands are dispatched through RootEngine via Shell.cmd();
 * this service just ensures the shell pool is warm.
 */
class RootPrivilegedService : RootService() {

    inner class RootBinder : android.os.Binder() {
        fun getService(): RootPrivilegedService = this@RootPrivilegedService
    }

    private val binder = RootBinder()

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        // Shell pool initialized on first Shell.cmd() call in RootEngine.
        // Nothing to pre-warm here beyond service creation.
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
