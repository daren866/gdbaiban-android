package com.gudaoweb.app

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import android.util.Base64

@Composable
fun MainActivityContent() {
    val context = LocalContext.current

    // 强制竖屏
    (context as? android.app.Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                }

                // 创建 JavaScript 接口实例并关联当前 WebView
                val jsInterface = JavaScriptInterface(context)
                jsInterface.setWebView(this)  // 正确传递 WebView 实例
                addJavascriptInterface(jsInterface, "Android")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        // 注入全局 JavaScript 函数
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

                // 加载 HTML 内容（包含文件读写测试按钮）
                loadDataWithBaseURL(
                    null,
                    """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>
                            body { margin: 0; padding: 16px; font-family: sans-serif; }
                            button { margin-top: 12px; padding: 8px 16px; font-size: 16px; margin-right: 8px; }
                            .button-group { margin-top: 12px; }
                            textarea { margin-top: 12px; width: 100%; padding: 8px; box-sizing: border-box; }
                        </style>
                    </head>
                    <body>
                        <h1>你好web世界！</h1>
                        <p>古道web框架哦。</p>
                        <button onclick="dialog('test dialog', '测试函数哦')">函数测试</button>
                        <div class="button-group">
                            <button onclick="statusbar.color('#FFFFFF')">白色状态栏</button>
                            <button onclick="statusbar.color('#66CCFF')">蓝色状态栏</button>
                        </div>
                        <div class="button-group">
                            <button onclick="saveFile()">保存文件</button>
                            <button onclick="loadFile()">读取文件</button>
                        </div>
                        <div class="button-group">
                            <button onclick="saveImage()">保存图片到相册</button>
                        </div>
                        <textarea id="fileContent" rows="4" placeholder="文件内容..."></textarea>
                        <div id="fileStatus"></div>
                        <script>
                            function saveFile() {
                                var content = document.getElementById('fileContent').value;
                                file.write('test.txt', content, function(error, result) {
                                    if (error) {
                                        document.getElementById('fileStatus').innerHTML = '保存失败: ' + error;
                                    } else {
                                        document.getElementById('fileStatus').innerHTML = '保存成功: ' + result;
                                    }
                                });
                            }
                            function loadFile() {
                                file.read('test.txt', function(error, content) {
                                    if (error) {
                                        document.getElementById('fileStatus').innerHTML = '读取失败: ' + error;
                                    } else {
                                        document.getElementById('fileStatus').innerHTML = '读取成功，内容: ' + content;
                                        document.getElementById('fileContent').value = content;
                                    }
                                });
                            }
                            function saveImage() {
                                var canvas = document.createElement('canvas');
                                canvas.width = 200;
                                canvas.height = 200;
                                var ctx = canvas.getContext('2d');
                                ctx.fillStyle = '#FF0000';
                                ctx.fillRect(0, 0, 200, 200);
                                ctx.fillStyle = '#FFFFFF';
                                ctx.font = '30px Arial';
                                ctx.textAlign = 'center';
                                ctx.fillText('测试图片', 100, 100);
                                var base64Data = canvas.toDataURL('image/png');
                                image.save(base64Data, function(error, result) {
                                    if (error) {
                                        document.getElementById('fileStatus').innerHTML = '保存图片失败: ' + error;
                                    } else {
                                        document.getElementById('fileStatus').innerHTML = '保存图片成功: ' + result;
                                    }
                                });
                            }
                        </script>
                    </body>
                    </html>
                    """.trimIndent(),
                    "text/html",
                    "UTF-8",
                    null
                )
            }
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
