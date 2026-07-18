package xyrus.code.ytplucker.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyrus.code.ytplucker.BuildConfig
import xyrus.code.ytplucker.R

class FeatureFlags private constructor(private val remoteConfig: FirebaseRemoteConfig?) {

    constructor(context: Context) : this(obtainRemoteConfig(context))

    private val _state = MutableStateFlow(
        // No Remote Config available → resolve immediately with defaults.
        if (remoteConfig == null) FeatureState(ready = true) else FeatureState(),
    )
    val state: StateFlow<FeatureState> = _state.asStateFlow()

    val browserForceEnabled: Boolean
        get() = remoteConfig?.getBoolean(KEY_BROWSER_ENABLED) ?: DEFAULT_BROWSER_ENABLED
    val browserOptInAllowed: Boolean
        get() = remoteConfig?.getBoolean(KEY_BROWSER_OPT_IN_ALLOWED) ?: DEFAULT_BROWSER_OPT_IN_ALLOWED

    init {
        remoteConfig?.let { config ->
            config.setDefaultsAsync(R.xml.remote_config_defaults)
            val fetchInterval = if (BuildConfig.DEBUG) 0L else 3600L
            config.fetch(fetchInterval)
                .addOnSuccessListener {
                    config.activate().addOnCompleteListener { resolveState() }
                }
                .addOnCanceledListener { resolveState() }
                .addOnFailureListener { resolveState() }
        }
    }

    private fun resolveState() {
        _state.value = FeatureState(
            browserForceEnabled = browserForceEnabled,
            browserOptInAllowed = browserOptInAllowed,
            ready = true,
        )
    }

    data class FeatureState(
        val browserForceEnabled: Boolean = DEFAULT_BROWSER_ENABLED,
        val browserOptInAllowed: Boolean = DEFAULT_BROWSER_OPT_IN_ALLOWED,
        val ready: Boolean = false,
    )

    companion object {
        // Last-resort fallback: an instance that never touches Firebase and
        // immediately resolves to default flags (ready = true).
        fun disabled(): FeatureFlags = FeatureFlags(remoteConfig = null)

        // Defensive: if Firebase's default app never initialized (e.g. a build produced
        // without google-services.json), getInstance() throws. Fall back to null so the
        // app degrades to defaults instead of crashing on launch.
        private fun obtainRemoteConfig(context: Context): FirebaseRemoteConfig? =
            runCatching {
                FirebaseApp.initializeApp(context) // no-op if already initialized
                FirebaseRemoteConfig.getInstance()
            }.getOrNull()

        private const val KEY_BROWSER_ENABLED = "browser_enabled"
        private const val KEY_BROWSER_OPT_IN_ALLOWED = "browser_opt_in_allowed"
        private const val DEFAULT_BROWSER_ENABLED = false
        private const val DEFAULT_BROWSER_OPT_IN_ALLOWED = true
    }
}
