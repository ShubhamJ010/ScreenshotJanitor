package dev.sj010.ssjanitor.service

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.sj010.ssjanitor.SsJanitorApp
import dev.sj010.ssjanitor.core.constants.AppConstants
import dev.sj010.ssjanitor.data.repository.ScreenshotRepository
import dev.sj010.ssjanitor.data.repository.SettingsRepository
import dev.sj010.ssjanitor.notifications.ScreenshotNotificationManager
import dev.sj010.ssjanitor.ui.overlay.ScreenshotActionOverlay
import dev.sj010.ssjanitor.ui.theme.SsJanitorTheme
import dev.sj010.ssjanitor.worker.ScreenshotDeletionScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel

class ScreenshotActionOverlayService : android.app.Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val applicationScope: CoroutineScope
        get() = (application as SsJanitorApp).applicationScope

    private val repository: ScreenshotRepository by lazy {
        val app = application as SsJanitorApp
        ScreenshotRepository(app.database.screenshotDao())
    }
    private val settingsRepository: SettingsRepository by lazy {
        (application as SsJanitorApp).settingsRepository
    }
    private val notificationManager: ScreenshotNotificationManager by lazy {
        ScreenshotNotificationManager(this)
    }
    private val windowManager: WindowManager by lazy {
        getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private var pendingUri: String? = null
    private var pendingCreatedAt: Long = 0L
    private var showJob: Job? = null
    private var overlayView: ComposeView? = null
    private var overlayOwner: OverlayViewOwner? = null
    private var mediaObserver: ContentObserver? = null
    private var isShowing = false

    override fun onCreate() {
        super.onCreate()
        registerMediaObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uriString = intent?.getStringExtra(AppConstants.EXTRA_SCREENSHOT_URI)
            ?: run {
                stopSelf()
                return START_NOT_STICKY
            }

        if (!Settings.canDrawOverlays(this)) {
            fallbackToNotification(uriString)
            stopSelf()
            return START_NOT_STICKY
        }

        pendingUri = uriString
        pendingCreatedAt = intent.getLongExtra(
            AppConstants.EXTRA_SCREENSHOT_CREATED_AT,
            System.currentTimeMillis()
        )

        cancelPendingShow()
        removeOverlay()

        val delaySeconds = settingsRepository.getDialogDelaySeconds()
        showJob = serviceScope.launch {
            delay(delaySeconds * 1000L)
            val fallbackUri = pendingUri
            val createdAt = pendingCreatedAt
            val latestUri = withContext(Dispatchers.IO) {
                resolveLatestScreenshotUri(createdAt) ?: fallbackUri
            }
            if (latestUri != null) {
                pendingUri = latestUri
            }
            showOverlay()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        cancelPendingShow()
        removeOverlay()
        mediaObserver?.let {
            runCatching { contentResolver.unregisterContentObserver(it) }
        }
        mediaObserver = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun showOverlay() {
        val uriString = pendingUri ?: return
        if (!Settings.canDrawOverlays(this)) {
            fallbackToNotification(uriString)
            stopSelf()
            return
        }

        val owner = OverlayViewOwner().also { it.attach() }
        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                SsJanitorTheme {
                    ScreenshotActionOverlay(
                        onKeep = { executeKeep(uriString) },
                        onArchive = { executeArchive(uriString) },
                        onPreset = { minutes -> executeTimer(uriString, minutes) },
                        onCustom = { minutes -> executeTimer(uriString, minutes) },
                        onDismiss = { finishWithoutAction() }
                    )
                }
            }
            isFocusableInTouchMode = true
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    finishWithoutAction()
                    true
                } else {
                    false
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            dimAmount = 0.45f
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            title = "ssJanitor"
        }

        try {
            windowManager.addView(view, params)
            view.requestFocus()
            overlayView = view
            overlayOwner = owner
            isShowing = true
        } catch (e: Exception) {
            Log.e(TAG, "Unable to show screenshot action dialog", e)
            owner.destroy()
            fallbackToNotification(uriString)
            stopSelf()
        }
    }

    private fun executeKeep(uriString: String) {
        val scope = applicationScope
        val repo = repository
        dismissAndStop()
        scope.launch {
            runCatching { repo.keepScreenshot(uriString) }
                .onFailure { Log.e(TAG, "Keep failed for $uriString", it) }
        }
    }

    private fun executeArchive(uriString: String) {
        val scope = applicationScope
        val repo = repository
        dismissAndStop()
        scope.launch {
            runCatching { repo.archiveScreenshot(uriString) }
                .onFailure { Log.e(TAG, "Archive failed for $uriString", it) }
        }
    }

    private fun executeTimer(uriString: String, minutes: Long) {
        val delayMillis = minutes * 60_000L
        val deleteAfter = System.currentTimeMillis() + delayMillis
        val scope = applicationScope
        val repo = repository
        val appContext = applicationContext
        dismissAndStop()
        scope.launch {
            runCatching {
                repo.archiveScreenshotWithTimer(uriString, deleteAfter)
                ScreenshotDeletionScheduler.scheduleDeletion(
                    appContext,
                    uriString,
                    delayMillis
                )
            }.onFailure { Log.e(TAG, "Scheduling deletion failed for $uriString", it) }
        }
    }

    private fun finishWithoutAction() {
        dismissAndStop()
    }

    private fun dismissAndStop() {
        cancelPendingShow()
        removeOverlay()
        stopSelf()
    }

    private fun cancelPendingShow() {
        showJob?.cancel()
        showJob = null
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
        overlayOwner?.destroy()
        overlayOwner = null
        isShowing = false
    }

    private fun fallbackToNotification(uriString: String) {
        try {
            notificationManager.showScreenshotNotification(uriString, isAutoArchived = false)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to fall back to notification", e)
        }
    }

    private fun registerMediaObserver() {
        if (mediaObserver != null) return
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                if (isShowing) return
                val observedUri = pendingUri ?: return
                val observedCreatedAt = pendingCreatedAt
                serviceScope.launch {
                    val latest = withContext(Dispatchers.IO) {
                        resolveLatestScreenshotUri(observedCreatedAt)
                    }
                    if (latest != null && !isShowing &&
                        pendingUri == observedUri && pendingCreatedAt == observedCreatedAt
                    ) {
                        pendingUri = latest
                    }
                }
            }
        }
        runCatching {
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
            mediaObserver = observer
        }.onFailure { Log.e(TAG, "Unable to register MediaStore observer", it) }
    }

    private fun resolveLatestScreenshotUri(createdAt: Long): String? {
        val cutoff = (createdAt / 1000L) - 1L
        val projection = arrayOf(
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.ImageColumns.DISPLAY_NAME,
            MediaStore.Images.ImageColumns.RELATIVE_PATH,
            MediaStore.Images.ImageColumns.IS_PENDING,
            MediaStore.Images.ImageColumns.DATE_ADDED,
            MediaStore.Images.ImageColumns.DATE_MODIFIED
        )

        return try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Images.ImageColumns.DATE_ADDED} >= ?",
                arrayOf(cutoff.toString()),
                "${MediaStore.Images.ImageColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val displayName = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DISPLAY_NAME)
                    ) ?: ""
                    val relativePath = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.RELATIVE_PATH)
                    ) ?: ""
                    val isPendingIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.IS_PENDING)
                    val isPending = isPendingIndex != -1 && cursor.getInt(isPendingIndex) != 0
                    val isScreenshot = displayName.contains("screenshot", ignoreCase = true) ||
                            relativePath.contains("screenshots", ignoreCase = true)

                    if (isPending || !isScreenshot) continue

                    val id = cursor.getLong(
                        cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns._ID)
                    )
                    return@use ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()
                }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resolve latest screenshot", e)
            null
        }
    }

    private class OverlayViewOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val store = ViewModelStore()
        private val savedStateController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val viewModelStore: ViewModelStore get() = store
        override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

        fun attach() {
            savedStateController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun destroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            store.clear()
        }
    }

    companion object {
        private const val TAG = "ScreenshotActionOverlay"

        fun start(context: Context, uriString: String, createdAt: Long) {
            val intent = Intent(context, ScreenshotActionOverlayService::class.java).apply {
                putExtra(AppConstants.EXTRA_SCREENSHOT_URI, uriString)
                putExtra(AppConstants.EXTRA_SCREENSHOT_CREATED_AT, createdAt)
            }
            context.applicationContext.startService(intent)
        }
    }
}
