package xyrus.code.ytplucker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyrus.code.ytplucker.data.AppPreferences
import xyrus.code.ytplucker.data.FeatureFlags

@Composable
fun SettingsScreen(
    featureFlags: FeatureFlags,
    preferences: AppPreferences,
) {
    val browserOptIn by preferences.browserOptIn.collectAsState()
    val flagsState by featureFlags.state.collectAsState()

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

            Spacer(Modifier.height(16.dp))
        }
    }
}
