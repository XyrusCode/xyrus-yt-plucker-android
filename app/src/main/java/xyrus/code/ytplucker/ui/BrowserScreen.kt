package xyrus.code.ytplucker.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import xyrus.code.ytplucker.R
import xyrus.code.ytplucker.domain.model.Quality
import xyrus.code.ytplucker.ui.theme.Accent
import xyrus.code.ytplucker.ui.theme.Bg
import xyrus.code.ytplucker.ui.theme.BorderCol
import xyrus.code.ytplucker.ui.theme.Panel
import xyrus.code.ytplucker.ui.theme.TextDim

private data class Platform(val name: String, val url: String, val color: Color)

private val platforms = listOf(
    Platform("YouTube", "https://m.youtube.com", Color(0xFFFF0000)),
    Platform("X / Twitter", "https://x.com", Color(0xFF1DA1F2)),
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel = viewModel(),
    onNavigateToDownloads: () -> Unit = {},
) {
    val currentUrl by viewModel.currentUrl.collectAsState()
    val isVideoPage by viewModel.isVideoPage.collectAsState()
    val pendingUrl by viewModel.pendingUrl.collectAsState()

    var urlInput by remember { mutableStateOf("") }
    var showDownloadSheet by remember { mutableStateOf(false) }
    var selectedQuality by remember { mutableStateOf(Quality.BEST) }

    val webViewRef = remember { mutableStateOf<WebView?>(null) }

    // Sync URL bar and load external requests
    LaunchedEffect(pendingUrl) {
        pendingUrl?.let { url ->
            webViewRef.value?.loadUrl(url)
            urlInput = url
            viewModel.onPendingUrlConsumed()
        }
    }
    LaunchedEffect(currentUrl) {
        if (currentUrl.isNotEmpty()) urlInput = currentUrl
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar — clearly visible against the background
            Surface(color = Panel) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        placeholder = { Text("Paste URL or search…") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { viewModel.onUrlEntered(urlInput) }) {
                        Icon(Icons.Default.Search, contentDescription = "Go")
                    }
                    IconButton(onClick = onNavigateToDownloads) {
                        Icon(
                            painter = painterResource(R.drawable.ic_download),
                            contentDescription = "Downloads",
                        )
                    }
                }
            }
            HorizontalDivider(color = BorderCol)

            // Platform buttons — tap to jump straight to that site
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                platforms.forEach { p ->
                    Button(
                        onClick = { viewModel.loadUrl(p.url) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = p.color.copy(alpha = 0.2f)),
                    ) {
                        Text(
                            p.name,
                            fontWeight = FontWeight.Bold,
                            color = p.color,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
            }

            // WebView area — a landing overlay covers the blank WebView until a site is opened,
            // so launch is never an empty screen and the user can jump to YouTube / X.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            setBackgroundColor(android.graphics.Color.parseColor("#0F1115"))
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, urlStr: String?, favicon: Bitmap?) {
                                    urlStr?.let { viewModel.onPageNavigated(it) }
                                }

                                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                    url?.let { viewModel.onPageNavigated(it) }
                                }
                            }
                            webViewRef.value = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                if (currentUrl.isEmpty()) {
                    BrowserLanding(onPick = { viewModel.loadUrl(it) })
                }
            }
        }

        // FAB — visible only when on a video page
        if (isVideoPage) {
            FloatingActionButton(
                onClick = {
                    selectedQuality = Quality.BEST
                    showDownloadSheet = true
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                containerColor = Accent,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_download),
                    contentDescription = "Download video",
                    tint = Color.White,
                )
            }
        }
    }

    // Download bottom sheet
    if (showDownloadSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showDownloadSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            DownloadSheetContent(
                quality = selectedQuality,
                onQualityChange = { selectedQuality = it },
                onDownload = {
                    showDownloadSheet = false
                    viewModel.triggerDownload(currentUrl, selectedQuality)
                },
            )
        }
    }
}

/** Empty-state shown over the WebView on launch: big cards to jump straight to YouTube / X. */
@Composable
private fun BrowserLanding(onPick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Where to?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Open a site, browse to a video, then tap the download button that appears.",
            color = TextDim,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        platforms.forEach { p ->
            Button(
                onClick = { onPick(p.url) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.color.copy(alpha = 0.18f)),
            ) {
                Text(
                    "Open ${p.name}",
                    fontWeight = FontWeight.Bold,
                    color = p.color,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadSheetContent(
    quality: Quality,
    onQualityChange: (Quality) -> Unit,
    onDownload: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Download Video",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = quality.label,
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
                        onClick = {
                            onQualityChange(q)
                            expanded = false
                        },
                    )
                }
            }
        }

        Button(
            onClick = onDownload,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Accent),
        ) {
            Text("Download")
        }

        Spacer(Modifier.height(24.dp))
    }
}
