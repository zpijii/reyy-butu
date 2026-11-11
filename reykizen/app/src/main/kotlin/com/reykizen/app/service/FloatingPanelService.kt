package com.reykizen.app.service

import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.reykizen.app.MainActivity
import com.reykizen.app.R
import com.reykizen.app.data.*
import com.reykizen.app.resolution.ResolutionManager
import com.reykizen.app.resolution.ResolutionResult
import com.reykizen.app.ui.theme.OrangePrimary
import com.reykizen.app.ui.theme.ReyKizenTheme
import kotlinx.coroutines.*

class FloatingPanelService : Service(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    // ── Lifecycle plumbing for Compose inside a Service ─────────────────────
    private val lifecycleRegistry    = LifecycleRegistry(this)
    private val vmStore              = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = vmStore
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    // ── Window manager ───────────────────────────────────────────────────────
    private lateinit var wm: WindowManager
    private var floatingView: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null

    // ── Mutable state shared with the ComposeView ────────────────────────────
    private val _collapsed     = mutableStateOf(true)
    private val _statusText    = mutableStateOf("Idle")
    private val _currentPreset = mutableStateOf<ResolutionPreset?>(null)
    private val _isApplied     = mutableStateOf(false)

    private lateinit var resManager: ResolutionManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ── Game broadcast receiver ──────────────────────────────────────────────
    private val gameReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                GameMonitorService.ACTION_GAME_STARTED -> onGameStarted()
                GameMonitorService.ACTION_GAME_STOPPED -> onGameStopped()
            }
        }
    }

    // ── Prefs ────────────────────────────────────────────────────────────────
    private var latestPrefs = ReyKizenPrefs()

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        resManager = ResolutionManager(this)

        // Collect prefs
        serviceScope.launch {
            prefsFlow().collect { p ->
                latestPrefs = p
                val preset = BLOODSTRIKE_PRESETS.find { it.id == p.selectedPresetId }
                _currentPreset.value = preset
            }
        }

        // Register game broadcast receiver
        val filter = IntentFilter().apply {
            addAction(GameMonitorService.ACTION_GAME_STARTED)
            addAction(GameMonitorService.ACTION_GAME_STOPPED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(gameReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(gameReceiver, filter)
        }

        startForeground(NOTIF_ID, buildNotification())
        attachComposeView()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> { floatingView?.visibility = View.VISIBLE }
            ACTION_HIDE -> { floatingView?.visibility = View.GONE    }
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        unregisterReceiver(gameReceiver)
        floatingView?.let { wm.removeView(it) }
        floatingView = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Floating ComposeView ─────────────────────────────────────────────────

    private fun attachComposeView() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 200
        }

        floatingView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingPanelService)
            setViewTreeViewModelStoreOwner(this@FloatingPanelService)
            setViewTreeSavedStateRegistryOwner(this@FloatingPanelService)
            setContent {
                ReyKizenTheme {
                    FloatingPanel(
                        collapsed     = _collapsed.value,
                        statusText    = _statusText.value,
                        currentPreset = _currentPreset.value,
                        isApplied     = _isApplied.value,
                        onToggle      = { _collapsed.value = !_collapsed.value },
                        onApply       = { applyCurrentPreset() },
                        onRestore     = { restoreNative() },
                        onDrag        = { dx, dy -> movePanel(dx, dy) }
                    )
                }
            }
        }

        wm.addView(floatingView, params)
    }

    private fun movePanel(dx: Float, dy: Float) {
        val p = params ?: return
        p.x += dx.toInt()
        p.y += dy.toInt()
        wm.updateViewLayout(floatingView, p)
    }

    // ── Resolution ops ───────────────────────────────────────────────────────

    private fun applyCurrentPreset() {
        val preset = _currentPreset.value ?: return
        serviceScope.launch {
            _statusText.value = "Applying…"
            val result = resManager.applyPreset(preset, latestPrefs.privilegeMode)
            when (result) {
                is ResolutionResult.Success -> {
                    _isApplied.value = true
                    _statusText.value = "${preset.width}×${preset.height} active"
                }
                is ResolutionResult.Failure -> {
                    _statusText.value = "Failed: ${result.reason.take(40)}"
                }
            }
        }
    }

    private fun restoreNative() {
        serviceScope.launch {
            _statusText.value = "Restoring…"
            val result = resManager.restoreNative(latestPrefs.privilegeMode)
            when (result) {
                is ResolutionResult.Success -> {
                    _isApplied.value = false
                    _statusText.value = "Restored"
                }
                is ResolutionResult.Failure -> {
                    _statusText.value = "Restore failed"
                }
            }
        }
    }

    // ── Auto game detection ──────────────────────────────────────────────────

    private fun onGameStarted() {
        if (!latestPrefs.autoApply) return
        val preset = _currentPreset.value ?: return
        serviceScope.launch {
            _statusText.value = "BloodStrike detected — applying…"
            val result = resManager.applyPreset(preset, latestPrefs.privilegeMode)
            if (result is ResolutionResult.Success) {
                _isApplied.value = true
                _statusText.value = "Active (auto)"
            }
        }
    }

    private fun onGameStopped() {
        if (!latestPrefs.autoRestore) return
        serviceScope.launch {
            resManager.restoreNative(latestPrefs.privilegeMode)
            _isApplied.value = false
            _statusText.value = "Idle"
        }
    }

    // ── Foreground notification ──────────────────────────────────────────────

    private fun buildNotification(): Notification {
        val channelId = "rey_kizen_fg"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW)
            ch.description = getString(R.string.notification_channel_desc)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        val intent = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, channelId)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(intent)
            .build()
    }

    companion object {
        private const val NOTIF_ID   = 1337
        const val ACTION_SHOW        = "com.reykizen.app.FLOAT_SHOW"
        const val ACTION_HIDE        = "com.reykizen.app.FLOAT_HIDE"
        const val ACTION_STOP        = "com.reykizen.app.FLOAT_STOP"
    }
}

// ── Composable: the actual floating panel UI ─────────────────────────────────

@Composable
private fun FloatingPanel(
    collapsed: Boolean,
    statusText: String,
    currentPreset: ResolutionPreset?,
    isApplied: Boolean,
    onToggle: () -> Unit,
    onApply: () -> Unit,
    onRestore: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    val dragOffset = remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        if (collapsed) {
            // ── Pill tab ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 44.dp)
                    .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                    .background(OrangePrimary)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DisplaySettings,
                    contentDescription = "Open panel",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            // ── Expanded panel ────────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.width(220.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {

                    // Header row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Rey Kizen",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = OrangePrimary
                        )
                        IconButton(
                            onClick = onToggle,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Collapse",
                                tint = Color(0xFF666666),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Spacer(Modifier.height(8.dp))

                    // Active preset
                    Text(
                        text = currentPreset?.label ?: "No preset",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF222222)
                    )
                    if (currentPreset != null) {
                        Text(
                            text = "${currentPreset.width}×${currentPreset.height} · ${currentPreset.density}dpi",
                            fontSize = 11.sp,
                            color = Color(0xFF888888)
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    // Status indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isApplied) Color(0xFF22C55E) else Color(0xFFD0D0D0))
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            statusText,
                            fontSize = 11.sp,
                            color = Color(0xFF555555)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Apply button
                    Button(
                        onClick = onApply,
                        enabled = !isApplied,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangePrimary,
                            disabledContainerColor = Color(0xFFFFCBA4)
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            if (isApplied) "Applied" else "Apply",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    // Restore button
                    OutlinedButton(
                        onClick = onRestore,
                        enabled = isApplied,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isApplied) OrangePrimary else Color(0xFFE0E0E0)),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "Restore",
                            fontSize = 13.sp,
                            color = if (isApplied) OrangePrimary else Color(0xFFCCCCCC)
                        )
                    }
                }
            }
        }
    }
}
