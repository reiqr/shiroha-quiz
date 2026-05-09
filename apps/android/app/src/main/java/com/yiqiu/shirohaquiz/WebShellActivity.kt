package com.yiqiu.shirohaquiz

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File

class WebShellActivity : ComponentActivity() {
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var webView: WebView

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = fileChooserCallback ?: return@registerForActivityResult
            val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
            callback.onReceiveValue(uris ?: emptyArray())
            fileChooserCallback = null
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = false
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = false
            }

            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
            addJavascriptInterface(ShirohaBridge(), "ShirohaAndroid")

            setDownloadListener { url, _, _, _, _ ->
                // Blob 下载由 JS Bridge 处理；普通外部下载交给系统浏览器。
                if (url.startsWith("blob:", ignoreCase = true)) {
                    runOnUiThread {
                        Toast.makeText(
                            this@WebShellActivity,
                            "当前下载由应用保存接口处理；若失败请使用复制备份文本。",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@setDownloadListener
                }
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }.onFailure {
                    runOnUiThread {
                        Toast.makeText(this@WebShellActivity, "无法打开下载链接", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString().orEmpty()
                    return when {
                        url.startsWith("file:///android_asset/") -> false
                        url.startsWith("http://") || url.startsWith("https://") -> {
                            runCatching {
                                startActivity(Intent(Intent.ACTION_VIEW, request?.url))
                            }
                            true
                        }
                        else -> true
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    fileChooserCallback?.onReceiveValue(null)
                    fileChooserCallback = filePathCallback

                    return try {
                        val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }
                        fileChooserLauncher.launch(intent)
                        true
                    } catch (error: Exception) {
                        fileChooserCallback?.onReceiveValue(null)
                        fileChooserCallback = null
                        runOnUiThread {
                            Toast.makeText(this@WebShellActivity, "无法打开文件选择器", Toast.LENGTH_SHORT).show()
                        }
                        false
                    }
                }
            }

            loadUrl("file:///android_asset/web/index.html")
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        setContentView(webView)
    }

    inner class ShirohaBridge {
        @JavascriptInterface
        fun saveJsonFile(fileName: String, content: String): Boolean {
            val safeName = sanitizeFileName(fileName.ifBlank { "shiroha-quiz-backup.json" })
            return runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/json")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: error("无法创建下载文件")
                    contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(content.toByteArray(Charsets.UTF_8))
                    } ?: error("无法写入下载文件")
                } else {
                    val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        ?: filesDir
                    if (!dir.exists()) dir.mkdirs()
                    File(dir, safeName).writeText(content, Charsets.UTF_8)
                }
                runOnUiThread {
                    Toast.makeText(this@WebShellActivity, "已保存备份：$safeName", Toast.LENGTH_SHORT).show()
                }
                true
            }.getOrElse { error ->
                runOnUiThread {
                    Toast.makeText(
                        this@WebShellActivity,
                        "保存失败，请使用复制备份文本：${error.message ?: "未知错误"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                false
            }
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
            .take(96)
            .ifBlank { "shiroha-quiz-backup.json" }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.destroy()
        }
        fileChooserCallback?.onReceiveValue(null)
        fileChooserCallback = null
        super.onDestroy()
    }
}
