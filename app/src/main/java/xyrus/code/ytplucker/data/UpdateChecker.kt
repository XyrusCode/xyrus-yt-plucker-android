package xyrus.code.ytplucker.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String,
)

class UpdateChecker(
    private val context: Context,
    private val currentVersion: String,
) {
    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/XyrusCode/xyrus-yt-plucker-android/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Xyrus-YT-Plucker")
            .build()

        val response = okHttp.newCall(request).execute()
        if (!response.isSuccessful) return@withContext null

        val json = JSONObject(response.body?.string() ?: return@withContext null)
        val tagName = json.optString("tag_name")
        if (tagName.isEmpty()) return@withContext null
        val latestVersion = tagName.removePrefix("v")

        if (compareVersions(latestVersion, currentVersion) <= 0) return@withContext null

        val assets = json.optJSONArray("assets") ?: return@withContext null
        val releaseNotes = json.optString("body", "")
        val apkUrl = findMatchingApk(assets, deviceAbi())
            ?: findUniversalApk(assets)
            ?: return@withContext null

        UpdateInfo(latestVersion, apkUrl, releaseNotes)
    }

    suspend fun downloadApk(update: UpdateInfo): File? = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates")
        dir.mkdirs()
        val target = File(dir, "yt-plucker-${update.latestVersion}.apk")
        if (target.exists()) return@withContext target

        try {
            val request = Request.Builder().url(update.downloadUrl).build()
            val response = okHttp.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val body = response.body ?: return@withContext null
            body.byteStream().use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
            target
        } catch (e: Exception) {
            target.delete()
            null
        }
    }

    fun installApk(file: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun findMatchingApk(assets: JSONArray, abi: String): String? {
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString("name", "")
            if (!name.endsWith(".apk")) continue
            val matched = when (abi) {
                "arm64-v8a" -> name.contains("arm64-v8a") || name.contains("arm64")
                "armeabi-v7a" -> name.contains("armeabi-v7a") || name.contains("arm32")
                "x86_64" -> name.contains("x86_64") || name.contains("x64")
                "x86" -> name.contains("x86") && !name.contains("x86_64")
                else -> false
            }
            if (matched) return asset.optString("browser_download_url")
        }
        return null
    }

    private fun findUniversalApk(assets: JSONArray): String? {
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.optString("name", "")
            if (name.contains("universal") && name.endsWith(".apk")) {
                return asset.optString("browser_download_url")
            }
        }
        return null
    }

    private fun deviceAbi(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    } else {
        Build.CPU_ABI
    }

    companion object {
        private fun parseSemver(v: String): List<Int> =
            v.split(".").mapNotNull { it.toIntOrNull() }

        private fun compareVersions(a: String, b: String): Int {
            val pa = parseSemver(a); val pb = parseSemver(b)
            for (i in 0 until maxOf(pa.size, pb.size)) {
                val va = pa.getOrElse(i) { 0 }; val vb = pb.getOrElse(i) { 0 }
                if (va != vb) return va - vb
            }
            return 0
        }
    }
}
