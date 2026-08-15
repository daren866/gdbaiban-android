package com.gudaoweb.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.systemBarsPadding
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import android.util.Base64
import android.view.ViewTreeObserver

@Composable
fun MainActivityContent() {
    val context = LocalContext.current

    AndroidView(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    layoutAlgorithm = android.webkit.WebSettings.LayoutAlgorithm.NORMAL
                    setInitialScale(0)
                    textZoom = 100
                }

                val jsInterface = JavaScriptInterface(context)
                jsInterface.setWebView(this)
                addJavascriptInterface(jsInterface, "Android")

                webChromeClient = object : WebChromeClient() {
                    override fun onShowCustomView(view: android.view.View?, callback: WebChromeClient.CustomViewCallback?) {
                        super.onShowCustomView(view, callback)
                    }

                    override fun onHideCustomView() {
                        super.onHideCustomView()
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.evaluateJavascript(
                            """
                            window.dialog = function(message, title) {
                                Android.showDialog(title, message);
                            };
                            window.statusbar = window.statusbar || {};
                            window.statusbar.color = function(colorCode) {
                                Android.setStatusBarColor(colorCode);
                            };
                            window.file = {
                                write: function(fileName, content, callback) {
                                    Android.writeFile(fileName, content, callback);
                                },
                                read: function(fileName, callback) {
                                    Android.readFile(fileName, callback);
                                }
                            };
                            window.image = {
                                save: function(base64Data, callback) {
                                    Android.saveImage(base64Data, callback);
                                }
                            };
                            """.trimIndent(),
                            null
                        )
                    }
                }

                // 监听 WebView 尺寸变化，当尺寸发生显著变化时（例如异常初始化后被纠正）
                // 主动向页面派发 resize 事件，强制 CSS 布局按新尺寸重新计算，
                // 避免 WebView 创建时为竖屏尺寸、Activity 切到横屏后页面排版异常。
                val resizeListener = object : ViewTreeObserver.OnGlobalLayoutListener {
                    private var lastWidth = 0
                    private var lastHeight = 0

                    override fun onGlobalLayout() {
                        val w = width
                        val h = height
                        if (w > 0 && h > 0 && (w != lastWidth || h != lastHeight)) {
                            lastWidth = w
                            lastHeight = h
                            // 派发 resize 事件，触发页面 CSS 重新计算布局
                            evaluateJavascript(
                                "if(window.dispatchEvent){window.dispatchEvent(new Event('resize'));}",
                                null
                            )
                        }
                    }
                }
                viewTreeObserver.addOnGlobalLayoutListener(resizeListener)

                loadUrl("file:///android_asset/index.html")
            }
        },
        update = { webView ->
            webView.settings.apply {
                loadWithOverviewMode = true
                useWideViewPort = true
            }
            webView.setInitialScale(0)
        }
    )


}

/**
 * JavaScript 接口类，提供原生功能
 */
private class JavaScriptInterface(private val context: android.content.Context) {
    private var webView: WebView? = null

    fun setWebView(webView: WebView) {
        this.webView = webView
    }

    @JavascriptInterface
    fun showDialog(title: String, message: String) {
        (context as? android.app.Activity)?.runOnUiThread {
            android.app.AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    @JavascriptInterface
    fun setStatusBarColor(colorCode: String) {
        val activity = context as? MainActivity ?: return
        activity.runOnUiThread {
            try {
                val color = Color.parseColor(colorCode)
                activity.setStatusBarColorFromWeb(color)
            } catch (e: Exception) {
                // 忽略颜色格式错误
            }
        }
    }

    @JavascriptInterface
    fun writeFile(fileName: String, content: String, callback: String) {
        Thread {
            try {
                val file = File(context.filesDir, fileName)
                FileOutputStream(file).use { fos ->
                    fos.write(content.toByteArray())
                }
                evaluateJavascript(callback, true, "写入成功")
            } catch (e: Exception) {
                evaluateJavascript(callback, false, e.message)
            }
        }.start()
    }

    @JavascriptInterface
    fun readFile(fileName: String, callback: String) {
        Thread {
            try {
                val file = File(context.filesDir, fileName)
                if (!file.exists()) {
                    evaluateJavascript(callback, false, "文件不存在")
                    return@Thread
                }
                val content = FileInputStream(file).use { fis ->
                    BufferedReader(InputStreamReader(fis)).use { reader ->
                        reader.readText()
                    }
                }
                evaluateJavascript(callback, true, content)
            } catch (e: Exception) {
                evaluateJavascript(callback, false, e.message)
            }
        }.start()
    }

    @JavascriptInterface
    fun saveImage(base64Data: String, callback: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val activity = context as? MainActivity ?: run {
                evaluateJavascript(callback, false, "无法获取Activity")
                return
            }
            activity.requestWriteStoragePermission(base64Data) { granted, error ->
                if (granted) {
                    saveImageToAlbum(base64Data, callback)
                } else {
                    evaluateJavascript(callback, false, error)
                }
            }
        } else {
            saveImageToAlbum(base64Data, callback)
        }
    }

    private fun saveImageToAlbum(base64Data: String, callback: String) {
        Thread {
            try {
                var imageData = base64Data
                if (imageData.startsWith("data:image/")) {
                    val index = imageData.indexOf(",")
                    if (index > 0) {
                        imageData = imageData.substring(index + 1)
                    }
                }
                val decodedBytes = Base64.decode(imageData, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                val displayName = "gudaoweb_${System.currentTimeMillis()}.png"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val mimeType = "image/png"
                    val contentValues = android.content.ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/gudaoweb")
                    }

                    var uri: Uri? = null
                    context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let {
                        uri = it
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        }
                    }

                    if (uri != null) {
                        evaluateJavascript(callback, true, "图片已保存到相册")
                    } else {
                        evaluateJavascript(callback, false, "保存图片失败")
                    }
                } else {
                    val dcimDir = File(android.os.Environment.getExternalStorageDirectory(), "DCIM/gudaoweb")
                    if (!dcimDir.exists()) {
                        dcimDir.mkdirs()
                    }
                    val file = File(dcimDir, displayName)
                    FileOutputStream(file).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    }
                    android.media.MediaScannerConnection.scanFile(
                        context,
                        arrayOf(file.absolutePath),
                        arrayOf("image/png"),
                        null
                    )
                    evaluateJavascript(callback, true, "图片已保存到相册")
                }
            } catch (e: Exception) {
                evaluateJavascript(callback, false, e.message ?: "保存图片异常")
            }
        }.start()
    }

    private fun evaluateJavascript(callback: String, success: Boolean, result: String?) {
        webView?.post {
            val js = if (success) {
                "$callback(null, ${escapeJsString(result ?: "")})"
            } else {
                "$callback(${escapeJsString(result ?: "未知错误")}, null)"
            }
            webView?.evaluateJavascript(js, null)
        }
    }

    private fun escapeJsString(s: String): String {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
    }
}
