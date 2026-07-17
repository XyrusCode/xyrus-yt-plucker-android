package xyrus.code.ytplucker.data

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyrus.code.ytplucker.BuildConfig
import xyrus.code.ytplucker.R

class FeatureFlags(context: Context) {

    private val remoteConfig = FirebaseRemoteConfig.getInstance()

    private val _state = MutableStateFlow(FeatureState())
    val state: StateFlow<FeatureState> = _state.asStateFlow()

    val browserForceEnabled: Boolean get() = remoteConfig.getBoolean(KEY_BROWSER_ENABLED)
    val browserOptInAllowed: Boolean get() = remoteConfig.getBoolean(KEY_BROWSER_OPT_IN_ALLOWED)

    init {
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        val fetchInterval = if (BuildConfig.DEBUG) 0L else 3600L
        remoteConfig.fetch(fetchInterval)
            .addOnSuccessListener {
                remoteConfig.activate().addOnCompleteListener {
                    _state.value = FeatureState(
                        browserForceEnabled = browserForceEnabled,
                        browserOptInAllowed = browserOptInAllowed,
                    )
                }
            }
            .addOnCanceledListener {
                _state.value = FeatureState(
                    browserForceEnabled = browserForceEnabled,
                    browserOptInAllowed = browserOptInAllowed,
                )
            }
    }

    data class FeatureState(
        val browserForceEnabled: Boolean = false,
        val browserOptInAllowed: Boolean = true,
    )

    companion object {
        private const val KEY_BROWSER_ENABLED = "browser_enabled"
        private const val KEY_BROWSER_OPT_IN_ALLOWED = "browser_opt_in_allowed"
    }
}
