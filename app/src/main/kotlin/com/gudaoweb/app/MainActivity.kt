package com.gudaoweb.app

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsControllerCompat
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContent {
            MainActivityContent()
        }
    }

    fun setStatusBarColorFromWeb(color: Int) {
        runOnUiThread {
            window.statusBarColor = color
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val isLight = (Color.red(color) * 0.299 + Color.green(color) * 0.587 + Color.blue(color) * 0.114) > 186
                WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLight
            }
        }
    }
}

@Composable
fun MainActivityContent() {
    val context = LocalContext.current
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
                val jsInterface = JavaScriptInterface(context as MainActivity)
                jsInterface.setWebView(this)
                addJavascriptInterface(jsInterface, "Android")
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
                                write: function(fileName, content, callbackName) {
                                    Android.writeFile(fileName, content, callbackName);
                                },
                                read: function(fileName, callbackName) {
                                    Android.readFile(fileName, callbackName);
                                }
                            };
                            """.trimIndent(),
                            null
                        )
                    }
                }
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
                            #fileStatus { margin-top: 8px; color: #333; }
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
                        <textarea id="fileContent" rows="4" placeholder="文件内容..."></textarea>
                        <div id="fileStatus"></div>
                        <script>
                            window.fileWriteCallback = function(error, result) {
                                if (error) {
                                    document.getElementById('fileStatus').innerHTML = '保存失败: ' + error;
                                } else {
                                    document.getElementById('fileStatus').innerHTML = '保存成功: ' + result;
                                }
                            };
                            window.fileReadCallback = function(error, content) {
                                if (error) {
                                    document.getElementById('fileStatus').innerHTML = '读取失败: ' + error;
                                } else {
                                    document.getElementById('fileStatus').innerHTML = '读取成功，内容: ' + content;
                                    document.getElementById('fileContent').value = content;
                                }
                            };
                            function saveFile() {
                                var content = document.getElementById('fileContent').value;
                                file.write('test.txt', content, 'fileWriteCallback');
                            }
                            function loadFile() {
                                file.read('test.txt', 'fileReadCallback');
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

private class JavaScriptInterface(private val activity: MainActivity) {
    private var webView: WebView? = null
    fun setWebView(webView: WebView) { this.webView = webView }

    @JavascriptInterface
    fun showDialog(title: String, message: String) {
        activity.runOnUiThread {
            android.app.AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    @JavascriptInterface
    fun setStatusBarColor(colorCode: String) {
        try {
            activity.setStatusBarColorFromWeb(Color.parseColor(colorCode))
        } catch (e: Exception) { e.printStackTrace() }
    }

    @JavascriptInterface
    fun writeFile(fileName: String, content: String, callbackName: String) {
        Thread {
            try {
                val file = File(activity.filesDir, fileName)
                FileOutputStream(file).use { fos -> fos.write(content.toByteArray()) }
                evaluateJavascript("$callbackName(null, ${escapeJsString("写入成功")})")
            } catch (e: Exception) {
                evaluateJavascript("$callbackName(${escapeJsString(e.message ?: "未知错误")}, null)")
            }
        }.start()
    }

    @JavascriptInterface
    fun readFile(fileName: String, callbackName: String) {
        Thread {
            try {
                val file = File(activity.filesDir, fileName)
                if (!file.exists()) {
                    evaluateJavascript("$callbackName(${escapeJsString("文件不存在")}, null)")
                    return@Thread
                }
                val content = FileInputStream(file).use { fis ->
                    BufferedReader(InputStreamReader(fis)).use { reader -> reader.readText() }
                }
                evaluateJavascript("$callbackName(null, ${escapeJsString(content)})")
            } catch (e: Exception) {
                evaluateJavascript("$callbackName(${escapeJsString(e.message ?: "读取失败")}, null)")
            }
        }.start()
    }

    private fun evaluateJavascript(js: String) {
        webView?.post { webView?.evaluateJavascript(js, null) }
    }

    private fun escapeJsString(s: String): String {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
    }
}
