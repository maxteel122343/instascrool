package com.example.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ScrollSearchStateBus
import com.example.model.OverlayUiVersion
import com.example.model.PhotoItem
import com.example.overlay.FastWebViewManager
import com.example.ui.overlay.views.F1PurePhotoView
import com.example.ui.overlay.views.F2PurePhotoView
import com.example.ui.overlay.views.F5PurePhotoView
import com.example.ui.overlay.views.F6PurePhotoView
import com.example.ui.overlay.views.F7PurePhotoView
import com.example.ui.overlay.views.PureFloatingControlsPill
import com.example.ui.overlay.views.V1BottomBarView
import com.example.ui.overlay.views.V2SidebarView
import com.example.ui.overlay.views.V5BottomSheetView
import com.example.ui.overlay.views.V6VerticalPhotosView
import com.example.ui.overlay.views.V7HorizontalPhotosView
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ScrollSearchTheme

@Composable
fun OverlayRootComposable(
    fastWebViewManager: FastWebViewManager,
    onCloseOverlay: () -> Unit,
    onBubbleDrag: ((deltaX: Float, deltaY: Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val username by ScrollSearchStateBus.currentUsername.collectAsState()
    val activeVersion by ScrollSearchStateBus.activeVersion.collectAsState()
    val isBubbleCollapsed by ScrollSearchStateBus.isBubbleCollapsed.collectAsState()
    val tags by ScrollSearchStateBus.tags.collectAsState()
    val selectedTag by ScrollSearchStateBus.selectedTag.collectAsState()
    val customQuery by ScrollSearchStateBus.customSearchQuery.collectAsState()
    val heightRatio by ScrollSearchStateBus.heightRatio.collectAsState()
    val photos by ScrollSearchStateBus.photos.collectAsState()
    val isLoadingPhotos by ScrollSearchStateBus.isLoadingPhotos.collectAsState()
    val isV1Expanded by ScrollSearchStateBus.isV1WebViewExpanded.collectAsState()
    val isPhotoDetailOpen by ScrollSearchStateBus.isPhotoDetailOpen.collectAsState()

    var showAddTagDialog by remember { mutableStateOf(false) }
    var selectedPhotoDetail by remember { mutableStateOf<PhotoItem?>(null) }

    ScrollSearchTheme {
        if (isBubbleCollapsed) {
            // Minimized floating pill docked to edge
            FloatingBubbleView(
                username = username,
                onExpand = {
                    ScrollSearchStateBus.setBubbleCollapsed(false)
                },
                onDrag = onBubbleDrag,
                modifier = modifier
            )
        } else if (isPhotoDetailOpen && selectedPhotoDetail != null) {
            // Fullscreen photo inspector dialog
            PhotoDetailDialog(
                photo = selectedPhotoDetail!!,
                onDismiss = {
                    selectedPhotoDetail = null
                    ScrollSearchStateBus.setPhotoDetailOpen(false)
                }
            )
        } else {
            // Main Overlay (V versions with tools or F versions with pure photos)
            when (activeVersion) {
                // ==========================================
                // F VERSIONS (PURE CLEAN HIGH QUALITY PHOTOS)
                // ==========================================
                OverlayUiVersion.F1 -> {
                    // F1: Barra Inferior - Só Fotos Grandes Horizontais
                    Box(modifier = modifier.fillMaxSize()) {
                        F1PurePhotoView(
                            photos = photos,
                            isLoading = isLoadingPhotos,
                            onPhotoClick = {
                                selectedPhotoDetail = it
                                ScrollSearchStateBus.setPhotoDetailOpen(true)
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        PureFloatingControlsPill(
                            currentVersion = activeVersion,
                            username = username,
                            onVersionSelected = { ScrollSearchStateBus.setVersion(it) },
                            onMinimize = { ScrollSearchStateBus.setBubbleCollapsed(true) },
                            onClose = onCloseOverlay,
                            onRefresh = { ScrollSearchStateBus.notifyEffectiveQueryChanged() },
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                }

                OverlayUiVersion.F2 -> {
                    // F2: Sidebar Lateral - Só Fotos Verticais
                    Box(modifier = modifier.fillMaxSize()) {
                        F2PurePhotoView(
                            photos = photos,
                            isLoading = isLoadingPhotos,
                            onPhotoClick = {
                                selectedPhotoDetail = it
                                ScrollSearchStateBus.setPhotoDetailOpen(true)
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        PureFloatingControlsPill(
                            currentVersion = activeVersion,
                            username = username,
                            onVersionSelected = { ScrollSearchStateBus.setVersion(it) },
                            onMinimize = { ScrollSearchStateBus.setBubbleCollapsed(true) },
                            onClose = onCloseOverlay,
                            onRefresh = { ScrollSearchStateBus.notifyEffectiveQueryChanged() },
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                }

                OverlayUiVersion.F5,
                OverlayUiVersion.F6,
                OverlayUiVersion.F7 -> {
                    // F5, F6, F7: Painéis Inferiores Redimensionáveis com Fotos Puras
                    Box(
                        modifier = modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top border resize handle
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(20.dp)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .pointerInput(Unit) {
                                        detectVerticalDragGestures { change, dragAmount ->
                                            change.consume()
                                            val deltaRatio = -dragAmount / 1200f
                                            ScrollSearchStateBus.setHeightRatio(heightRatio + deltaRatio)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(44.dp)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.4f))
                                )
                            }

                            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                when (activeVersion) {
                                    OverlayUiVersion.F5 -> {
                                        F5PurePhotoView(
                                            photos = photos,
                                            isLoading = isLoadingPhotos,
                                            onPhotoClick = {
                                                selectedPhotoDetail = it
                                                ScrollSearchStateBus.setPhotoDetailOpen(true)
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    OverlayUiVersion.F6 -> {
                                        F6PurePhotoView(
                                            photos = photos,
                                            isLoading = isLoadingPhotos,
                                            onPhotoClick = {
                                                selectedPhotoDetail = it
                                                ScrollSearchStateBus.setPhotoDetailOpen(true)
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    OverlayUiVersion.F7 -> {
                                        F7PurePhotoView(
                                            photos = photos,
                                            isLoading = isLoadingPhotos,
                                            onPhotoClick = {
                                                selectedPhotoDetail = it
                                                ScrollSearchStateBus.setPhotoDetailOpen(true)
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    else -> Unit
                                }
                            }
                        }

                        // Minimalist floating control pill
                        PureFloatingControlsPill(
                            currentVersion = activeVersion,
                            username = username,
                            onVersionSelected = { ScrollSearchStateBus.setVersion(it) },
                            onMinimize = { ScrollSearchStateBus.setBubbleCollapsed(true) },
                            onClose = onCloseOverlay,
                            onRefresh = { ScrollSearchStateBus.notifyEffectiveQueryChanged() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 16.dp)
                        )
                    }
                }

                // ==========================================
                // V VERSIONS (STANDARD CONTROLS & WEBVIEWS)
                // ==========================================
                OverlayUiVersion.V2 -> {
                    // V2: Sidebar lateral layout
                    Surface(
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                        color = DarkSurface,
                        shadowElevation = 12.dp,
                        modifier = modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            OverlayHeader(
                                username = username,
                                currentVersion = activeVersion,
                                onVersionSelected = { ScrollSearchStateBus.setVersion(it) },
                                onMinimize = { ScrollSearchStateBus.setBubbleCollapsed(true) },
                                onClose = onCloseOverlay,
                                onRefresh = { ScrollSearchStateBus.notifyEffectiveQueryChanged() }
                            )

                            TagsRow(
                                tags = tags,
                                selectedTag = selectedTag,
                                onTagSelected = { ScrollSearchStateBus.selectTag(it) },
                                onAddNewTagClick = { showAddTagDialog = true }
                            )

                            SearchBarComponent(
                                query = customQuery,
                                onQueryChange = { ScrollSearchStateBus.setCustomSearchQuery(it) },
                                placeholder = "@${username ?: "criador"}...",
                                onSearchSubmitted = { ScrollSearchStateBus.setCustomSearchQuery(it) },
                                onFocusChanged = { ScrollSearchStateBus.setSearchFocused(it) }
                            )

                            V2SidebarView(
                                photos = photos,
                                isLoading = isLoadingPhotos,
                                onPhotoClick = {
                                    selectedPhotoDetail = it
                                    ScrollSearchStateBus.setPhotoDetailOpen(true)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                OverlayUiVersion.V1 -> {
                    // V1: Barra inferior com cards
                    Surface(
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        color = DarkSurface,
                        shadowElevation = 14.dp,
                        modifier = modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            OverlayHeader(
                                username = username,
                                currentVersion = activeVersion,
                                onVersionSelected = { ScrollSearchStateBus.setVersion(it) },
                                onMinimize = { ScrollSearchStateBus.setBubbleCollapsed(true) },
                                onClose = onCloseOverlay,
                                onRefresh = { ScrollSearchStateBus.notifyEffectiveQueryChanged() }
                            )

                            TagsRow(
                                tags = tags,
                                selectedTag = selectedTag,
                                onTagSelected = { ScrollSearchStateBus.selectTag(it) },
                                onAddNewTagClick = { showAddTagDialog = true }
                            )

                            SearchBarComponent(
                                query = customQuery,
                                onQueryChange = { ScrollSearchStateBus.setCustomSearchQuery(it) },
                                placeholder = "Buscar com @${username ?: "criador"}...",
                                onSearchSubmitted = { ScrollSearchStateBus.setCustomSearchQuery(it) },
                                onFocusChanged = { ScrollSearchStateBus.setSearchFocused(it) }
                            )

                            V1BottomBarView(
                                photos = photos,
                                isLoading = isLoadingPhotos,
                                onPhotoClick = {
                                    selectedPhotoDetail = it
                                    ScrollSearchStateBus.setPhotoDetailOpen(true)
                                },
                                showWebView = isV1Expanded,
                                onToggleWebView = {
                                    ScrollSearchStateBus.setV1WebViewExpanded(!isV1Expanded)
                                },
                                fastWebViewManager = fastWebViewManager,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                OverlayUiVersion.V5,
                OverlayUiVersion.V6,
                OverlayUiVersion.V7 -> {
                    // V5, V6, V7: Bottom panels com redimensionamento
                    Surface(
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        color = DarkSurface,
                        shadowElevation = 16.dp,
                        modifier = modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectVerticalDragGestures { change, dragAmount ->
                                            change.consume()
                                            val deltaRatio = -dragAmount / 1200f
                                            ScrollSearchStateBus.setHeightRatio(heightRatio + deltaRatio)
                                        }
                                    }
                                    .testTag("top_border_resize_handle")
                            ) {
                                OverlayHeader(
                                    username = username,
                                    currentVersion = activeVersion,
                                    onVersionSelected = { ScrollSearchStateBus.setVersion(it) },
                                    onMinimize = { ScrollSearchStateBus.setBubbleCollapsed(true) },
                                    onClose = onCloseOverlay,
                                    onRefresh = { ScrollSearchStateBus.notifyEffectiveQueryChanged() }
                                )
                            }

                            TagsRow(
                                tags = tags,
                                selectedTag = selectedTag,
                                onTagSelected = { ScrollSearchStateBus.selectTag(it) },
                                onAddNewTagClick = { showAddTagDialog = true }
                            )

                            SearchBarComponent(
                                query = customQuery,
                                onQueryChange = { ScrollSearchStateBus.setCustomSearchQuery(it) },
                                placeholder = "Pesquisar imagens com @${username ?: "criador"}...",
                                onSearchSubmitted = { ScrollSearchStateBus.setCustomSearchQuery(it) },
                                onFocusChanged = { ScrollSearchStateBus.setSearchFocused(it) }
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                when (activeVersion) {
                                    OverlayUiVersion.V5 -> {
                                        V5BottomSheetView(
                                            fastWebViewManager = fastWebViewManager,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    OverlayUiVersion.V6 -> {
                                        V6VerticalPhotosView(
                                            photos = photos,
                                            isLoading = isLoadingPhotos,
                                            onPhotoClick = {
                                                selectedPhotoDetail = it
                                                ScrollSearchStateBus.setPhotoDetailOpen(true)
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    OverlayUiVersion.V7 -> {
                                        V7HorizontalPhotosView(
                                            photos = photos,
                                            isLoading = isLoadingPhotos,
                                            onPhotoClick = {
                                                selectedPhotoDetail = it
                                                ScrollSearchStateBus.setPhotoDetailOpen(true)
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Tag Dialog
        if (showAddTagDialog) {
            AddTagDialog(
                onDismiss = { showAddTagDialog = false },
                onAddTag = { ScrollSearchStateBus.addCustomTag(it) }
            )
        }
    }
}
