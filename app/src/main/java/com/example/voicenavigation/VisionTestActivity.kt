package com.example.voicenavigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
<<<<<<< HEAD
import android.graphics.Matrix
import android.graphics.RectF
import android.os.SystemClock
=======
import android.net.wifi.WifiManager
>>>>>>> ff19ed6f514731b631f20d3ab0e9b1c5ed599537
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.corsight.inference.Detection
import com.corsight.inference.ModelRegistry
import com.corsight.vision.Frame
import com.corsight.vision.ImageSource
import com.corsight.vision.ToolRegistry
import com.corsight.vision.ToolResult
import com.corsight.vision.tools.GenericDetectionTool
import com.example.voicenavigation.databinding.ActivityVisionTestBinding
<<<<<<< HEAD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.ArrayDeque
=======
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
>>>>>>> ff19ed6f514731b631f20d3ab0e9b1c5ed599537
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class VisionTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVisionTestBinding
    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val inferenceExecutor = Executors.newSingleThreadExecutor()
<<<<<<< HEAD
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Volatile private var destroyed = false
    @Volatile private var cloudRequestRunning = false
    @Volatile private var localInferenceRunning = false
    private var currentSource: ImageSource? = null
    private var activeMode = DetectionMode.LOCAL
    private var lastFrameWidth = 1
    private var lastFrameHeight = 1
    private var lastInferenceAt = 0L
    private val smoothedHistory = ArrayDeque<List<Detection>>()
    private val cameraSource by lazy {
        CameraSource(this, this, binding.previewView, cameraExecutor)
    }
=======

    private var currentSource: ImageSource? = null
    private val cameraSource by lazy {
        CameraSource(this, this, binding.previewView, cameraExecutor)
    }

    // UDP 自动发现相关
    private var udpSocket: DatagramSocket? = null
    private var udpReceiveThread: Thread? = null
    private val UDP_DISCOVERY_PORT = 8888          // 与 ESP32 广播端口一致
    private val AUTO_DISCOVERY_TIMEOUT_MS = 5000L  // 等待广播超时时间
>>>>>>> ff19ed6f514731b631f20d3ab0e9b1c5ed599537

    companion object {
        private const val TAG = "VisionTest"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
<<<<<<< HEAD
    }

    private enum class DetectionMode {
        LOCAL,
        CLOUD
=======
        private const val MODEL_INPUT_SIZE = 640
        private const val DEFAULT_STREAM_PORT = 8080
>>>>>>> ff19ed6f514731b631f20d3ab0e9b1c5ed599537
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVisionTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ToolRegistry.register(GenericDetectionTool())
        ToolRegistry.activate(this, "generic_detection")

        setupUI()
<<<<<<< HEAD
    }

    private fun setupUI() {
        binding.btnSourceCamera.setOnClickListener {
            activeMode = DetectionMode.LOCAL
            startCameraOrRequestPermission()
        }

        binding.btnSourceNetwork.setOnClickListener {
            val serverUrl = getDetectionServerUrl()
            binding.layoutNetworkConfig.visibility = View.VISIBLE
            binding.tvDetectionServer.text =
                if (serverUrl.isEmpty()) "检测服务地址未配置，请在设置中填写" else "检测服务：$serverUrl"
            if (serverUrl.isEmpty()) {
                Toast.makeText(this, "请先在设置中保存检测服务地址", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            activeMode = DetectionMode.CLOUD
=======

        val useExternal = getSharedPreferences("corsight_config", MODE_PRIVATE)
            .getBoolean("use_external_device", false)

        if (useExternal) {
            // 优先尝试外设：启动 UDP 发现，超时后退回相机
            binding.tvDetections.text = "正在寻找外设..."
            startUdpAutoDiscovery(onFound = { ip ->
                connectToNetworkSource(ip, DEFAULT_STREAM_PORT)
            }, onTimeout = {
                Toast.makeText(this, "未找到外设，退回本机相机", Toast.LENGTH_SHORT).show()
                startCameraOrRequestPermission()
            })
        } else {
>>>>>>> ff19ed6f514731b631f20d3ab0e9b1c5ed599537
            startCameraOrRequestPermission()
        }
    }

    private fun startCameraOrRequestPermission() {
        if (cameraSource.allPermissionsGranted()) {
            switchToSource(cameraSource)
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

<<<<<<< HEAD
    private fun switchToSource(source: ImageSource) {
        currentSource?.stop()
        currentSource = null
        binding.tvPreviewHint.visibility = View.GONE
        binding.previewView.visibility = View.VISIBLE
        binding.ivNetwork.visibility = View.GONE

        binding.btnSourceCamera.setBackgroundColor(
            ContextCompat.getColor(this, if (activeMode == DetectionMode.LOCAL) R.color.purple_700 else R.color.gray)
        )
        binding.btnSourceNetwork.setBackgroundColor(
            ContextCompat.getColor(this, if (activeMode == DetectionMode.CLOUD) R.color.purple_700 else R.color.gray)
        )

        val ok = source.start { bitmap, rotation -> processFrame(bitmap, rotation) }
        if (ok) {
            currentSource = source
            binding.tvDetections.text =
                if (activeMode == DetectionMode.LOCAL) "本地检测已启动" else "云端检测已启动"
        } else {
            Toast.makeText(this, "启动相机失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processFrame(bitmap: Bitmap, rotationDegrees: Int) {
        if (destroyed) return
        val now = SystemClock.elapsedRealtime()
        if (activeMode == DetectionMode.LOCAL && (localInferenceRunning || now - lastInferenceAt < 90L)) return
        if (activeMode == DetectionMode.LOCAL) lastInferenceAt = now
        val displayWidth = if (rotationDegrees == 90 || rotationDegrees == 270) bitmap.height else bitmap.width
        val displayHeight = if (rotationDegrees == 90 || rotationDegrees == 270) bitmap.width else bitmap.height
        lastFrameWidth = displayWidth
        lastFrameHeight = displayHeight
        runOnUiThread {
            binding.overlayView.setSourceImageSize(lastFrameWidth, lastFrameHeight)
        }
        when (activeMode) {
            DetectionMode.LOCAL -> processLocalFrame(bitmap, rotationDegrees)
            DetectionMode.CLOUD -> processCloudFrame(bitmap, rotationDegrees)
        }
    }

    private fun processLocalFrame(bitmap: Bitmap, rotationDegrees: Int) {
        localInferenceRunning = true
        inferenceExecutor.execute {
            val frame = Frame(bitmap, rotationDegrees)
            val result = ToolRegistry.activeTool.value?.process(frame)
            runOnUiThread {
                localInferenceRunning = false
                renderResult(result, rotationDegrees)
            }
        }
    }

    private fun processCloudFrame(bitmap: Bitmap, rotationDegrees: Int) {
        if (cloudRequestRunning) return
        val serverUrl = getDetectionServerUrl()
        if (serverUrl.isEmpty()) return

        cloudRequestRunning = true
        runOnUiThread { binding.progressConnecting.visibility = View.VISIBLE }

        val uploadBitmap = bitmap.rotateForDisplay(rotationDegrees)
        val jpegBytes = uploadBitmap.toJpegBytes()
        if (uploadBitmap !== bitmap && !uploadBitmap.isRecycled) {
            uploadBitmap.recycle()
        }
        val imageBody = jpegBytes.toRequestBody("image/jpeg".toMediaType())
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", "frame.jpg", imageBody)
            .addFormDataPart("rotation", rotationDegrees.toString())
            .build()
        val request = Request.Builder()
            .url(serverUrl.trimEnd('/') + "/api/detect")
            .post(body)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                cloudRequestRunning = false
                Log.e(TAG, "Cloud detection failed", e)
                runOnUiThread {
                    binding.progressConnecting.visibility = View.GONE
                    binding.tvDetections.text = "云端检测失败：${e.message}"
                    binding.overlayView.updateDetections(emptyList())
                }
            }

            override fun onResponse(call: Call, response: Response) {
                cloudRequestRunning = false
                val bodyText = response.body?.string().orEmpty()
                val detections = if (response.isSuccessful) parseCloudDetections(bodyText) else emptyList()
                runOnUiThread {
                    binding.progressConnecting.visibility = View.GONE
                    if (response.isSuccessful) {
                        renderDetections(detections, rotationDegrees)
                    } else {
                        binding.tvDetections.text = "云端检测失败：HTTP ${response.code}"
                        binding.overlayView.updateDetections(emptyList())
                    }
                }
            }
        })
    }

    private fun renderResult(result: ToolResult?, rotationDegrees: Int) {
        when (result) {
            is ToolResult.Detections -> renderDetections(result.items, rotationDegrees)
            else -> renderDetections(emptyList(), rotationDegrees)
        }
    }

    private fun renderDetections(items: List<Detection>, rotationDegrees: Int) {
        val stableItems = stabilizeDetections(items)
        binding.tvDetections.text = if (stableItems.isEmpty()) {
            "æœªæ£€æµ‹åˆ°æ˜Žæ˜¾éšœç¢ç‰©"
        } else {
            stableItems.take(5).joinToString("\n") {
                "${it.label}: ${(it.score * 100).toInt()}%"
            }
        }

        val previewW = binding.overlayView.width
        val previewH = binding.overlayView.height
        if (previewW > 0 && previewH > 0) {
            binding.overlayView.updateDetections(stableItems)
        }
    }

    private fun stabilizeDetections(items: List<Detection>): List<Detection> {
        if (items.isEmpty()) {
            smoothedHistory.clear()
            return emptyList()
        }

        val historyFrames = smoothedHistory.toList()
        val stable = items.mapNotNull { current ->
            val matched = historyFrames.asReversed()
                .flatMap { frame -> frame.filter { isSameTarget(current, it) } }
                .take(4)

            if (matched.isEmpty()) current else mergeDetections(matched + current)
        }

        smoothedHistory.addLast(stable)
        while (smoothedHistory.size > 5) {
            smoothedHistory.removeFirst()
        }
        return stable
    }

    private fun mergeDetections(items: List<Detection>): Detection {
        var left = 0f
        var top = 0f
        var right = 0f
        var bottom = 0f
        var score = 0f
        for (item in items) {
            left += item.box.left
            top += item.box.top
            right += item.box.right
            bottom += item.box.bottom
            score += item.score
        }
        val count = items.size.coerceAtLeast(1)
        return items.first().copy(
            box = RectF(left / count, top / count, right / count, bottom / count),
            score = (score / count).coerceIn(0f, 1f)
        )
    }

    private fun isSameTarget(a: Detection, b: Detection): Boolean {
        return a.classId == b.classId && a.label == b.label && iou(a.box, b.box) >= 0.35f
    }

    private fun iou(box1: RectF, box2: RectF): Float {
        val x1 = maxOf(box1.left, box2.left)
        val y1 = maxOf(box1.top, box2.top)
        val x2 = minOf(box1.right, box2.right)
        val y2 = minOf(box1.bottom, box2.bottom)
        val intersection = maxOf(0f, x2 - x1) * maxOf(0f, y2 - y1)
        val union = box1.width() * box1.height() + box2.width() * box2.height() - intersection
        return if (union > 0f) intersection / union else 0f
    }
    private fun parseCloudDetections(json: String): List<Detection> {
        return try {
            val root = JSONObject(json)
            val array = when {
                root.has("detections") -> root.optJSONArray("detections")
                root.has("data") -> root.optJSONObject("data")?.optJSONArray("detections")
                root.has("items") -> root.optJSONArray("items")
                else -> JSONArray()
            } ?: JSONArray()

            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val score = item.optDouble("score", item.optDouble("confidence", 0.0)).toFloat()
                    val label = item.optString("label", item.optString("class_name", "unknown"))
                    val classId = item.optInt("class_id", item.optInt("classId", -1))
                    val box = parseBox(item)
                    add(Detection(box, score, classId, label))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse cloud detections failed", e)
            emptyList()
        }
    }

    private fun parseBox(item: JSONObject): RectF {
        val boxArray = item.optJSONArray("box") ?: item.optJSONArray("bbox")
        if (boxArray != null && boxArray.length() >= 4) {
            return normalizeBox(
                boxArray.optDouble(0).toFloat(),
                boxArray.optDouble(1).toFloat(),
                boxArray.optDouble(2).toFloat(),
                boxArray.optDouble(3).toFloat()
            )
        }
        val x1 = item.optDouble("x1", item.optDouble("left", 0.0)).toFloat()
        val y1 = item.optDouble("y1", item.optDouble("top", 0.0)).toFloat()
        val x2 = item.optDouble("x2", item.optDouble("right", 0.0)).toFloat()
        val y2 = item.optDouble("y2", item.optDouble("bottom", 0.0)).toFloat()
        val width = item.optDouble("width", 0.0).toFloat()
        val height = item.optDouble("height", 0.0).toFloat()
        if (width > 0f && height > 0f && x2 <= 1.5f && y2 <= 1.5f) {
            return normalizeBox(x1, y1, x1 + width, y1 + height)
        }
        return normalizeBox(x1, y1, x2, y2)
    }

    private fun normalizeBox(x1: Float, y1: Float, x2: Float, y2: Float): RectF {
        return if (x2 <= 1.5f && y2 <= 1.5f) {
            RectF(
                x1 * lastFrameWidth,
                y1 * lastFrameHeight,
                x2 * lastFrameWidth,
                y2 * lastFrameHeight
            )
        } else {
            RectF(x1, y1, x2, y2)
        }
    }

    private fun Bitmap.toJpegBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, output)
        return output.toByteArray()
    }

    private fun Bitmap.rotateForDisplay(rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return this
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun getDetectionServerUrl(): String {
        return AppConfig.prefs(this)
            .getString(AppConfig.KEY_DETECTION_SERVER_BASE_URL, "")
            .orEmpty()
            .trim()
=======
    private fun setupUI() {
        binding.btnSourceCamera.setOnClickListener {
            if (cameraSource.allPermissionsGranted()) {
                switchToSource(cameraSource)
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            }
        }

        // 网络流按钮：点击时展开配置面板，并同时启动自动发现（不阻塞UI）
        binding.btnSourceNetwork.setOnClickListener {
            if (binding.layoutNetworkConfig.visibility != View.VISIBLE) {
                binding.layoutNetworkConfig.visibility = View.VISIBLE
            }
            startUdpAutoDiscovery(
                onFound = { ip -> connectToNetworkSource(ip, DEFAULT_STREAM_PORT) },
                onTimeout = {
                    Toast.makeText(this, "未找到外设，请手动输入 IP", Toast.LENGTH_SHORT).show()
                }
            )
        }

        binding.btnConnectStream.setOnClickListener {
            val text = binding.etStreamIp.text.toString().trim()
            val parts = text.split(":")
            val ip = parts[0]
            val port = if (parts.size > 1) parts[1].toIntOrNull() ?: DEFAULT_STREAM_PORT else DEFAULT_STREAM_PORT

            if (ip.isEmpty()) {
                Toast.makeText(this, "请输入 IP 地址", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            connectToNetworkSource(ip, port)
        }
    }

    // ==================== UDP 自动发现 ====================

    /**
     * 启动 UDP 广播监听，等待 ESP32 发送身份消息。
     * @param onFound 发现设备后的回调（在主线程执行）
     * @param onTimeout 超时后的回调（在主线程执行）
     */
    private fun startUdpAutoDiscovery(
        onFound: ((String) -> Unit)? = null,
        onTimeout: (() -> Unit)? = null
    ) {
        if (udpReceiveThread != null && udpReceiveThread!!.isAlive) {
            Toast.makeText(this, "正在自动发现中，请稍候...", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressConnecting.visibility = View.VISIBLE
        binding.tvDetections.text = "等待设备广播..."

        udpReceiveThread = Thread {
            try {
                udpSocket = DatagramSocket(UDP_DISCOVERY_PORT).apply {
                    soTimeout = 1000
                }
                val buffer = ByteArray(512)
                val packet = DatagramPacket(buffer, buffer.size)

                val startTime = System.currentTimeMillis()
                var found = false

                while (!found && System.currentTimeMillis() - startTime < AUTO_DISCOVERY_TIMEOUT_MS) {
                    try {
                        udpSocket?.receive(packet)
                        val msg = String(packet.data, 0, packet.length)
                        Log.d(TAG, "UDP 收到广播: $msg")

                        val ip = extractIpFromMessage(msg)
                        if (ip != null) {
                            runOnUiThread {
                                binding.progressConnecting.visibility = View.GONE
                                binding.tvDetections.text = "发现设备: $ip"
                                binding.etStreamIp.setText(ip)
                                if (onFound != null) {
                                    onFound(ip)
                                } else {
                                    connectToNetworkSource(ip, DEFAULT_STREAM_PORT)
                                }
                            }
                            found = true
                        }
                    } catch (e: SocketTimeoutException) {
                        // 超时继续下一次循环
                    }
                }

                if (!found) {
                    runOnUiThread {
                        binding.progressConnecting.visibility = View.GONE
                        binding.tvDetections.text = "自动发现超时"
                        if (onTimeout != null) {
                            onTimeout()
                        } else {
                            Toast.makeText(this@VisionTestActivity,
                                "未收到设备广播，请确保 ESP32 已连接热点并正在发送广播", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "UDP 接收错误", e)
                runOnUiThread {
                    binding.progressConnecting.visibility = View.GONE
                    binding.tvDetections.text = "自动发现失败"
                    if (onTimeout != null) {
                        onTimeout()
                    } else {
                        Toast.makeText(this@VisionTestActivity, "UDP 监听失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                closeUdpSocket()
                udpReceiveThread = null
            }
        }
        udpReceiveThread?.start()
>>>>>>> ff19ed6f514731b631f20d3ab0e9b1c5ed599537
    }

    /**
     * 解析广播消息中的 IP 地址
     * 支持格式: "ESP32_CAM IP=192.168.43.10 TCP=8080"
     */
    private fun extractIpFromMessage(message: String): String? {
        val ipPattern = Regex("""IP=(\d+\.\d+\.\d+\.\d+)""")
        val match = ipPattern.find(message)
        return match?.groupValues?.get(1)
    }

    private fun closeUdpSocket() {
        try {
            udpSocket?.close()
            udpSocket = null
        } catch (e: Exception) { }
    }

    // ==================== 网络流连接 ====================

    private fun connectToNetworkSource(ip: String, port: Int) {
        val newSource = NetworkSource(ip, port)
        switchToSource(newSource)
        binding.layoutNetworkConfig.visibility = View.GONE
    }

    // ==================== 源切换 ====================

    private fun switchToSource(source: ImageSource) {
        currentSource?.stop()
        currentSource = null

        binding.previewView.visibility =
            if (source is CameraSource) View.VISIBLE else View.GONE
        binding.ivNetwork.visibility =
            if (source !is CameraSource) View.VISIBLE else View.GONE

        binding.btnSourceCamera.setBackgroundColor(
            ContextCompat.getColor(this,
                if (source is CameraSource) R.color.purple_700 else R.color.gray))
        binding.btnSourceNetwork.setBackgroundColor(
            ContextCompat.getColor(this,
                if (source !is CameraSource) R.color.purple_700 else R.color.gray))

        val ok = source.start { bitmap, rotation -> processFrame(bitmap, rotation, source) }
        if (ok) {
            currentSource = source
            binding.tvDetections.text = "源已切换: ${source.displayName}"
        } else {
            Toast.makeText(this, "启动 ${source.displayName} 失败", Toast.LENGTH_SHORT).show()
        }
    }

    // ==================== 帧处理 ====================

    /**
     * 统一帧处理入口。
     * 相机：直接走检测管线（CameraX 的 analyzer 已在后台线程）。
     * 网络流：先显示原图，再在后台线程跑推理，结果回来叠加。
     */
    private fun processFrame(bitmap: Bitmap, rotationDegrees: Int, source: ImageSource) {
        if (source is CameraSource) {
            val frame = Frame(bitmap, rotationDegrees)
            val result = ToolRegistry.activeTool.value?.process(frame)
            runOnUiThread { renderResult(result, bitmap, rotationDegrees) }
        } else {
            runOnUiThread { binding.ivNetwork.setImageBitmap(bitmap) }

            inferenceExecutor.execute {
                val frame = Frame(bitmap, rotationDegrees)
                val result = ToolRegistry.activeTool.value?.process(frame)
                runOnUiThread { renderResult(result, bitmap, rotationDegrees) }
            }
        }
    }

    /** 渲染检测结果到 UI（必须在主线程调用） */
    private fun renderResult(result: ToolResult?, bitmap: Bitmap, rotationDegrees: Int) {
        when (result) {
            is ToolResult.Detections -> {
                val items = result.items
                binding.tvDetections.text = if (items.isEmpty()) {
                    "未检测到目标"
                } else {
                    items.take(3).joinToString("\n") {
                        "${it.label}: ${(it.score * 100).toInt()}%"
                    }
                }

                val previewW = binding.overlayView.width
                val previewH = binding.overlayView.height
                if (previewW > 0 && previewH > 0) {
                    binding.overlayView.setTransformations(
                        MODEL_INPUT_SIZE, previewW, previewH, rotationDegrees
                    )
                    binding.overlayView.updateDetections(items)
                }
            }
            else -> {
                binding.tvDetections.text = "处理中..."
                binding.overlayView.updateDetections(emptyList())
            }
        }
    }

    // ==================== 权限与生命周期 ====================

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                switchToSource(cameraSource)
            } else {
                Toast.makeText(this, "相机权限未授予", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
<<<<<<< HEAD
        destroyed = true
        currentSource?.stop()
        currentSource = null
        scope.cancel()
        cameraExecutor.shutdownNow()
        inferenceExecutor.shutdownNow()
        ToolRegistry.releaseAll()
        ModelRegistry.releaseAll()
        super.onDestroy()
=======
        super.onDestroy()
        currentSource?.stop()
        cameraExecutor.shutdown()
        inferenceExecutor.shutdown()
        ToolRegistry.releaseAll()
        ModelRegistry.releaseAll()
        scope.cancel()
        closeUdpSocket()
        udpReceiveThread?.interrupt()
>>>>>>> ff19ed6f514731b631f20d3ab0e9b1c5ed599537
    }
}