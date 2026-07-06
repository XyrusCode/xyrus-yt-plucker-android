package xyrus.code.ytplucker.ui

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyrus.code.ytplucker.R
import xyrus.code.ytplucker.domain.model.DownloadedFile
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text(
            "Downloads",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        if (items.isEmpty()) {
            Text(
                "Nothing here yet. Your downloaded files will appear here — tap one to open it in " +
                    "your player.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.uri.toString() }) { file ->
                HistoryRow(file) {
                    runCatching { context.startActivity(xyrus.code.ytplucker.data.HistoryStore.openIntent(file)) }
                        .onFailure {
                            val msg = if (it is ActivityNotFoundException) {
                                "No app can open this file type"
                            } else {
                                "Couldn't open file"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(file: DownloadedFile, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(iconFor(file)),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                Text(
                    formatSize(file.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun iconFor(file: DownloadedFile): Int = when {
    file.isAudio -> R.drawable.ic_audio
    file.isImage -> R.drawable.ic_image
    else -> R.drawable.ic_video
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) String.format(Locale.US, "%.2f GB", mb / 1024)
    else String.format(Locale.US, "%.1f MB", mb)
}
