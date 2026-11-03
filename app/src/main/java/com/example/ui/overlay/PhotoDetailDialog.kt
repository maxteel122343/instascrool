package com.example.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.IgPink
import com.example.util.ImageDownloader
import kotlinx.coroutines.launch

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ScrollSearchStateBus

@Composable
fun PhotoDetailDialog(
    images: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val capturedBitmap by ScrollSearchStateBus.currentCapturedBitmap.collectAsStateWithLifecycle()

    val safeImages = if (images.isNotEmpty()) images else listOf("")
    val safeIndex = initialIndex.coerceIn(0, (safeImages.size - 1).coerceAtLeast(0))

    val pagerState = rememberPagerState(
        initialPage = safeIndex,
        pageCount = { safeImages.size }
    )

    // Current page zoom and pan transformation states
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // User preference: retain zoom level when dragging to the next image
    var keepZoomOnSwipe by remember { mutableStateOf(true) }

    // Prevent duplicate swipe triggers during ongoing animated page transitions
    var isPagingTransitionActive by remember { mutableStateOf(false) }

    // When opened or safeIndex changes, ensure pager is scrolled to the clicked image index!
    LaunchedEffect(safeIndex, safeImages.size) {
        if (safeIndex in safeImages.indices && pagerState.currentPage != safeIndex) {
            pagerState.scrollToPage(safeIndex)
        }
    }

    // When page changes, reset offset to center. If keepZoomOnSwipe is true, maintain current zoom scale!
    LaunchedEffect(pagerState.currentPage) {
        if (!keepZoomOnSwipe) {
            scale = 1f
        }
        offset = Offset.Zero
        isPagingTransitionActive = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.96f))
            .testTag("photo_detail_dialog")
    ) {
        // HorizontalPager allows swiping/dragging to previous and next images
        // Swiping is enabled natively when at standard zoom; while zoomed in, custom drag gestures handle edge flipping
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = scale <= 1.05f,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val imageUrl = safeImages.getOrNull(pageIndex).orEmpty()
            val isCurrentCaptured = (pageIndex == safeIndex && capturedBitmap != null && capturedBitmap!!.width > 20)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(pageIndex) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5.5f)
                            if (scale > 1f) {
                                val maxOffsetX = (size.width * (scale - 1f)) / 2f
                                val maxOffsetY = (size.height * (scale - 1f)) / 2f
                                val prospectiveX = offset.x + pan.x * scale
                                val prospectiveY = offset.y + pan.y * scale

                                // Horizontal boundary edge dragging:
                                // If the user has panned to the edge and continues dragging to the side,
                                // transition to the next / previous photo while retaining zoom (if keepZoomOnSwipe is enabled).
                                if (prospectiveX < -maxOffsetX - 50f && pan.x < -2f) {
                                    if (pagerState.currentPage < safeImages.size - 1 && !isPagingTransitionActive) {
                                        isPagingTransitionActive = true
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                                } else if (prospectiveX > maxOffsetX + 50f && pan.x > 2f) {
                                    if (pagerState.currentPage > 0 && !isPagingTransitionActive) {
                                        isPagingTransitionActive = true
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                }

                                val newX = prospectiveX.coerceIn(-maxOffsetX, maxOffsetX)
                                val newY = prospectiveY.coerceIn(-maxOffsetY, maxOffsetY)
                                offset = Offset(newX, newY)
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                    .pointerInput(pageIndex) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.2f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Determine image model: prioritize valid image URL / Base64; fallback to capturedBitmap
                val modelToUse: Any = if (imageUrl.isNotBlank()) {
                    imageUrl
                } else if (isCurrentCaptured) {
                    capturedBitmap!!
                } else {
                    ""
                }

                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(modelToUse)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Visualização da foto em alta qualidade",
                    contentScale = ContentScale.Fit,
                    loading = {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = IgPink, modifier = Modifier.size(36.dp))
                        }
                    },
                    error = {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Imagem protegida contra download externo",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Use o botão de captura direta no grid de fotos",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                )
            }
        }

        // Left Navigation Arrow (Previous Photo)
        if (pagerState.currentPage > 0) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    .testTag("prev_photo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Foto anterior",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Right Navigation Arrow (Next Photo)
        if (pagerState.currentPage < safeImages.size - 1) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                    .testTag("next_photo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Próxima foto",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(40.dp)
                    .background(DarkBackground.copy(alpha = 0.85f), CircleShape)
                    .testTag("close_photo_dialog_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fechar visualização",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${pagerState.currentPage + 1} de ${safeImages.size}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (scale > 1.05f) {
                        "Zoom: ${(scale * 100).toInt()}% • Arraste p/ o lado p/ próxima no zoom"
                    } else {
                        "Arraste para o lado ou faça pinça para zoom"
                    },
                    color = if (scale > 1.05f) Color(0xFF00E5FF) else Color.LightGray,
                    fontSize = 11.sp
                )
            }

            // Download Current Photo Button
            val currentPhotoUrl = safeImages.getOrNull(pagerState.currentPage).orEmpty()
            Button(
                onClick = {
                    if (pagerState.currentPage == safeIndex && capturedBitmap != null) {
                        ImageDownloader.saveBitmapToGallery(context, capturedBitmap!!, "ScrollSearch_ZoomedPhoto")
                    } else if (currentPhotoUrl.startsWith("data:image")) {
                        ImageDownloader.downloadBase64(context, currentPhotoUrl, "ScrollSearch_ZoomedPhoto")
                    } else if (currentPhotoUrl.isNotBlank()) {
                        ImageDownloader.downloadPhoto(context, currentPhotoUrl, "https://www.google.com/", "ScrollSearch_ZoomedPhoto")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IgPink),
                shape = CircleShape,
                modifier = Modifier.testTag("download_photo_fullscreen_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Baixar Imagem",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Baixar", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Bottom Zoom Controls Bar (Zoom In, Zoom Out, Sair do Zoom, Manter Zoom)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.85f),
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Zoom out
                IconButton(
                    onClick = {
                        scale = (scale - 0.5f).coerceAtLeast(1f)
                        if (scale <= 1.05f) {
                            scale = 1f
                            offset = Offset.Zero
                        }
                    },
                    modifier = Modifier.size(34.dp).testTag("zoom_out_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Diminuir zoom",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Zoom percentage indicator
                Text(
                    text = "${(scale * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 3.dp)
                )

                // Zoom in
                IconButton(
                    onClick = {
                        scale = (scale + 0.5f).coerceAtMost(5.5f)
                    },
                    modifier = Modifier.size(34.dp).testTag("zoom_in_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Aumentar zoom",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // "Manter Zoom ao arrastar" Toggle Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (keepZoomOnSwipe) Color(0xFF00E5FF).copy(alpha = 0.20f)
                            else Color.White.copy(alpha = 0.08f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (keepZoomOnSwipe) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            keepZoomOnSwipe = !keepZoomOnSwipe
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("toggle_keep_zoom_chip")
                ) {
                    Text(
                        text = if (keepZoomOnSwipe) "Zoom Fixo: ON" else "Zoom Fixo: OFF",
                        color = if (keepZoomOnSwipe) Color(0xFF00E5FF) else Color.LightGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // "Sair do Zoom" / Reset Zoom Button
                AnimatedVisibility(
                    visible = scale > 1.05f,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Button(
                        onClick = {
                            scale = 1f
                            offset = Offset.Zero
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("reset_zoom_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Sair do Zoom",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sair do Zoom",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
