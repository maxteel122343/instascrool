package com.example.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import com.example.data.ScrollSearchStateBus
import com.example.model.OverlayUiVersion
import com.example.model.PhotoItem
import com.example.ui.overlay.OverlayRootComposable
import com.example.util.GoogleSearchHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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

    private val displayMetrics get() = context.resources.displayMetrics
    private val screenWidth get() = displayMetrics.widthPixels
    private val screenHeight get() = displayMetrics.heightPixels
    private val density get() = displayMetrics.density

    private val layoutParams = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        gravity = Gravity.BOTTOM or Gravity.START
        width = WindowManager.LayoutParams.MATCH_PARENT
        height = (screenHeight * 0.48f).toInt()
        x = 0
        y = 0
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showOverlay() {
        if (isAttached) return

        try {
            fastWebViewManager = FastWebViewManager(context)
            viewTreeOwner = ServiceViewTreeOwner()

            // Setup bridge to extract images directly from the working Google WebView
            fastWebViewManager?.onImagesExtractedListener = { extractedUrls ->
                if (extractedUrls.isNotEmpty()) {
                    val currentQuery = ScrollSearchStateBus.getEffectiveSearchQuery()
                    val photoItems = extractedUrls.mapIndexed { index, url ->
                        PhotoItem(
                            id = "extracted_${currentQuery.hashCode()}_$index",
                            url = url,
                            title = "$currentQuery #${index + 1}",
                            sourceDomain = "Google Imagens",
                            width = 800,
                            height = 1000
                        )
                    }
                    ScrollSearchStateBus.updatePhotos(photoItems, loading = false)
                }
            }

            recalculateLayoutParams()

            composeView = ComposeView(context).apply {
                viewTreeOwner?.attachToView(this)
                setContent {
                    OverlayRootComposable(
                        fastWebViewManager = fastWebViewManager!!,
                        onCloseOverlay = {
                            ScrollSearchStateBus.setOverlayActive(false)
                            hideOverlay()
                        },
                        onBubbleDrag = { dx, dy ->
                            onBubbleDragged(dx, dy)
                        }
                    )
                }
            }

            windowManager.addView(composeView, layoutParams)
            isAttached = true
            Log.d(TAG, "Floating overlay attached with V and F version support")

            observeStateBus()
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying floating overlay: ${e.message}")
        }
    }

    private fun observeStateBus() {
        ScrollSearchStateBus.setOnRequestFocusToggle { focused ->
            updateFocusableMode(focused)
        }

        ScrollSearchStateBus.setOnQueryChangedListener { query ->
            onSearchQueryTriggered(query)
        }

        val initialQuery = ScrollSearchStateBus.getEffectiveSearchQuery()
        onSearchQueryTriggered(initialQuery)

        // Observe layout changes for all V and F versions
        scope.launch {
            combine(
                ScrollSearchStateBus.isBubbleCollapsed,
                ScrollSearchStateBus.activeVersion,
                ScrollSearchStateBus.heightRatio,
                ScrollSearchStateBus.isPhotoDetailOpen,
                ScrollSearchStateBus.isV1WebViewExpanded
            ) { collapsed, version, ratio, isPhotoDetail, isV1Expanded ->
                recalculateLayoutParams(collapsed, version, ratio, isPhotoDetail, isV1Expanded)
            }.collectLatest {
                applyLayoutParams()
            }
        }
    }

    private fun recalculateLayoutParams(
        collapsed: Boolean = ScrollSearchStateBus.isBubbleCollapsed.value,
        version: OverlayUiVersion = ScrollSearchStateBus.activeVersion.value,
        ratio: Float = ScrollSearchStateBus.heightRatio.value,
        isPhotoDetail: Boolean = ScrollSearchStateBus.isPhotoDetailOpen.value,
        isV1Expanded: Boolean = ScrollSearchStateBus.isV1WebViewExpanded.value
    ) {
        val isFocused = ScrollSearchStateBus.isSearchFocused.value

        val baseFlags = if (isFocused) {
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        }
        layoutParams.flags = baseFlags

        if (isPhotoDetail) {
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.gravity = Gravity.CENTER
            layoutParams.x = 0
            layoutParams.y = 0
            return
        }

        if (collapsed) {
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            layoutParams.gravity = Gravity.TOP or Gravity.START
            layoutParams.x = ScrollSearchStateBus.bubbleX.value
            layoutParams.y = ScrollSearchStateBus.bubbleY.value
            return
        }

        when (version) {
            // V1 and F1: Bottom bar layouts
            OverlayUiVersion.V1,
            OverlayUiVersion.F1 -> {
                layoutParams.gravity = Gravity.BOTTOM or Gravity.START
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                val v1Height = if (version == OverlayUiVersion.F1) {
                    (210 * density).toInt() // F1: Clean large photos on bottom
                } else if (isV1Expanded) {
                    (screenHeight * 0.50f).toInt()
                } else {
                    (225 * density).toInt()
                }
                layoutParams.height = v1Height
                layoutParams.x = 0
                layoutParams.y = 0
            }

            // V2 and F2: Sidebar layouts on right edge
            OverlayUiVersion.V2,
            OverlayUiVersion.F2 -> {
                layoutParams.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                layoutParams.width = (220 * density).toInt().coerceAtMost((screenWidth * 0.60f).toInt())
                layoutParams.height = (screenHeight * 0.75f).toInt()
                layoutParams.x = 0
                layoutParams.y = 0
            }

            // V5, V6, V7 and F5, F6, F7: Resizable bottom panels
            OverlayUiVersion.V5,
            OverlayUiVersion.V6,
            OverlayUiVersion.V7,
            OverlayUiVersion.F5,
            OverlayUiVersion.F6,
            OverlayUiVersion.F7 -> {
                layoutParams.gravity = Gravity.BOTTOM or Gravity.START
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
                val panelHeight = (screenHeight * ratio).toInt().coerceIn(
                    (190 * density).toInt(),
                    (screenHeight * 0.85f).toInt()
                )
                layoutParams.height = panelHeight
                layoutParams.x = 0
                layoutParams.y = 0
            }
        }
    }

    private fun applyLayoutParams() {
        if (!isAttached || composeView == null) return
        try {
            windowManager.updateViewLayout(composeView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply layout params: ${e.message}")
        }
    }

    private fun updateFocusableMode(isFocused: Boolean) {
        if (!isAttached || composeView == null) return
        try {
            recalculateLayoutParams()
            windowManager.updateViewLayout(composeView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update focusable mode: ${e.message}")
        }
    }

    private fun onBubbleDragged(deltaX: Float, deltaY: Float) {
        if (!isAttached || composeView == null) return
        try {
            val newX = (layoutParams.x + deltaX.toInt()).coerceIn(0, screenWidth - 100)
            val newY = (layoutParams.y + deltaY.toInt()).coerceIn(60, screenHeight - 100)
            layoutParams.x = newX
            layoutParams.y = newY
            ScrollSearchStateBus.updateBubblePosition(newX, newY)
            windowManager.updateViewLayout(composeView, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating bubble position: ${e.message}")
        }
    }

    private fun onSearchQueryTriggered(query: String) {
        // 1. Load in WebView (and automatically extract rendered Google images via ScrollSearchBridge)
        fastWebViewManager?.loadSearchQuery(query)

        // 2. Concurrently fetch high-res photography
        fetchPhotosJob?.cancel()
        fetchPhotosJob = scope.launch {
            ScrollSearchStateBus.setLoadingPhotos(true)
            delay(50)
            val photos = GoogleSearchHelper.fetchCleanPhotos(query)
            if (photos.isNotEmpty()) {
                ScrollSearchStateBus.updatePhotos(photos, loading = false)
            }
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
