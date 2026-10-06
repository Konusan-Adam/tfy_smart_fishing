package com.example.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val changelog: String,
    val apkDownloadUrl: String,
    val isForceUpdate: Boolean = false,
    val publishedAt: String = ""
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult()
    data class NoUpdate(val currentVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

/**
 * 📲 GITHUB OVER-THE-AIR (OTA) IN-APP GÜNCELLEME YÖNETİCİSİ
 * - Google Play Store gerektirmeden doğrudan GitHub Releases üzerinden çalışır
 * - En son sürümü kontrol eder, changelog'u alır
 * - İlerleme çubuğu ile APK'yı indirir
 * - Android FileProvider ve PackageInstaller ile tek dokunuşla güncellemeyi başlatır
 */
class AppUpdateManager(private val context: Context) {

    companion object {
        const val DEFAULT_GITHUB_OWNER = "tfy-fishing"
        const val DEFAULT_GITHUB_REPO = "tfy-smart-fishing"
        private const val PREFS_NAME = "tfy_updater_prefs"
        private const val KEY_CUSTOM_REPO = "custom_github_repo"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getTargetRepository(): String {
        return prefs.getString(KEY_CUSTOM_REPO, "$DEFAULT_GITHUB_OWNER/$DEFAULT_GITHUB_REPO")
            ?: "$DEFAULT_GITHUB_OWNER/$DEFAULT_GITHUB_REPO"
    }

    fun setTargetRepository(repoPath: String) {
        prefs.edit().putString(KEY_CUSTOM_REPO, repoPath.trim()).apply()
    }

    val currentVersionName: String
        get() = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }

    val currentVersionCode: Int
        get() = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (_: Exception) {
            1
        }

    /**
     * GitHub Releases API'sinden en son yayınlanan sürümü sorgular
     */
    suspend fun checkForUpdates(customRepo: String? = null): UpdateCheckResult = withContext(Dispatchers.IO) {
        val repo = customRepo ?: getTargetRepository()
        val apiUrl = "https://api.github.com/repos/$repo/releases/latest"

        try {
            val url = URL(apiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "TFY-Smart-Fishing-App")
                connectTimeout = 8000
                readTimeout = 8000
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)

                val tagName = json.optString("tag_name", "").removePrefix("v").trim()
                val body = json.optString("body", "Yeni özellikler ve performans iyileştirmeleri.")
                val publishedAt = json.optString("published_at", "")

                // Asset listesinde .apk dosyasını ara
                val assets = json.optJSONArray("assets")
                var apkUrl = ""
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }

                // Sürüm karşılaştırması
                val isNewer = isVersionNewer(remoteVersion = tagName, currentVersion = currentVersionName)

                if (isNewer && apkUrl.isNotBlank()) {
                    val updateInfo = AppUpdateInfo(
                        versionName = tagName,
                        versionCode = currentVersionCode + 1,
                        changelog = body,
                        apkDownloadUrl = apkUrl,
                        isForceUpdate = body.contains("[FORCE]", ignoreCase = true),
                        publishedAt = publishedAt
                    )
                    UpdateCheckResult.UpdateAvailable(updateInfo)
                } else {
                    UpdateCheckResult.NoUpdate(currentVersion = currentVersionName)
                }
            } else if (responseCode == 404) {
                UpdateCheckResult.Error("GitHub'da henüz yayınlanmış bir sürüm (Release) bulunamadı.")
            } else {
                UpdateCheckResult.Error("GitHub API Hatası: HTTP $responseCode")
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error("Güncelleme denetlenirken bağlantı hatası oluştu: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * SemVer Karşılaştırması (Örn: 1.2.0 > 1.1.5)
     */
    private fun isVersionNewer(remoteVersion: String, currentVersion: String): Boolean {
        if (remoteVersion.isBlank()) return false
        try {
            val remoteParts = remoteVersion.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
            val currentParts = currentVersion.split(".").map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }

            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        } catch (_: Exception) {
            return remoteVersion != currentVersion
        }
    }

    /**
     * APK'yı arka planda indirir ve indirme ilerlemesini (0.0f - 1.0f) iletir
     */
    suspend fun downloadApk(
        downloadUrl: String,
        versionName: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val url = URL(downloadUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 20000
            }

            val fileLength = connection.contentLength
            val cacheDir = File(context.getExternalFilesDir(null) ?: context.cacheDir, "updates")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val outputFile = File(cacheDir, "TFY_Smart_Fishing_v$versionName.apk")
            if (outputFile.exists()) outputFile.delete()

            connection.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (fileLength > 0) {
                            val progress = (totalBytesRead.toFloat() / fileLength.toFloat()).coerceIn(0f, 1f)
                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                    output.flush()
                }
            }
            withContext(Dispatchers.Main) {
                onProgress(1.0f)
            }
            outputFile
        } catch (e: Exception) {
            android.util.Log.e("AppUpdateManager", "APK download error: ${e.message}", e)
            null
        }
    }

    /**
     * İndirilen APK dosyasını Android PackageInstaller'a yönlendirerek kurulumu başlatır
     */
    fun installApk(apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() <= 0) return false

        try {
            // Android 8.0+ (Oreo) için bilinmeyen kaynak kontrolü
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    return false
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
            return true
        } catch (e: Exception) {
            android.util.Log.e("AppUpdateManager", "Install APK error: ${e.message}", e)
            return false
        }
    }
}
