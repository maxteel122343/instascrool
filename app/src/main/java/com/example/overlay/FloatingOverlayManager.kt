package com.example.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
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
import org.json.JSONArray
import org.json.JSONTokener

class FloatingOverlayManager(private val context: Context) {

    companion object {
        private const val TAG = "FloatingOverlayManager"
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var composeView: ComposeView? = null
    private var viewTreeOwner: ServiceViewTreeOwner? = null
    private var fastWebViewManager: FastWebViewManager? = null

    // Dedicated always-active background extraction WebView
    // Remains active and attached across all versions (V1, V2, V5, V6, V7, F1..F7)
    private var extractorContainer: FrameLayout? = null
    private var extractorWebView: WebView? = null
    private val extractedImageUrls = mutableListOf<String>()
    private var currentExtractionQuery: String = ""

    private var isAttached = false
    private var fetchPhotosJob: Job? = null

    private val displayMetrics get() = context.resources.displayMetrics
    private val screenWidth get() = displayMetrics.widthPixels
    private val screenHeight get() = displayMetrics.heightPixels
    private val density get() = displayMetrics.density

    // Window layout params for the visible Compose UI overlay
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

    // Window layout params for the background extractor:
    // Fully transparent (alpha = 0f), non-focusable, and non-touchable so it NEVER blocks the app below
    private val extractorLayoutParams = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        gravity = Gravity.TOP or Gravity.START
        width = screenWidth
        height = (screenHeight * 0.40f).toInt().coerceAtLeast(400)
        x = 0
        y = 0
        alpha = 0f
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showOverlay() {
        if (isAttached) return

        try {
            // 1. Initialize always-active background Extractor WebView
            setupExtractorWebView()

            // 2. Initialize interactive FastWebViewManager (for interactive V5 and V1-expanded)
            fastWebViewManager = FastWebViewManager(context)
            fastWebViewManager?.onImagesExtractedListener = { extractedUrls ->
                if (extractedUrls.isNotEmpty()) {
                    scope.launch(Dispatchers.Main) {
                        handleNewExtractedUrls(extractedUrls, currentExtractionQuery)
                    }
                }
            }

            viewTreeOwner = ServiceViewTreeOwner()
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
            Log.d(TAG, "Floating overlay and background extractor attached successfully")

            observeStateBus()
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying floating overlay: ${e.message}")
        }
    }

    /**
     * Initializes the background Extractor WebView attached to WindowManager.
     * With non-zero width/height, hardware acceleration, and View.VISIBLE within a 0-alpha window,
     * the Blink/Chromium engine lays out the DOM, executes JavaScript, and extracts real images
     * without delay, independent of which version (V or F) the user is currently viewing.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupExtractorWebView() {
        try {
            val wv = WebView(context.applicationContext).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                visibility = View.VISIBLE
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                setBackgroundColor(0xFF12121E.toInt())

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    loadsImagesAutomatically = true
                    blockNetworkImage = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                }

                addJavascriptInterface(ExtractorBridge(), "ScrollSearchBridge")

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        ScrollSearchStateBus.setLoadingPhotos(true)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript(GoogleSearchHelper.getCleanViewCssInjectionScript(), null)
                        runImageExtraction(view)

                        // Follow-up passes to capture asynchronously rendered thumbnails
                        view?.postDelayed({ runImageExtraction(view) }, 300)
                        view?.postDelayed({ runImageExtraction(view) }, 750)
                        view?.postDelayed({ runImageExtraction(view) }, 1500)
                    }

                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        super.onReceivedError(view, request, error)
                        Log.e(TAG, "Extractor WebView error: ${error?.description}")
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        if (newProgress >= 40) {
                            runImageExtraction(view)
                        }
                    }
                }
            }

            val container = FrameLayout(context.applicationContext).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                addView(wv)
            }

            extractorWebView = wv
            extractorContainer = container

            windowManager.addView(container, extractorLayoutParams)
            Log.d(TAG, "Background Extractor WebView attached to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup background extractor WebView: ${e.message}")
        }
    }

    private inner class ExtractorBridge {
        @JavascriptInterface
        fun onReceiveImages(jsonString: String) {
            processExtractedJson(jsonString, currentExtractionQuery)
        }
    }

    private fun runImageExtraction(view: WebView?) {
        val script = GoogleSearchHelper.getImageExtractorScript()
        view?.evaluateJavascript(script) { resultJson ->
            if (!resultJson.isNullOrBlank() && resultJson != "null" && resultJson != "\"[]\"") {
                processExtractedJson(resultJson, currentExtractionQuery)
            }
        }
    }

    private fun processExtractedJson(json: String, query: String) {
        try {
            val cleanJson = if (json.startsWith("\"") && json.endsWith("\"")) {
                JSONTokener(json).nextValue().toString()
            } else {
                json
            }
            val array = JSONArray(cleanJson)
            val urls = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val u = array.optString(i)
                if (u.isNotBlank() && (u.startsWith("http") || u.startsWith("data:image"))) {
                    urls.add(u)
                }
            }
            if (urls.isNotEmpty()) {
                mainHandler.post {
                    handleNewExtractedUrls(urls, query)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing extracted JSON: ${e.message}")
        }
    }

    private fun handleNewExtractedUrls(urls: List<String>, query: String) {
        val distinct = urls.distinct()
        if (distinct.isEmpty()) return

        extractedImageUrls.clear()
        extractedImageUrls.addAll(distinct)

        val photoItems = distinct.mapIndexed { index, url ->
            PhotoItem(
                id = "extracted_${query.hashCode()}_$index",
                url = url,
                title = "$query #${index + 1}",
                sourceDomain = "Google Imagens",
                width = 800,
                height = 1000
            )
        }

        ScrollSearchStateBus.updatePhotos(photoItems, loading = false)
        Log.d(TAG, "Populated ${photoItems.size} photos for query '$query'")
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
                    (210 * density).toInt()
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
        val cleanQuery = query.trim().ifEmpty { "instagram" }
        currentExtractionQuery = cleanQuery

        // 1. Immediately signal loading state to all photo views (V1, V2, V6, V7, and F1..F7)
        ScrollSearchStateBus.updatePhotos(emptyList(), loading = true)
        extractedImageUrls.clear()

        // 2. Load query into the always-active background Extractor WebView
        val targetUrl = GoogleSearchHelper.buildGoogleImagesUrl(cleanQuery)
        extractorWebView?.let { wv ->
            wv.stopLoading()
            wv.loadUrl(targetUrl)
        }

        // 3. Keep FastWebViewManager in sync for interactive V5 and V1-expanded
        fastWebViewManager?.loadSearchQuery(cleanQuery)

        // 4. Concurrently fetch high-res photography via network fallback (Wiki/HTML)
        fetchPhotosJob?.cancel()
        fetchPhotosJob = scope.launch {
            delay(50)
            val fallbackPhotos = GoogleSearchHelper.fetchCleanPhotos(cleanQuery)
            if (fallbackPhotos.isNotEmpty()) {
                val current = ScrollSearchStateBus.photos.value
                if (current.isEmpty()) {
                    ScrollSearchStateBus.updatePhotos(fallbackPhotos, loading = false)
                } else {
                    val merged = (current + fallbackPhotos).distinctBy { it.url }
                    ScrollSearchStateBus.updatePhotos(merged, loading = false)
                }
            }
        }
    }

    fun cleanResetWebView() {
        fastWebViewManager?.cleanRecreate()
        extractorWebView?.let { wv ->
            wv.clearCache(false)
            val query = ScrollSearchStateBus.getEffectiveSearchQuery()
            wv.loadUrl(GoogleSearchHelper.buildGoogleImagesUrl(query))
        }
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
            extractorContainer?.let { container ->
                windowManager.removeView(container)
            }
            extractorWebView?.let { wv ->
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.stopLoading()
                wv.destroy()
            }
            viewTreeOwner?.destroy()
            fastWebViewManager?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding floating overlay: ${e.message}")
        } finally {
            composeView = null
            extractorContainer = null
            extractorWebView = null
            viewTreeOwner = null
            fastWebViewManager = null
            isAttached = false
            scope.cancel()
        }
    }
}
