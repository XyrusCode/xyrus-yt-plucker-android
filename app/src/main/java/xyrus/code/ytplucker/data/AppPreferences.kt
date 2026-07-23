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

    // -----------------------------------------------------------------------------------
    // Cookie storage per platform
    // -----------------------------------------------------------------------------------

    private val _loggedInPlatforms = MutableStateFlow(
        SUPPORTED_PLATFORM_KEYS.associateWith { key ->
            prefs.getString(cookiePrefKey(key), null) != null
        }
    )
    val loggedInPlatforms: StateFlow<Map<String, Boolean>> = _loggedInPlatforms.asStateFlow()

    fun getCookies(platformKey: String): String? =
        prefs.getString(cookiePrefKey(platformKey), null)

    fun setCookies(platformKey: String, cookies: String) {
        prefs.edit().putString(cookiePrefKey(platformKey), cookies).apply()
        _loggedInPlatforms.value = _loggedInPlatforms.value + (platformKey to true)
    }

    fun clearCookies(platformKey: String) {
        prefs.edit().remove(cookiePrefKey(platformKey)).apply()
        _loggedInPlatforms.value = _loggedInPlatforms.value + (platformKey to false)
    }

    fun isLoggedIn(platformKey: String): Boolean =
        prefs.getString(cookiePrefKey(platformKey), null) != null

    private fun cookiePrefKey(platformKey: String): String = "cookies_$platformKey"

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_BROWSER_OPT_IN = "browser_opt_in"

        val SUPPORTED_PLATFORM_KEYS = listOf("youtube", "twitter", "tiktok")
    }
}
