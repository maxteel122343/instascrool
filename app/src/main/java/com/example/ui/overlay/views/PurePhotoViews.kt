package com.example.ui.overlay.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.OverlayUiVersion
import com.example.model.PhotoItem
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.IgOrange
import com.example.ui.theme.IgPink
import com.example.ui.theme.IgPurple

/**
 * Minimalist floating pill with version switch, refresh, minimize, and close.
 * Allows quick switching between V and F versions without polluting the clean photo view.
 */
@Composable
fun PureFloatingControlsPill(
    currentVersion: OverlayUiVersion,
    username: String?,
    onVersionSelected: (OverlayUiVersion) -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.75f),
        shadowElevation = 8.dp,
        modifier = modifier
            .padding(8.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Version pill
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showMenu = true }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = currentVersion.code,
                        color = IgPink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (!username.isNullOrBlank()) "@$username" else "",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(DarkSurfaceElevated)
                ) {
                    OverlayUiVersion.entries.forEach { version ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = version.code,
                                        color = if (version.isCleanPhotoOnly) IgOrange else IgPink,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = version.title,
                                        color = if (version == currentVersion) Color.White else Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                onVersionSelected(version)
                            }
                        )
                    }
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Atualizar",
                    tint = Color.LightGray,
                    modifier = Modifier.size(14.dp)
                )
            }

            IconButton(
                onClick = onMinimize,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Minimizar",
                    tint = Color.LightGray,
                    modifier = Modifier.size(14.dp)
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Fechar",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * F1: Baseado no V1 (Barra inferior), mas só fotos grandes na horizontal.
 * Sem barra de busca, sem cabeçalhos, sem Google.
 */
@Composable
fun F1PurePhotoView(
    photos: List<PhotoItem>,
    isLoading: Boolean,
    onPhotoClick: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A12))
    ) {
        if (isLoading && photos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IgPink, modifier = Modifier.size(26.dp))
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photos) { photo ->
                    Box(
                        modifier = Modifier
                            .fillParentMaxHeight()
                            .width(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPhotoClick(photo) }
                            .testTag("f1_photo_card")
                    ) {
                        AsyncImage(
                            model = photo.url,
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

/**
 * F2: Baseado no V2 (Sidebar Lateral), mas apenas fotos verticais puras de alta qualidade.
 * Sem headers, sem bordas, sem página do Google.
 */
@Composable
fun F2PurePhotoView(
    photos: List<PhotoItem>,
    isLoading: Boolean,
    onPhotoClick: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A12))
    ) {
        if (isLoading && photos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IgPink, modifier = Modifier.size(26.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(photos) { photo ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPhotoClick(photo) }
                            .testTag("f2_photo_card")
                    ) {
                        AsyncImage(
                            model = photo.url,
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

/**
 * F5: Baseado no V5 (Bottom Sheet), mas apenas fotos grandes de alta qualidade
 * com deslizamento lateral / galeria, sem página do Google.
 */
@Composable
fun F5PurePhotoView(
    photos: List<PhotoItem>,
    isLoading: Boolean,
    onPhotoClick: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A12))
    ) {
        if (isLoading && photos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IgPink, modifier = Modifier.size(32.dp))
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(photos) { photo ->
                    Box(
                        modifier = Modifier
                            .fillParentMaxHeight()
                            .fillParentMaxWidth(0.85f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPhotoClick(photo) }
                            .testTag("f5_photo_card")
                    ) {
                        AsyncImage(
                            model = photo.url,
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

/**
 * F6: Baseado no V6 (Fotos Vertical), totalmente limpa, sem header, sem barra de busca.
 * Fotos grandes na vertical em tela cheia da área disponível.
 */
@Composable
fun F6PurePhotoView(
    photos: List<PhotoItem>,
    isLoading: Boolean,
    onPhotoClick: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A12))
    ) {
        if (isLoading && photos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IgPink, modifier = Modifier.size(32.dp))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(photos) { photo ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPhotoClick(photo) }
                            .testTag("f6_photo_card")
                    ) {
                        AsyncImage(
                            model = photo.url,
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

/**
 * F7: Baseado no V7 (Swipe Horizontal), tela cheia de fotos puras grandes.
 * Deslize lateral de alta resolução sem elementos extras.
 */
@Composable
fun F7PurePhotoView(
    photos: List<PhotoItem>,
    isLoading: Boolean,
    onPhotoClick: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A12))
    ) {
        if (isLoading && photos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = IgPink, modifier = Modifier.size(32.dp))
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(photos) { index, photo ->
                    Box(
                        modifier = Modifier
                            .fillParentMaxHeight()
                            .fillParentMaxWidth(0.92f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPhotoClick(photo) }
                            .testTag("f7_photo_card_$index")
                    ) {
                        AsyncImage(
                            model = photo.url,
                            contentDescription = photo.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Discreto contador no canto superior
                        Box(
                            modifier = Modifier
                                .padding(12.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${index + 1}/${photos.size}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
