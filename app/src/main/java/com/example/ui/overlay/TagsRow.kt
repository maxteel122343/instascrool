package com.example.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.IgPink

@Composable
fun TagsRow(
    tags: List<String>,
    selectedTag: String,
    onTagSelected: (String) -> Unit,
    onAddNewTagClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "+" Add Tag Button
        item {
            Surface(
                shape = CircleShape,
                color = IgPink,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onAddNewTagClick)
                    .testTag("add_tag_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Adicionar Tag",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Tag",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // List of tags
        items(tags) { tag ->
            val isSelected = tag.equals(selectedTag, ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) IgPink.copy(alpha = 0.25f) else DarkSurfaceElevated,
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(1.dp, IgPink)
                } else null,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onTagSelected(tag) }
                    .testTag("tag_item_${tag}")
            ) {
                Text(
                    text = if (tag == "tudo") "Tudo" else "#$tag",
                    color = if (isSelected) Color.White else Color(0xFFC0C0D8),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
    }
}
