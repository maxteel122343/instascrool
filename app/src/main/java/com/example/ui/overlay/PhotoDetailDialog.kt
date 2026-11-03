package com.example.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.PhotoItem
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.IgPink
import com.example.util.ImageDownloader

@Composable
fun PhotoDetailDialog(
    photo: PhotoItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .testTag("photo_detail_dialog")
    ) {
        // Fullscreen photo
        AsyncImage(
            model = photo.url,
            contentDescription = photo.title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        )

        // Top action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(40.dp)
                    .background(DarkBackground.copy(alpha = 0.8f), CircleShape)
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
                    text = photo.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "ScrollSearch Instagram Imagem",
                    color = IgPink,
                    fontSize = 11.sp
                )
            }

            // Download Button
            Button(
                onClick = {
                    ImageDownloader.downloadPhoto(context, photo.url, photo.title)
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
                Text("Baixar", fontSize = 12.sp, color = Color.White)
            }
        }
    }
}
