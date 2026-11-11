package com.reykizen.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Placeholder user service for Shizuku binding.
 * Shizuku binds this service with elevated privileges via its own process,
 * giving the binder elevated IWindowManager access.
 * Actual resolution commands are issued from ShizukuEngine via SystemServiceHelper.
 */
class ShizukuUserService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
    }
}
