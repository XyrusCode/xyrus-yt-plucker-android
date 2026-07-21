package xyrus.code.ytplucker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import xyrus.code.ytplucker.domain.model.Quality
import xyrus.code.ytplucker.domain.model.normalizeForEngine
import xyrus.code.ytplucker.service.DownloadService
import xyrus.code.ytplucker.ui.BrowserScreen
import xyrus.code.ytplucker.ui.BrowserViewModel
import xyrus.code.ytplucker.ui.DownloadScreen
import xyrus.code.ytplucker.ui.DownloadViewModel
import xyrus.code.ytplucker.ui.HistoryScreen
import xyrus.code.ytplucker.ui.HistoryViewModel
import xyrus.code.ytplucker.ui.SettingsScreen
import xyrus.code.ytplucker.ui.theme.YtPluckerTheme

class MainActivity : ComponentActivity() {

    private val sharedUrl = mutableStateOf<String?>(null)

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedUrl.value = extractSharedUrl(intent)
        val app = application as YtPluckerApp
        setContent {
            YtPluckerTheme {
                LaunchedEffect(Unit) { requestNeededPermissions() }
                AppRoot(sharedUrl, app)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractSharedUrl(intent)?.let { sharedUrl.value = it }
    }

    private fun requestNeededPermissions() {
        val perms = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        if (perms.isNotEmpty()) requestPermissions.launch(perms.toTypedArray())
    }

    private fun extractSharedUrl(intent: Intent?): String? {
        intent ?: return null
        return when (intent.action) {
            Intent.ACTION_SEND ->
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let { firstUrl(it) }
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        }
    }

    private fun firstUrl(text: String): String? =
        Regex("""https?://\S+""").find(text)?.value
}

private fun triggerDownload(context: Context, url: String) {
    val jobId = "job-${System.currentTimeMillis()}"
    val intent = DownloadService.startIntent(
        context, jobId, normalizeForEngine(url), Quality.BEST, destDir = null,
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private enum class Tab(val label: String, val icon: Int) {
    BROWSER("Browser", R.drawable.ic_browser),
    DOWNLOAD("Download", R.drawable.ic_download),
    HISTORY("History", R.drawable.ic_history),
    SETTINGS("Settings", R.drawable.ic_settings),
}

@Composable
private fun AppRoot(
    sharedUrl: androidx.compose.runtime.MutableState<String?>,
    app: YtPluckerApp,
) {
    val context = LocalContext.current
    val browserVm: BrowserViewModel = viewModel()
    val downloadVm: DownloadViewModel = viewModel()
    val historyVm: HistoryViewModel = viewModel()

    val flagsState by app.featureFlags.state.collectAsState()

    // Hold on a spinner until feature flags resolve (Remote Config fetch, cancel,
    // failure, or the no-Firebase fallback). Avoids flashing default UI and the
    // Browser tab popping in/out once the real flags land.
    if (!flagsState.ready) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val browserOptIn by app.preferences.browserOptIn.collectAsState()

    val browserVisible = flagsState.browserForceEnabled ||
        (flagsState.browserOptInAllowed && browserOptIn)

    var tab by remember {
        mutableStateOf(
            if (browserVisible) Tab.BROWSER else Tab.DOWNLOAD
        )
    }

    val visibleTabs = remember(browserVisible) {
        Tab.entries.filter { it != Tab.BROWSER || browserVisible }
    }

    if (!visibleTabs.contains(tab)) {
        tab = Tab.DOWNLOAD
    }

    LaunchedEffect(sharedUrl.value) {
        sharedUrl.value?.let { url ->
            if (browserVisible) {
                browserVm.loadUrl(url)
                tab = Tab.BROWSER
            } else {
                triggerDownload(context, url)
                tab = Tab.DOWNLOAD
            }
            sharedUrl.value = null
        }
    }

    LaunchedEffect(tab) { if (tab == Tab.HISTORY) historyVm.refresh() }

    val updateInfo by app.pendingUpdate.collectAsState()
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }

    if (updateInfo != null && !downloading) {
        AlertDialog(
            onDismissRequest = { app.pendingUpdate.value = null },
            title = { Text(stringResource(R.string.update_available)) },
            text = {
                Text(stringResource(R.string.update_message,
                    updateInfo!!.latestVersion, BuildConfig.VERSION_NAME))
            },
            confirmButton = {
                TextButton(onClick = {
                    downloading = true
                    scope.launch {
                        val file = app.updateChecker.downloadApk(updateInfo!!)
                        if (file != null) {
                            app.updateChecker.installApk(file)
                        }
                        downloading = false
                        app.pendingUpdate.value = null
                    }
                }) { Text(stringResource(R.string.update_now)) }
            },
            dismissButton = {
                TextButton(onClick = { app.pendingUpdate.value = null }) {
                    Text(stringResource(R.string.update_later))
                }
            },
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                visibleTabs.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(painterResource(t.icon), contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { inner ->
        Box(modifier = Modifier.padding(inner).statusBarsPadding()) {
            when (tab) {
                Tab.BROWSER -> BrowserScreen(
                    viewModel = browserVm,
                    onNavigateToDownloads = { tab = Tab.HISTORY },
                )
                Tab.DOWNLOAD -> DownloadScreen(viewModel = downloadVm)
                Tab.HISTORY -> HistoryScreen(viewModel = historyVm)
                Tab.SETTINGS -> SettingsScreen(
                    featureFlags = app.featureFlags,
                    preferences = app.preferences,
                )
            }
        }
    }
}
