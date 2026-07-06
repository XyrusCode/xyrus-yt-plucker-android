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
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import xyrus.code.ytplucker.ui.theme.YtPluckerTheme

class MainActivity : ComponentActivity() {

    // Holds a URL delivered via the share sheet (see the SEND intent-filter in the manifest).
    private val sharedUrl = mutableStateOf<String?>(null)

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sharedUrl.value = extractSharedUrl(intent)
        setContent {
            YtPluckerTheme {
                LaunchedEffect(Unit) { requestNeededPermissions() }
                AppRoot(sharedUrl)
            }
        }
    }

    // A share while the app is already running arrives here.
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
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE) // pre-scoped-storage saving
            }
        }
        if (perms.isNotEmpty()) requestPermissions.launch(perms.toTypedArray())
    }

    /** Pull a URL out of a SEND (share) or VIEW (deep link) intent. */
    private fun extractSharedUrl(intent: Intent?): String? {
        intent ?: return null
        return when (intent.action) {
            Intent.ACTION_SEND ->
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let { firstUrl(it) }
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        }
    }

    // Share text is often "caption https://…"; grab the first URL token.
    private fun firstUrl(text: String): String? =
        Regex("""https?://\S+""").find(text)?.value
}

private enum class Tab(val label: String, val icon: Int) {
    BROWSER("Browser", R.drawable.ic_browser),
    DOWNLOAD("Download", R.drawable.ic_download),
    HISTORY("History", R.drawable.ic_history),
}

@Composable
private fun AppRoot(sharedUrl: MutableState<String?>) {
    // All three view models are activity-scoped so they survive tab switches.
    val browserVm: BrowserViewModel = viewModel()
    val downloadVm: DownloadViewModel = viewModel()
    val historyVm: HistoryViewModel = viewModel()
    var tab by remember { mutableStateOf(Tab.BROWSER) }

    // A shared URL loads in the browser and switches to that tab.
    LaunchedEffect(sharedUrl.value) {
        sharedUrl.value?.let {
            browserVm.loadUrl(it)
            tab = Tab.BROWSER
            sharedUrl.value = null
        }
    }
    // Refresh history each time that tab is shown.
    LaunchedEffect(tab) { if (tab == Tab.HISTORY) historyVm.refresh() }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
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
        Box(modifier = Modifier.padding(inner)) {
            when (tab) {
                Tab.BROWSER -> BrowserScreen(
                    viewModel = browserVm,
                    onNavigateToDownloads = { tab = Tab.HISTORY },
                )
                Tab.DOWNLOAD -> DownloadScreen(viewModel = downloadVm)
                Tab.HISTORY -> HistoryScreen(viewModel = historyVm)
            }
        }
    }
}
