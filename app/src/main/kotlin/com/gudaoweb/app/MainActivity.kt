import android.Manifest
import android.content.pm.PackageManager
import android.os.Environment
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class MainActivity : ComponentActivity() {
    // ... 已有代码 ...

    // 请求码
    private val REQUEST_STORAGE_PERMISSION = 1001

    // 供 JavaScript 调用的下载方法
    fun downloadFile(url: String, fileplace: String, filename: String) {
        // 检查存储权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // 请求权限，并将参数保存
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_STORAGE_PERMISSION)
            // 保存参数以便权限回调后使用
            pendingDownload = Triple(url, fileplace, filename)
            return
        }
        // 已有权限，执行下载
        performDownload(url, fileplace, filename)
    }

    private var pendingDownload: Triple<String, String, String>? = null

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            pendingDownload?.let {
                performDownload(it.first, it.second, it.third)
                pendingDownload = null
            }
        } else {
            Toast.makeText(this, "存储权限被拒绝，无法下载文件", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performDownload(url: String, fileplace: String, filename: String) {
        Thread {
            try {
                // 创建目录
                val dir = File(fileplace)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, filename)

                // 下载文件
                val connection = URL(url).openConnection()
                connection.connect()
                val inputStream = connection.getInputStream()
                val outputStream = FileOutputStream(file)
                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.close()
                inputStream.close()

                // 在主线程显示 Toast
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "下载完成，文件位置：$fileplace",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}
