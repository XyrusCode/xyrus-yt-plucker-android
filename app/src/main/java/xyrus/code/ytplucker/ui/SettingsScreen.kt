package xyrus.code.ytplucker.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import xyrus.code.ytplucker.data.AppPreferences
import xyrus.code.ytplucker.data.FeatureFlags
import xyrus.code.ytplucker.domain.model.SUPPORTED_PLATFORMS
import xyrus.code.ytplucker.domain.model.SitePlatform

@Composable
fun SettingsScreen(
    featureFlags: FeatureFlags,
    preferences: AppPreferences,
) {
    val browserOptIn by preferences.browserOptIn.collectAsState()
    val flagsState by featureFlags.state.collectAsState()
    val loggedInPlatforms by preferences.loggedInPlatforms.collectAsState()
    var loginPlatform by remember { mutableStateOf<SitePlatform?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Settings",
                style = MaterialTheme.typography.titleLarge,
            )

            Text(
                "Experimental features",
                style = MaterialTheme.typography.titleMedium,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Enable experimental browser",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        "The built-in browser is a work in progress. Some sites may not render correctly, and video detection may not work on all pages. Use at your own discretion.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = browserOptIn,
                    onCheckedChange = { preferences.setBrowserOptIn(it) },
                    enabled = flagsState.browserOptInAllowed,
                )
            }

            Text(
                "Login & Cookies",
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                "Log in to save session cookies so yt-dlp can download age-restricted or sensitive content.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SUPPORTED_PLATFORMS.forEach { platform ->
                PlatformLoginRow(
                    platform = platform,
                    isLoggedIn = loggedInPlatforms[platform.cookieKey] == true,
                    onLogin = { loginPlatform = platform },
                    onLogout = { preferences.clearCookies(platform.cookieKey) },
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    loginPlatform?.let { platform ->
        LoginDialog(
            platform = platform,
            preferences = preferences,
            onDismiss = { loginPlatform = null },
        )
    }
}

@Composable
private fun PlatformLoginRow(
    platform: SitePlatform,
    isLoggedIn: Boolean,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(platform.argb)),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(platform.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (isLoggedIn) "Logged in" else "Not logged in",
                style = MaterialTheme.typography.bodySmall,
                color = if (isLoggedIn) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isLoggedIn) {
            OutlinedButton(
                onClick = onLogout,
                shape = RoundedCornerShape(8.dp),
            ) { Text("Logout") }
        } else {
            Button(
                onClick = onLogin,
                shape = RoundedCornerShape(8.dp),
            ) { Text("Login") }
        }
    }
}
