package xyrus.code.ytplucker.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import xyrus.code.ytplucker.R
import xyrus.code.ytplucker.domain.model.Quality
import xyrus.code.ytplucker.domain.model.SUPPORTED_PLATFORMS
import xyrus.code.ytplucker.domain.model.isWebUrl
import xyrus.code.ytplucker.domain.model.webFallbackFromAppLink
import xyrus.code.ytplucker.ui.theme.Accent
import xyrus.code.ytplucker.ui.theme.Bg
import xyrus.code.ytplucker.ui.theme.BorderCol
import xyrus.code.ytplucker.ui.theme.Panel
import xyrus.code.ytplucker.ui.theme.TextDim

/** Ignore scroll jitter below this, so the bars don't flicker on a twitchy finger. */
private const val SCROLL_HIDE_THRESHOLD_PX = 8

/** Compose hands back whatever Context wraps the Activity, so unwrap rather than cast. */
private fun Context.findActivity(): Activity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

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
    var showExitPrompt by remember { mutableStateOf(false) }

    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val chromeVisible = remember { mutableStateOf(true) }
    val activity = LocalContext.current.findActivity()

    // Back walks the browser's own history first. Only once there's nowhere left to go does it
    // mean "leave the app", and that asks rather than dropping the user out mid-browse.
    BackHandler {
        val webView = webViewRef.value
        if (webView != null && webView.canGoBack()) webView.goBack() else showExitPrompt = true
    }

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
            // The URL bar and cards cost ~a third of a phone screen, so they collapse away as
            // the user reads down a page. Always shown on the landing, where they are the UI.
            AnimatedVisibility(visible = chromeVisible.value || currentUrl.isEmpty()) {
                BrowserChrome(
                    urlInput = urlInput,
                    onUrlInputChange = { urlInput = it },
                    onGo = { viewModel.onUrlEntered(urlInput) },
                    onNavigateToDownloads = onNavigateToDownloads,
                    onPickPlatform = { viewModel.loadUrl(it) },
                )
            }

            // WebView area — a landing overlay covers the blank WebView until a site is opened,
            // so launch is never an empty screen and the user can jump straight to a site.
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
                                /**
                                 * Sites push visitors at their native app with custom schemes
                                 * (TikTok: snssdk1340://). A WebView can't load those and shows
                                 * ERR_UNKNOWN_URL_SCHEME, which dead-ends browsing. Keep every
                                 * non-web scheme out of the WebView, and follow the real page the
                                 * link smuggles along when there is one.
                                 */
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    if (isWebUrl(url)) return false
                                    webFallbackFromAppLink(url)?.let { view?.loadUrl(it) }
                                    return true
                                }

                                override fun onPageStarted(view: WebView?, urlStr: String?, favicon: Bitmap?) {
                                    urlStr?.let { viewModel.onPageNavigated(it) }
                                    chromeVisible.value = true
                                }

                                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                    url?.let { viewModel.onPageNavigated(it) }
                                }
                            }
                            // Chrome-style auto-hide: reclaim the top bar + cards while reading
                            // down the page, bring them back the moment the user reaches up.
                            setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                                val dy = scrollY - oldScrollY
                                when {
                                    scrollY <= 0 -> chromeVisible.value = true
                                    dy > SCROLL_HIDE_THRESHOLD_PX -> chromeVisible.value = false
                                    dy < -SCROLL_HIDE_THRESHOLD_PX -> chromeVisible.value = true
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

    if (showExitPrompt) {
        AlertDialog(
            onDismissRequest = { showExitPrompt = false },
            title = { Text(stringResource(R.string.exit_title)) },
            text = { Text(stringResource(R.string.exit_message)) },
            confirmButton = {
                TextButton(onClick = { showExitPrompt = false; activity?.finish() }) {
                    Text(stringResource(R.string.exit_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitPrompt = false }) {
                    Text(stringResource(R.string.exit_cancel))
                }
            },
        )
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

/** The URL bar and platform cards. Collapses out of the way while the user reads down a page. */
@Composable
private fun BrowserChrome(
    urlInput: String,
    onUrlInputChange: (String) -> Unit,
    onGo: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onPickPlatform: (String) -> Unit,
) {
    Column {
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
                    onValueChange = onUrlInputChange,
                    placeholder = { Text("Paste URL or search…") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onGo) {
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

        // Platform buttons — tap to jump straight to that site. Scrolls rather than sharing
        // the width evenly, so longer names aren't ellipsized as sites are added.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SUPPORTED_PLATFORMS.forEach { p ->
                val color = Color(p.argb)
                Button(
                    onClick = { onPickPlatform(p.homeUrl) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.2f)),
                ) {
                    Text(
                        p.name,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}

/** Empty-state shown over the WebView on launch: big cards to jump straight to a site. */
@Composable
private fun BrowserLanding(onPick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            // Scrolls so the cards stay reachable in landscape and on TV, where the
            // column is far shorter than in portrait.
            .verticalScroll(rememberScrollState())
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
        SUPPORTED_PLATFORMS.forEach { p ->
            val color = Color(p.argb)
            Button(
                onClick = { onPick(p.homeUrl) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.18f)),
            ) {
                Text(
                    "Open ${p.name}",
                    fontWeight = FontWeight.Bold,
                    color = color,
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
