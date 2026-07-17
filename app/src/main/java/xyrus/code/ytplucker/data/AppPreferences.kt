package xyrus.code.ytplucker.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _browserOptIn = MutableStateFlow(prefs.getBoolean(KEY_BROWSER_OPT_IN, false))
    val browserOptIn: StateFlow<Boolean> = _browserOptIn.asStateFlow()

    fun setBrowserOptIn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BROWSER_OPT_IN, enabled).apply()
        _browserOptIn.value = enabled
    }

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_BROWSER_OPT_IN = "browser_opt_in"
    }
}
