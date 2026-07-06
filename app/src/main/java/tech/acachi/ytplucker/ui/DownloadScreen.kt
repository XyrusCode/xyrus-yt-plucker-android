package tech.acachi.ytplucker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import tech.acachi.ytplucker.domain.model.DownloadProgress
import tech.acachi.ytplucker.domain.model.JobStatus
import tech.acachi.ytplucker.domain.model.Quality
import tech.acachi.ytplucker.service.formatSpeed
import tech.acachi.ytplucker.ui.theme.Accent
import tech.acachi.ytplucker.ui.theme.Ok
import tech.acachi.ytplucker.ui.theme.Warn

@Composable
fun DownloadScreen(viewModel: DownloadViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Respect status/navigation/cutout insets so the header isn't drawn under the
                // system bars (the app is edge-to-edge via enableEdgeToEdge()).
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header()

            OutlinedTextField(
                value = state.url,
                onValueChange = viewModel::onUrlChange,
                label = { Text("YouTube or X (Twitter) video / playlist URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = viewModel::onAnalyze,
                    enabled = !state.analyzing,
                    modifier = Modifier.weight(1f),
                ) { Text(if (state.analyzing) "Analyzing…" else "Analyze") }

                Button(
                    onClick = viewModel::onDownload,
                    modifier = Modifier.weight(1f),
                ) { Text("Download") }
            }

            QualityPicker(
                selected = state.quality,
                availableHeights = state.meta?.availableHeights ?: emptyList(),
                onSelect = viewModel::onQualityChange,
            )

            state.error?.let {
                Text(it, color = Warn, style = MaterialTheme.typography.bodySmall)
            }

            state.meta?.let { MetaCard(it) }

            if (state.jobs.isNotEmpty()) {
                Text("Downloads", fontWeight = FontWeight.SemiBold)
                state.jobs.forEach { job ->
                    JobCard(job, onCancel = { viewModel.onCancel(job.jobId) })
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = Accent, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(40.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { Text("▶", color = Color.White, fontWeight = FontWeight.Bold) }
        }
        Column {
            Text("YT-Plucker", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "pluck videos from YouTube & X, straight to disk",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualityPicker(
    selected: Quality,
    availableHeights: List<Int>,
    onSelect: (Quality) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    // After an Analyze, only offer resolution tiers the source actually has (video only).
    // "best" and audio-only options are always available; the `/b` format fallback keeps
    // any choice safe even when a source's resolutions are limited (e.g. short X clips).
    val maxH = availableHeights.maxOrNull()
    val minH = availableHeights.minOrNull()
    fun isEnabled(q: Quality): Boolean {
        val h = q.id.toIntOrNull() ?: return true // best / mp3 / m4a
        if (maxH == null) return true              // not analyzed yet
        return h in minH!!..maxH
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Quality") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Quality.entries.forEach { q ->
                DropdownMenuItem(
                    text = { Text(q.label) },
                    enabled = isEnabled(q),
                    onClick = { onSelect(q); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun MetaCard(meta: tech.acachi.ytplucker.domain.model.VideoMeta) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (meta.thumbnailUrl != null) {
                AsyncImage(
                    model = meta.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
            Text(meta.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            // "YouTube · 3:41" / "X (Twitter) · 0:22" — source label joined with duration.
            val subtitle = listOfNotNull(
                meta.prettySource,
                meta.durationSeconds?.let { formatDuration(it) },
            ).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun JobCard(job: DownloadProgress, onCancel: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(job.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            if (job.status == JobStatus.RUNNING) {
                LinearProgressIndicator(
                    progress = { (job.percent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(statusLine(job), color = statusColor(job.status), style = MaterialTheme.typography.bodySmall)
                if (job.status == JobStatus.RUNNING) {
                    OutlinedButton(onClick = onCancel) { Text("Cancel") }
                }
            }
        }
    }
}

private fun statusLine(job: DownloadProgress): String = when (job.status) {
    JobStatus.RUNNING -> {
        val eta = if (job.etaSeconds > 0) " • ETA ${formatDuration(job.etaSeconds.toInt())}" else ""
        "${job.percent.toInt()}% • ${formatSpeed(job.speedBytesPerSec)}$eta"
    }
    JobStatus.COMPLETED -> "Completed"
    JobStatus.CANCELLED -> "Cancelled"
    JobStatus.FAILED -> "Failed: ${job.error ?: "unknown error"}"
}

private fun statusColor(status: JobStatus): Color = when (status) {
    JobStatus.COMPLETED -> Ok
    JobStatus.FAILED -> Warn
    else -> Accent
}

private fun formatDuration(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
