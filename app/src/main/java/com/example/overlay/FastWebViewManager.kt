package com.example.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.util.GoogleSearchHelper

class FastWebViewManager(private val context: Context) {

    private var webView: WebView? = null
    private var currentUrl: String? = null
    private var lastLoadTimestamp: Long = 0L

    var onPageStartedListener: (() -> Unit)? = null
    var onPageFinishedListener: (() -> Unit)? = null
    var onErrorListener: ((String) -> Unit)? = null

    init {
        initWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        try {
            val wv = WebView(context.applicationContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Hardware acceleration for maximum smoothness
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                setBackgroundColor(0xFF12121E.toInt())

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    setSupportZoom(false)
                    loadsImagesAutomatically = true
                    mediaPlaybackRequiresUserGesture = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onPageStartedListener?.invoke()
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Inject clean styling to remove Google clutter and headers
                        view?.evaluateJavascript(GoogleSearchHelper.getCleanViewCssInjectionScript(), null)
                        onPageFinishedListener?.invoke()
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        onErrorListener?.invoke(error?.description?.toString() ?: "Erro ao carregar")
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        if (newProgress > 60) {
                            view?.evaluateJavascript(GoogleSearchHelper.getCleanViewCssInjectionScript(), null)
                        }
                    }
                }
            }
            webView = wv
        } catch (e: Exception) {
            Log.e("FastWebViewManager", "Failed to init WebView: ${e.message}")
        }
    }

    fun getWebView(): WebView? {
        if (webView == null) {
            initWebView()
        }
        return webView
    }

    /**
     * Reuses existing WebView and only navigates when URL changes
     * (Zero full-overlay re-creations, near real-time response).
     */
    fun loadSearchQuery(query: String) {
        val targetUrl = GoogleSearchHelper.buildGoogleImagesUrl(query)
        if (targetUrl == currentUrl) {
            // Already loaded this query
            return
        }

        currentUrl = targetUrl
        lastLoadTimestamp = SystemClock.uptimeMillis()

        webView?.let { wv ->
            wv.stopLoading()
            wv.loadUrl(targetUrl)
        } ?: run {
            initWebView()
            webView?.loadUrl(targetUrl)
        }
    }

    /**
     * Cleanly recreates WebView if memory gets full or page becomes unresponsive.
     */
    fun cleanRecreate() {
        try {
            webView?.let { wv ->
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.stopLoading()
                wv.clearHistory()
                wv.clearCache(false)
                wv.destroy()
            }
        } catch (e: Exception) {
            Log.e("FastWebViewManager", "Error destroying old webview: ${e.message}")
        } finally {
            webView = null
            initWebView()
            currentUrl?.let { loadSearchQuery(it) }
        }
    }

    fun destroy() {
        try {
            webView?.let { wv ->
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.stopLoading()
                wv.destroy()
            }
        } catch (e: Exception) {
            Log.e("FastWebViewManager", "Error on destroy: ${e.message}")
        } finally {
            webView = null
        }
    }
}
