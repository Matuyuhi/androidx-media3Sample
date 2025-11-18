package com.matuyuhi.media3.sample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import com.matuyuhi.media3.sample.data.HistoryRepository
import com.matuyuhi.media3.sample.data.MediaCatalog
import java.text.SimpleDateFormat
import java.util.*

/**
 * YouTube Music風の統合再生履歴BottomSheet
 * 履歴と今後の再生キューを1つのリストで表示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackHistorySheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    historyRepository: HistoryRepository,
    mediaCatalog: MediaCatalog,
    currentMediaItem: MediaItem?,
    upcomingQueue: List<MediaItem>,
    onMediaItemClick: (MediaItem) -> Unit
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp)
            ) {
                // ヘッダー
                Text(
                    text = "再生履歴とキュー",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                // 履歴データを取得
                val historyEntries by historyRepository.getAllFlow()
                    .collectAsState(initial = emptyList())

                val historyItems = remember(historyEntries) {
                    historyEntries.mapNotNull { entry ->
                        mediaCatalog.getMediaItem(entry.mediaId)?.let { item ->
                            HistoryItemData(
                                mediaItem = item,
                                timestamp = entry.timestamp,
                                playDurationMs = entry.playDurationMs,
                                completionReason = entry.completionReason.toString()
                            )
                        }
                    }
                }

                // 統合リスト（時系列順：過去→現在→未来）
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // セクション: 再生履歴（過去）
                    if (historyItems.isNotEmpty()) {
                        item {
                            SectionHeader(
                                icon = Icons.Default.History,
                                title = "再生履歴"
                            )
                        }
                        items(historyItems) { historyData ->
                            HistoryMediaItemCard(
                                historyData = historyData,
                                onClick = { onMediaItemClick(historyData.mediaItem) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    // セクション: 現在再生中
                    if (currentMediaItem != null) {
                        item {
                            SectionHeader(
                                icon = Icons.Default.PlayArrow,
                                title = "現在再生中"
                            )
                        }
                        item {
                            MediaItemCard(
                                mediaItem = currentMediaItem,
                                subtitle = "再生中",
                                isCurrentlyPlaying = true,
                                onClick = { onMediaItemClick(currentMediaItem) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }

                    // セクション: 次に再生（未来）
                    if (upcomingQueue.isNotEmpty()) {
                        item {
                            SectionHeader(
                                icon = Icons.Default.QueueMusic,
                                title = "次に再生 (${upcomingQueue.size})"
                            )
                        }
                        items(upcomingQueue) { mediaItem ->
                            MediaItemCard(
                                mediaItem = mediaItem,
                                subtitle = "キューに追加済み",
                                isCurrentlyPlaying = false,
                                onClick = { onMediaItemClick(mediaItem) }
                            )
                        }
                    }

                    // 履歴が空の場合
                    if (historyItems.isEmpty() && currentMediaItem == null && upcomingQueue.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "再生履歴がありません",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * セクションヘッダー
 */
@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * メディアアイテムカード（キュー用）
 */
@Composable
private fun MediaItemCard(
    mediaItem: MediaItem,
    subtitle: String,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isCurrentlyPlaying) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // アイコンまたはサムネイル
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCurrentlyPlaying) Icons.Default.PlayArrow else Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // タイトルとサブタイトル
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = mediaItem.mediaMetadata.title?.toString() ?: "不明なタイトル",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 履歴メディアアイテムカード
 */
@Composable
private fun HistoryMediaItemCard(
    historyData: HistoryItemData,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // アイコン
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 情報
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = historyData.mediaItem.mediaMetadata.title?.toString() ?: "不明なタイトル",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(historyData.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatDuration(historyData.playDurationMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 履歴データクラス
 */
data class HistoryItemData(
    val mediaItem: MediaItem,
    val timestamp: Long,
    val playDurationMs: Long,
    val completionReason: String
)

/**
 * タイムスタンプのフォーマット
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "たった今"
        diff < 3600_000 -> "${diff / 60_000}分前"
        diff < 86400_000 -> "${diff / 3600_000}時間前"
        diff < 604800_000 -> "${diff / 86400_000}日前"
        else -> {
            val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}

/**
 * 再生時間のフォーマット
 */
private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return if (minutes > 0) {
        "${minutes}分${remainingSeconds}秒"
    } else {
        "${remainingSeconds}秒"
    }
}
