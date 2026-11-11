package com.reykizen.app

import android.app.Application
import com.topjohnwu.superuser.Shell

class ReyKizenApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Pre-configure libsu Shell before any Shell.cmd() call.
        // FLAG_REDIRECT_STDERR ensures stderr appears in result.out.
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(15)
        )
    }
}
