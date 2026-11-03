package com.example.ui.dashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ScrollSearchStateBus
import com.example.model.OverlayUiVersion
import com.example.service.FloatingOverlayService
import com.example.ui.overlay.AddTagDialog
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.IgOrange
import com.example.ui.theme.IgPink
import com.example.ui.theme.IgPurple
import com.example.ui.theme.IgYellow

@Composable
fun DashboardScreen(
    isOverlayPermissionGranted: Boolean,
    isAccessibilityServiceEnabled: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onLaunchInstagram: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUsername by ScrollSearchStateBus.currentUsername.collectAsState()
    val isOverlayActive by ScrollSearchStateBus.isOverlayActive.collectAsState()
    val activeVersion by ScrollSearchStateBus.activeVersion.collectAsState()
    val tags by ScrollSearchStateBus.tags.collectAsState()
    val selectedTag by ScrollSearchStateBus.selectedTag.collectAsState()
    val detectionCount by ScrollSearchStateBus.detectionCount.collectAsState()
    val isInstagramForeground by ScrollSearchStateBus.isInstagramForeground.collectAsState()

    var showAddTagDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Title & Status Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        IgPurple.copy(alpha = 0.25f),
                                        DarkSurface
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "ScrollSearch",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Instagram Reels & Feed Overlay",
                                    fontSize = 12.sp,
                                    color = IgPink,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Live Instagram Activity Chip
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isInstagramForeground) Color(0xFF00C853).copy(alpha = 0.2f) else DarkSurfaceElevated,
                                border = if (isInstagramForeground) {
                                    androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00C853))
                                } else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isInstagramForeground) Color(0xFF00C853) else Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isInstagramForeground) "Instagram Ativo" else "Standby",
                                        color = if (isInstagramForeground) Color(0xFF00E676) else Color.LightGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatBox(
                                title = "Criador Atual",
                                value = "@${currentUsername ?: "nenhum"}",
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                title = "Detecções",
                                value = "$detectionCount scrolls",
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                title = "Throttle",
                                value = "40ms (Rápido)",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Master Activation Toggle & Permissions
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Overlay Flutuante",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Exibe busca do Google Imagens sobre o Instagram",
                                    color = Color(0xFFA0A0C0),
                                    fontSize = 12.sp
                                )
                            }

                            Switch(
                                checked = isOverlayActive,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (isOverlayPermissionGranted) {
                                            FloatingOverlayService.startService(context)
                                        } else {
                                            onRequestOverlayPermission()
                                        }
                                    } else {
                                        FloatingOverlayService.stopService(context)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = IgPink,
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = DarkSurfaceElevated
                                ),
                                modifier = Modifier.testTag("overlay_toggle_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Permissions Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PermissionStatusPill(
                                label = "Sobreposição",
                                granted = isOverlayPermissionGranted,
                                onClick = onRequestOverlayPermission,
                                modifier = Modifier.weight(1f)
                            )
                            PermissionStatusPill(
                                label = "Acessibilidade",
                                granted = isAccessibilityServiceEnabled,
                                onClick = onRequestAccessibilityPermission,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Version Selector (V1, V2, V5, V6, V7)
            item {
                Text(
                    text = "Versão de Interface (Otimizadas)",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OverlayUiVersion.entries.forEach { version ->
                        val isSelected = activeVersion == version
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) IgPink.copy(alpha = 0.15f) else DarkSurface,
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, IgPink)
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    ScrollSearchStateBus.setVersion(version)
                                }
                                .testTag("select_version_${version.code}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) IgPink else DarkSurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = version.code,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${version.code}: ${version.title}",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = version.subtitle,
                                        color = Color(0xFFA0A0C0),
                                        fontSize = 11.sp
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selecionado",
                                        tint = IgPink,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Keyword Templates & Dynamic Tags
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Templates de Palavras-Chave",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Busca automática: {nickname} + tag",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = { showAddTagDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = IgPink),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("dashboard_add_tag_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Adicionar tag",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Nova Tag", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(tags) { tag ->
                                val isSelected = tag.equals(selectedTag, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) IgPink.copy(alpha = 0.25f) else DarkSurfaceElevated,
                                    border = if (isSelected) {
                                        androidx.compose.foundation.BorderStroke(1.dp, IgPink)
                                    } else null,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { ScrollSearchStateBus.selectTag(tag) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (tag == "tudo") "Tudo" else "#$tag",
                                            color = if (isSelected) Color.White else Color.LightGray,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )

                                        if (tag != "tudo") {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remover tag",
                                                tint = Color.Gray,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { ScrollSearchStateBus.removeTag(tag) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Instagram Simulator & Test Area
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Test Studio / Simulador de Reels",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Simule scrolls rápidos entre criadores famosos",
                                    color = Color(0xFFA0A0C0),
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = onLaunchInstagram,
                                colors = ButtonDefaults.buttonColors(containerColor = IgPurple),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("launch_instagram_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Abrir Instagram",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Abrir IG", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val sampleCreators = listOf(
                            "cristiano", "zendaya", "selenagomez", "mrbeast",
                            "anitta", "virginiadafonseca", "leomessi", "billieeilish"
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(sampleCreators) { creator ->
                                val isCurrent = creator.equals(currentUsername, ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCurrent) IgOrange.copy(alpha = 0.25f) else DarkSurfaceElevated,
                                    border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, IgOrange) else null,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            ScrollSearchStateBus.updateDetectedUsername(creator)
                                        }
                                        .testTag("simulate_creator_$creator")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "@$creator",
                                            color = if (isCurrent) Color.White else Color.LightGray,
                                            fontSize = 12.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Clean WebView Reset Button
                        OutlinedButton(
                            onClick = {
                                FloatingOverlayService.resetWebView(context)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clean_reset_webview_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reiniciar WebView",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Recriar WebView Limpo (Estabilidade)", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAddTagDialog) {
        AddTagDialog(
            onDismiss = { showAddTagDialog = false },
            onAddTag = { ScrollSearchStateBus.addCustomTag(it) }
        )
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceElevated,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Color.Gray,
                fontSize = 10.sp
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PermissionStatusPill(
    label: String,
    granted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (granted) Color(0xFF00C853).copy(alpha = 0.15f) else Color(0xFFFF5252).copy(alpha = 0.15f),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.Check else Icons.Default.LockOpen,
                contentDescription = null,
                tint = if (granted) Color(0xFF00E676) else Color(0xFFFF5252),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$label: ${if (granted) "OK" else "Ativar"}",
                color = if (granted) Color(0xFF00E676) else Color(0xFFFF5252),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
