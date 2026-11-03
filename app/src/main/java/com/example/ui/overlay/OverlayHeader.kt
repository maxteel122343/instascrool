package com.example.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Language
import com.example.model.OverlayUiVersion
import com.example.model.SearchEngine
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.IgOrange
import com.example.ui.theme.IgPink
import com.example.ui.theme.IgPurple

@Composable
fun OverlayHeader(
    username: String?,
    currentVersion: OverlayUiVersion,
    currentEngine: SearchEngine,
    isSafeSearchDisabled: Boolean = true,
    onVersionSelected: (OverlayUiVersion) -> Unit,
    onEngineSelected: (SearchEngine) -> Unit,
    onToggleSafeSearch: () -> Unit = {},
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showVersionMenu by remember { mutableStateOf(false) }
    var showEngineMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
    ) {
        // Drag Indicator bar at the very top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Instagram Creator Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                // Gradient Instagram indicator ring
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(IgPurple, IgPink, IgOrange)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(DarkSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "@",
                            color = IgPink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = if (!username.isNullOrBlank()) "@$username" else "Aguardando Instagram...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Reels & Feed Live Search",
                        color = Color(0xFFA0A0C0),
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }

            // Version Selector Pill & Actions
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // UI Version Selector Dropdown Pill
                Box {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurfaceElevated,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showVersionMenu = true }
                            .testTag("version_picker_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Versões",
                                tint = IgPink,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentVersion.code,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Abrir menu",
                                tint = Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showVersionMenu,
                        onDismissRequest = { showVersionMenu = false },
                        modifier = Modifier.background(DarkSurfaceElevated)
                    ) {
                        OverlayUiVersion.entries.forEach { version ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = "${version.code}: ${version.title}",
                                            color = if (version == currentVersion) IgPink else Color.White,
                                            fontWeight = if (version == currentVersion) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = version.subtitle,
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    }
                                },
                                onClick = {
                                    showVersionMenu = false
                                    onVersionSelected(version)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(5.dp))

                // Search Engine Selector Pill (Google, Bing, Duck, Yandex, Yahoo)
                Box {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurfaceElevated,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showEngineMenu = true }
                            .testTag("engine_picker_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Buscador",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = currentEngine.shortName,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Selecionar buscador",
                                tint = Color.LightGray,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showEngineMenu,
                        onDismissRequest = { showEngineMenu = false },
                        modifier = Modifier.background(DarkSurfaceElevated)
                    ) {
                        SearchEngine.entries.forEach { engine ->
                            val isEngineSelected = engine == currentEngine
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = engine.displayName,
                                            color = if (isEngineSelected) Color(0xFF00E5FF) else Color.White,
                                            fontWeight = if (isEngineSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = engine.domain,
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    }
                                },
                                onClick = {
                                    showEngineMenu = false
                                    onEngineSelected(engine)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // SafeSearch Toggle Pill (Navegação Segura: Desativada / Ativada)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSafeSearchDisabled) Color(0xFF1B5E20).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSafeSearchDisabled) Color(0xFF00E676) else Color.Gray.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onToggleSafeSearch() }
                        .testTag("safesearch_toggle_button")
                ) {
                    Text(
                        text = if (isSafeSearchDisabled) "Safe: OFF" else "Safe: ON",
                        color = if (isSafeSearchDisabled) Color(0xFF00E676) else Color.LightGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                // Refresh Button
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("overlay_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Atualizar busca",
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Minimize Button (dock to edge)
                IconButton(
                    onClick = onMinimize,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("overlay_minimize_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Minimizar overlay",
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Close Button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("overlay_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar overlay",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
