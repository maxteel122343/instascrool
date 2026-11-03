package com.example.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ScrollSearchStateBus
import com.example.model.OverlayUiVersion
import com.example.model.PhotoItem
import com.example.overlay.FastWebViewManager
import com.example.ui.overlay.views.V1BottomBarView
import com.example.ui.overlay.views.V2SidebarView
import com.example.ui.overlay.views.V5BottomSheetView
import com.example.ui.overlay.views.V6VerticalPhotosView
import com.example.ui.overlay.views.V7HorizontalPhotosView
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ScrollSearchTheme

@Composable
fun OverlayRootComposable(
    fastWebViewManager: FastWebViewManager,
    onCloseOverlay: () -> Unit,
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

    var showAddTagDialog by remember { mutableStateOf(false) }
    var selectedPhotoDetail by remember { mutableStateOf<PhotoItem?>(null) }
    var showV1WebView by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenHeightPx = configuration.screenHeightDp.dp

    ScrollSearchTheme {
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("overlay_root_container")
        ) {
            if (isBubbleCollapsed) {
                // Minimized floating pill docked to edge
                FloatingBubbleView(
                    username = username,
                    onExpand = {
                        ScrollSearchStateBus.setBubbleCollapsed(false)
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                )
            } else {
                // Expanded floating search window
                when (activeVersion) {
                    OverlayUiVersion.V2 -> {
                        // Sidebar lateral layout
                        Surface(
                            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                            color = DarkSurface,
                            shadowElevation = 12.dp,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight(0.85f)
                                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                        ) {
                            Column {
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
                                    onPhotoClick = { selectedPhotoDetail = it }
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
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        ) {
                            Column {
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
                                    onPhotoClick = { selectedPhotoDetail = it },
                                    showWebView = showV1WebView,
                                    onToggleWebView = { showV1WebView = !showV1WebView },
                                    fastWebViewManager = fastWebViewManager
                                )
                            }
                        }
                    }

                    OverlayUiVersion.V5,
                    OverlayUiVersion.V6,
                    OverlayUiVersion.V7 -> {
                        // Resizable from top border for V5, V6, and V7!
                        Surface(
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                            color = DarkSurface,
                            shadowElevation = 16.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .fillMaxHeight(heightRatio)
                                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Top Border Drag Handle for resizing V5, V6, and V7
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(Unit) {
                                            detectVerticalDragGestures { change, dragAmount ->
                                                change.consume()
                                                // Dragging upwards increases height ratio
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
                                                onPhotoClick = { selectedPhotoDetail = it },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        OverlayUiVersion.V7 -> {
                                            V7HorizontalPhotosView(
                                                photos = photos,
                                                isLoading = isLoadingPhotos,
                                                onPhotoClick = { selectedPhotoDetail = it },
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

            // Dialogs
            if (showAddTagDialog) {
                AddTagDialog(
                    onDismiss = { showAddTagDialog = false },
                    onAddTag = { ScrollSearchStateBus.addCustomTag(it) }
                )
            }

            selectedPhotoDetail?.let { photo ->
                PhotoDetailDialog(
                    photo = photo,
                    onDismiss = { selectedPhotoDetail = null }
                )
            }
        }
    }
}
