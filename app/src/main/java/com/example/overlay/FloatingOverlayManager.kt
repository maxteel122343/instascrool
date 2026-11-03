package com.example.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import com.example.data.ScrollSearchStateBus
import com.example.model.OverlayUiVersion
import com.example.ui.overlay.OverlayRootComposable
import com.example.util.GoogleSearchHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingOverlayManager(private val context: Context) {

    companion object {
        private const val TAG = "FloatingOverlayManager"
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var composeView: ComposeView? = null
    private var viewTreeOwner: ServiceViewTreeOwner? = null
    private var fastWebViewManager: FastWebViewManager? = null
    private var isAttached = false
    private var fetchPhotosJob: Job? = null

    private val layoutParams = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        gravity = Gravity.BOTTOM or Gravity.START
        width = WindowManager.LayoutParams.MATCH_PARENT
        height = WindowManager.LayoutParams.MATCH_PARENT
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showOverlay() {
        if (isAttached) return

        try {
            fastWebViewManager = FastWebViewManager(context)
            viewTreeOwner = ServiceViewTreeOwner()

            composeView = ComposeView(context).apply {
                viewTreeOwner?.attachToView(this)
                setContent {
                    OverlayRootComposable(
                        fastWebViewManager = fastWebViewManager!!,
                        onCloseOverlay = {
                            ScrollSearchStateBus.setOverlayActive(false)
                            hideOverlay()
                        }
                    )
                }
            }

            windowManager.addView(composeView, layoutParams)
            isAttached = true
            Log.d(TAG, "Floating overlay attached successfully")

            observeStateBus()
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying floating overlay: ${e.message}")
        }
    }

    private fun observeStateBus() {
        // Toggle keyboard focusable mode on search focus
        ScrollSearchStateBus.setOnRequestFocusToggle { focused ->
            updateFocusableMode(focused)
        }

        // Query change listener (reusing webview and fetching clean photos)
        ScrollSearchStateBus.setOnQueryChangedListener { query ->
            onSearchQueryTriggered(query)
        }

        // Initial search load
        val initialQuery = ScrollSearchStateBus.getEffectiveSearchQuery()
        onSearchQueryTriggered(initialQuery)

        // Observe bubble collapse state to adjust window bounds
        scope.launch {
            ScrollSearchStateBus.isBubbleCollapsed.collectLatest { collapsed ->
                updateWindowBounds(collapsed)
            }
        }
    }

    private fun onSearchQueryTriggered(query: String) {
        // 1. Fast WebView reuse (Requirement 1: do not recreate webview, only update url)
        fastWebViewManager?.loadSearchQuery(query)

        // 2. Asynchronously extract clean photos for V6 & V7 (no google clutter)
        fetchPhotosJob?.cancel()
        fetchPhotosJob = scope.launch {
            ScrollSearchStateBus.setLoadingPhotos(true)
            delay(50) // Small debounce for smooth UI
            val photos = GoogleSearchHelper.fetchCleanPhotos(query)
            ScrollSearchStateBus.updatePhotos(photos, loading = false)
        }
    }

    private fun updateFocusableMode(isFocused: Boolean) {
        if (!isAttached || composeView == null) return
        try {
            if (isFocused) {
                // Focusable so the on-screen keyboard works!
                layoutParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            } else {
                // Non-focusable so background taps pass through
                layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            }
            windowManager.updateViewLayout(composeView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update focusable mode: ${e.message}")
        }
    }

    private fun updateWindowBounds(isCollapsed: Boolean) {
        if (!isAttached || composeView == null) return
        try {
            if (isCollapsed) {
                layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                layoutParams.gravity = Gravity.END or Gravity.CENTER_VERTICAL
            } else {
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
                layoutParams.gravity = Gravity.BOTTOM or Gravity.START
            }
            windowManager.updateViewLayout(composeView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update window bounds: ${e.message}")
        }
    }

    fun cleanResetWebView() {
        fastWebViewManager?.cleanRecreate()
    }

    fun hideOverlay() {
        if (!isAttached) return
        try {
            ScrollSearchStateBus.setOnRequestFocusToggle(null)
            ScrollSearchStateBus.setOnQueryChangedListener(null)
            fetchPhotosJob?.cancel()

            composeView?.let { view ->
                windowManager.removeView(view)
            }
            viewTreeOwner?.destroy()
            fastWebViewManager?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding floating overlay: ${e.message}")
        } finally {
            composeView = null
            viewTreeOwner = null
            fastWebViewManager = null
            isAttached = false
            scope.cancel()
        }
    }
}
