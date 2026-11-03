package com.example.ui.overlay

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.example.overlay.FastWebViewManager
import com.example.ui.theme.IgPink

@Composable
fun AndroidWebViewContainer(
    fastWebViewManager: FastWebViewManager,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(false) }

    DisposableEffect(fastWebViewManager) {
        fastWebViewManager.onPageStartedListener = {
            isLoading = true
        }
        fastWebViewManager.onPageFinishedListener = {
            isLoading = false
        }
        onDispose {
            fastWebViewManager.onPageStartedListener = null
            fastWebViewManager.onPageFinishedListener = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF12121E))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val container = FrameLayout(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val wv = fastWebViewManager.getWebView()
                wv?.let {
                    // Detach from previous parent if needed
                    (it.parent as? ViewGroup)?.removeView(it)
                    container.addView(it)
                }
                container
            },
            update = { container ->
                val wv = fastWebViewManager.getWebView()
                if (wv != null && wv.parent != container) {
                    (wv.parent as? ViewGroup)?.removeView(wv)
                    container.removeAllViews()
                    container.addView(wv)
                }
            }
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = IgPink,
                    strokeWidth = androidx.compose.ui.unit.Dp(2.5f)
                )
            }
        }
    }
}
