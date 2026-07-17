package xyrus.code.ytplucker

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val browserVm: BrowserViewModel = viewModel()
    val downloadVm: DownloadViewModel = viewModel()
    val historyVm: HistoryViewModel = viewModel()

    val flagsState by app.featureFlags.state.collectAsState()
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
        sharedUrl.value?.let {
            browserVm.loadUrl(it)
            tab = Tab.BROWSER
            sharedUrl.value = null
        }
    }

    LaunchedEffect(tab) { if (tab == Tab.HISTORY) historyVm.refresh() }

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
