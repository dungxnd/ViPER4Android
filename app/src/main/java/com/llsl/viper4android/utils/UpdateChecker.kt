package com.dxnd.viper4android.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val apkUrl: String?,
    val apkName: String?,
)

sealed interface UpdateResult {
    data class Available(
        val release: ReleaseInfo,
    ) : UpdateResult

    data class UpToDate(
        val release: ReleaseInfo,
    ) : UpdateResult

    data class Error(
        val message: String,
    ) : UpdateResult
}

@Singleton
class UpdateChecker
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val client: OkHttpClient,
    ) {
        companion object {
            private const val TAG = "UpdateChecker"
            private const val OWNER = "likelikeslike"
            private const val REPO = "ViPER4Android"
            private const val LATEST_RELEASE_URL =
                "https://api.github.com/repos/$OWNER/$REPO/releases/latest"
        }

        suspend fun check(currentVersion: String): UpdateResult =
            withContext(Dispatchers.IO) {
                try {
                    val json = fetchLatestRelease()
                    val release = parseRelease(json)
                    if (isNewer(currentVersion, release.tagName)) {
                        UpdateResult.Available(release)
                    } else {
                        UpdateResult.UpToDate(release)
                    }
                } catch (e: Exception) {
                    FileLogger.e(TAG, "Update check failed", e)
                    UpdateResult.Error(e.message ?: "unknown error")
                }
            }

        private fun fetchLatestRelease(): String {
            val request =
                Request
                    .Builder()
                    .url(LATEST_RELEASE_URL)
                    .header("Accept", "application/vnd.github+json")
                    .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP ${response.code}")
                }
                return response.body.string()
            }
        }

        private fun parseRelease(raw: String): ReleaseInfo {
            val obj = JSONObject(raw)
            val assets = obj.optJSONArray("assets")
            var apkUrl: String? = null
            var apkName: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        apkName = name
                        break
                    }
                }
            }
            return ReleaseInfo(
                tagName = obj.optString("tag_name"),
                name = obj.optString("name").ifBlank { obj.optString("tag_name") },
                body = obj.optString("body"),
                htmlUrl = obj.optString("html_url"),
                apkUrl = apkUrl,
                apkName = apkName,
            )
        }

        internal fun isNewer(
            current: String,
            latest: String,
        ): Boolean {
            val cur = parseVersion(current)
            val lat = parseVersion(latest)
            val size = maxOf(cur.size, lat.size)
            for (i in 0 until size) {
                val c = cur.getOrElse(i) { 0 }
                val l = lat.getOrElse(i) { 0 }
                if (l != c) return l > c
            }
            return false
        }

        private fun parseVersion(v: String): List<Int> =
            v
                .trim()
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore('-')
                .split(".")
                .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

        suspend fun download(
            release: ReleaseInfo,
            onProgress: (Int) -> Unit,
        ): File? =
            withContext(Dispatchers.IO) {
                val url = release.apkUrl ?: return@withContext null
                val dir = File(context.cacheDir, "update").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() }
                val outFile = File(dir, release.apkName ?: "update.apk")
                try {
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw IllegalStateException("HTTP ${response.code}")
                        }
                        val body = response.body
                        val total = body.contentLength()
                        body.byteStream().use { input ->
                            outFile.outputStream().use { output ->
                                val buffer = ByteArray(8192)
                                var downloaded = 0L
                                var read: Int
                                while (input.read(buffer).also { read = it } != -1) {
                                    output.write(buffer, 0, read)
                                    downloaded += read
                                    if (total > 0) {
                                        onProgress(((downloaded * 100) / total).toInt())
                                    }
                                }
                            }
                        }
                    }
                    outFile
                } catch (e: Exception) {
                    FileLogger.e(TAG, "APK download failed", e)
                    outFile.delete()
                    null
                }
            }

        fun install(apk: File) {
            val uri: Uri =
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apk,
                )
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
        }
    }
