package com.hitunguang.feature.transaction.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hitunguang.feature.transaction.domain.model.Attachment
import java.io.File

@Composable
fun AttachmentGrid(
    attachments: List<Attachment>,
    onAddClick: () -> Unit,
    onDeleteClick: (Attachment) -> Unit,
    onAttachmentClick: (Attachment) -> Unit,
    isEditable: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Lampiran (${attachments.size}/5)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Gather all elements (real attachments + optional add button)
        val items = attachments.map { AttachmentGridItem.Real(it) }.toMutableList<AttachmentGridItem>()
        if (isEditable && attachments.size < 5) {
            items.add(AttachmentGridItem.AddButton)
        }

        if (items.isEmpty()) {
            Text(
                text = "Tidak ada lampiran",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            val chunked = items.chunked(3)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                chunked.forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { item ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            ) {
                                when (item) {
                                    is AttachmentGridItem.Real -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { onAttachmentClick(item.attachment) }
                                        ) {
                                            val imageModel = remember(item.attachment.filePath) {
                                                if (item.attachment.filePath.startsWith("content://") || 
                                                    item.attachment.filePath.startsWith("file://")
                                                ) {
                                                    android.net.Uri.parse(item.attachment.filePath)
                                                } else {
                                                    File(item.attachment.filePath)
                                                }
                                            }
                                            AsyncImage(
                                                model = imageModel,
                                                contentDescription = "Lampiran",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            if (isEditable) {
                                                IconButton(
                                                    onClick = { onDeleteClick(item.attachment) },
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(4.dp)
                                                        .size(24.dp)
                                                        .background(
                                                            color = Color.Black.copy(alpha = 0.6f),
                                                            shape = RoundedCornerShape(12.dp)
                                                        )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Hapus Lampiran",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    is AttachmentGridItem.AddButton -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .background(MaterialTheme.colorScheme.surface)
                                                .clickable { onAddClick() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Tambah Lampiran",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Tambah",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Pad the remaining slots in the row so the weights are correct
                        val remaining = 3 - rowItems.size
                        if (remaining > 0) {
                            repeat(remaining) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed interface AttachmentGridItem {
    data class Real(val attachment: Attachment) : AttachmentGridItem
    object AddButton : AttachmentGridItem
}
