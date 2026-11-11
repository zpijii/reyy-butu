package com.reykizen.app.resolution

import android.content.Context
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

class ShizukuEngine(private val context: Context) {

    companion object {
        // IWindowManager transaction codes (stable across Android 8–15)
        private const val TRANSACTION_setForcedDisplaySize    = 11
        private const val TRANSACTION_clearForcedDisplaySize  = 12
        private const val TRANSACTION_setForcedDisplayDensity = 14
        private const val TRANSACTION_clearForcedDisplayDensityForUser = 15

        private const val DISPLAY_ID_DEFAULT = 0
    }

    fun isAvailable(): Boolean = runCatching {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun requestPermissionIfNeeded(requestCode: Int) {
        if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(requestCode)
        }
    }

    private fun getWindowManagerBinder(): IBinder? = runCatching {
        SystemServiceHelper.getSystemService("window")
    }.getOrNull()

    /**
     * Calls IWindowManager.setForcedDisplaySize(displayId, width, height) via Binder.
     * Then calls setForcedDisplayDensityForUser(displayId, density, userId=0).
     */
    fun setResolution(width: Int, height: Int, density: Int): ResolutionResult {
        return runCatching {
            val binder = getWindowManagerBinder()
                ?: return ResolutionResult.Failure("WindowManager binder unavailable")
            val wrapped = ShizukuBinderWrapper(binder)

            // setForcedDisplaySize(displayId, width, height)
            val dataSz = Parcel.obtain()
            val replySz = Parcel.obtain()
            try {
                dataSz.writeInterfaceToken("android.view.IWindowManager")
                dataSz.writeInt(DISPLAY_ID_DEFAULT)
                dataSz.writeInt(width)
                dataSz.writeInt(height)
                wrapped.transact(TRANSACTION_setForcedDisplaySize, dataSz, replySz, 0)
                replySz.readException()
            } finally {
                dataSz.recycle()
                replySz.recycle()
            }

            // setForcedDisplayDensityForUser(displayId, density, userId)
            val dataDpi = Parcel.obtain()
            val replyDpi = Parcel.obtain()
            try {
                dataDpi.writeInterfaceToken("android.view.IWindowManager")
                dataDpi.writeInt(DISPLAY_ID_DEFAULT)
                dataDpi.writeInt(density)
                dataDpi.writeInt(0) // userId = 0
                wrapped.transact(TRANSACTION_setForcedDisplayDensity, dataDpi, replyDpi, 0)
                replyDpi.readException()
            } finally {
                dataDpi.recycle()
                replyDpi.recycle()
            }

            ResolutionResult.Success
        }.getOrElse { e ->
            ResolutionResult.Failure("Shizuku setResolution failed: ${e.message}")
        }
    }

    fun resetResolution(): ResolutionResult {
        return runCatching {
            val binder = getWindowManagerBinder()
                ?: return ResolutionResult.Failure("WindowManager binder unavailable")
            val wrapped = ShizukuBinderWrapper(binder)

            // clearForcedDisplaySize(displayId)
            val dataSz = Parcel.obtain()
            val replySz = Parcel.obtain()
            try {
                dataSz.writeInterfaceToken("android.view.IWindowManager")
                dataSz.writeInt(DISPLAY_ID_DEFAULT)
                wrapped.transact(TRANSACTION_clearForcedDisplaySize, dataSz, replySz, 0)
                replySz.readException()
            } finally {
                dataSz.recycle()
                replySz.recycle()
            }

            // clearForcedDisplayDensityForUser(displayId, userId)
            val dataDpi = Parcel.obtain()
            val replyDpi = Parcel.obtain()
            try {
                dataDpi.writeInterfaceToken("android.view.IWindowManager")
                dataDpi.writeInt(DISPLAY_ID_DEFAULT)
                dataDpi.writeInt(0) // userId = 0
                wrapped.transact(TRANSACTION_clearForcedDisplayDensityForUser, dataDpi, replyDpi, 0)
                replyDpi.readException()
            } finally {
                dataDpi.recycle()
                replyDpi.recycle()
            }

            ResolutionResult.Success
        }.getOrElse { e ->
            ResolutionResult.Failure("Shizuku resetResolution failed: ${e.message}")
        }
    }
}
