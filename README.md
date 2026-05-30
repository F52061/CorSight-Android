# 瞳心引航 Android

瞳心引航是一个面向视障辅助出行的 Android 客户端，集成地图定位、目的地搜索、行前预览、语音交互、视觉检测与避障告警能力。

## 项目结构

```text
CorSight-Android/
├── app/                         # Android 主应用
│   ├── src/main/java/com/example/voicenavigation/
│   │   ├── MainActivity.java     # 主页面、地图、搜索、设置、语音入口
│   │   ├── VisionTestActivity.kt # 视觉检测页面，本地/云端检测入口
│   │   ├── YoloModelConfig.kt    # 本地 YOLO 模型配置
│   │   ├── ImageQualityAnalyzer.kt
│   │   ├── ObstacleRiskAnalyzer.kt
│   │   ├── ObstacleAlertTracker.kt
│   │   ├── data/                 # Room 本地导航历史
│   │   ├── navigation/           # 高德定位、路线规划、导航逻辑
│   │   ├── network/              # 后端 HTTP 接口
│   │   ├── stt/                  # 百度语音识别/TTS 封装
│   │   └── collection/           # 数据采集页面
│   ├── src/main/assets/models/   # 本地模型与标签文件
│   └── src/main/res/             # 布局、资源、主题、字符串
├── inference/                    # ONNX 推理模块
├── vision/                       # 视觉工具注册与检测工具封装
├── docs/                         # 后端接口文档
├── build.gradle
├── settings.gradle
└── local.properties              # 本地 SDK 与私有 API Key 配置，不应提交
```

## 当前功能

- 主页面显示“瞳心引航”，提供“行前预览”和“按住说话”等核心入口。
- 支持高德地图定位、POI 搜索、地图选点、步行路线规划和导航提示。
- 支持百度语音识别与百度 TTS 语音播报。
- 支持导航历史记录本地保存。
- 支持设置页配置后端服务地址，不再把临时后端地址硬编码到代码中。
- 支持视觉检测页面使用本地 YOLO ONNX 模型识别摄像头画面。
- 支持将摄像头清晰帧上传到云端检测服务。
- 支持图像清晰度过滤，模糊帧不会进入本地或云端检测流程。
- 支持基于画面中心风险区域的避障告警等级计算。
- 支持同一目标连续多帧去重播报，仅在首次达到低级告警或告警等级提升时播报。

## 技术栈

| 技术 | 用途 |
|:---|:---|
| Android Gradle Plugin 8.x | Android 构建 |
| Kotlin / Java | 应用主要开发语言 |
| 高德 3D Map SDK | 地图显示与定位 |
| 高德 Search SDK | POI 搜索与路线规划 |
| 百度语音 SDK | 语音识别 |
| 百度语音合成 REST API | TTS 播报 |
| CameraX | 摄像头图像采集 |
| ONNX Runtime | 本地 YOLO 模型推理 |
| OkHttp | 后端、云端检测、TTS 网络请求 |
| Room | 本地历史记录存储 |
| minSdk 24 / targetSdk 34 | Android 7.0+ |

## 快速开始

### 环境要求

- Android Studio Hedgehog 或更新版本
- JDK 17+
- Android SDK 34
- Android 7.0+ 真机，推荐使用真机测试定位和摄像头

### 配置高德 Key

高德 Key 通过 `local.properties` 注入，避免写死在源码中：

```properties
amap.api.key=你的高德 Android Key
```

相关代码位置：

- `local.properties`：填写 `amap.api.key`
- `app/build.gradle`：读取 `amap.api.key`，生成 `BuildConfig.AMAP_API_KEY` 和 Manifest placeholder
- `app/src/main/AndroidManifest.xml`：通过 `${AMAP_API_KEY}` 配置高德 meta-data
- `app/src/main/java/com/example/voicenavigation/MainActivity.java`：运行时调用高德 SDK `setApiKey`
- `app/src/main/java/com/example/voicenavigation/collection/DataCollectionActivity.kt`：数据采集页运行时调用高德 SDK `setApiKey`

高德控制台需要确认：

- Key 类型必须是 Android 应用 Key。
- 包名必须匹配 `com.example.voicenavigation`。
- SHA1 必须匹配当前安装包签名，debug 包绑定 debug SHA1，release 包绑定 release SHA1。
- 定位、地图、搜索、路线规划相关服务需要开通。

如果点击定位仍提示 `KEY错误` 或搜索提示 `服务错误：1008`，优先检查高德控制台的包名、SHA1、Key 类型和服务权限，而不是后端服务。

### 配置百度语音

百度语音配置在：

- `app/src/main/res/values/strings.xml`
- `app/src/main/AndroidManifest.xml`

字段包括：

- `baidu_speech_app_id`
- `baidu_speech_api_key`
- `baidu_speech_secret_key`

注意：这些属于敏感配置，正式项目建议改为本地配置或构建注入，不建议提交真实密钥。

### 配置后端服务地址

后端服务由单独程序提供，App 通过设置页填写地址。

设置页包含两个地址：

- 地图/大模型预览服务地址：用于行前预览与数据采集上传。
- 云端检测服务地址：用于视觉检测页面上传清晰帧。

相关代码位置：

- `app/src/main/res/layout/page_settings.xml`：设置页输入框与保存按钮。
- `app/src/main/java/com/example/voicenavigation/AppConfig.java`：SharedPreferences key 和 URL 归一化。
- `app/src/main/java/com/example/voicenavigation/MainActivity.java`：读取、保存设置页地址。
- `app/src/main/java/com/example/voicenavigation/network/TripPreviewService.java`：调用 `/api/navigation/preview`。
- `app/src/main/java/com/example/voicenavigation/VisionTestActivity.kt`：调用 `/api/detect`。
- `app/src/main/java/com/example/voicenavigation/collection/UploadService.kt`：调用 `/api/upload/sampling_point`。

地址示例：

```text
http://192.168.1.100:8000
https://your-domain.example.com
```

真机连接本机后端时，手机和电脑需要在同一局域网，并使用电脑局域网 IP。模拟器访问电脑本机服务时通常使用 `http://10.0.2.2:端口`。

## 视觉检测与避障逻辑

### 本地模型配置

本地 YOLO 模型配置在 `app/src/main/java/com/example/voicenavigation/YoloModelConfig.kt`：

| 参数 | 默认值 | 说明 |
|:---|:---|:---|
| `MODEL_ASSET_PATH` | `models/yolov8.onnx` | ONNX 模型路径 |
| `LABEL_ASSET_PATH` | `models/coco80.txt` | 标签文件路径 |
| `INPUT_SIZE` | `640` | 模型输入尺寸 |
| `confidenceThreshold` | `0.5f` | 检测置信度阈值 |
| `nmsThreshold` | `0.45f` | NMS 阈值 |

### 图像清晰度过滤

清晰度检测在 `app/src/main/java/com/example/voicenavigation/ImageQualityAnalyzer.kt`：

| 参数 | 默认值 | 说明 |
|:---|:---|:---|
| `DEFAULT_MIN_SHARPNESS` | `95.0` | 清晰度低于该值时认为是模糊帧 |

本地检测和云端检测都会先进行清晰度判断，只有清晰帧才进入模型检测或上传流程。

### 避障风险区域

风险区域计算在 `app/src/main/java/com/example/voicenavigation/ObstacleRiskAnalyzer.kt`：

- 以图像中心为基准绘制矩形风险区域。
- 风险区域面积占整张图像 `30%`。
- 当前宽度比例为 `60%`，高度比例由面积比例推导。
- 目标框与中心风险区域重叠后，根据重叠占比计算告警等级。

告警阈值：

| 重叠占比 | 告警等级 | 语音内容 |
|:---|:---|:---|
| `< 30%` | 不告警 | 不播报 |
| `>= 30%` | 低级告警 | `请注意，不远处有{label}` |
| `>= 50%` | 中级告警 | `请注意，正在接近{label}` |
| `>= 70%` | 高级告警 | `请注意，已靠近{label}` |

### 重复播报抑制

重复播报逻辑在 `app/src/main/java/com/example/voicenavigation/ObstacleAlertTracker.kt`：

- 第一次检测到目标时，如果未达到低级告警，不播报。
- 第一次达到低级告警时，记录该目标并播报一次。
- 连续多帧检测到同一目标时，只累计计数，不重复播报。
- 只有同一目标告警等级提升时，才再次播报。
- 当某个目标从画面中消失时，清空该目标计数和状态。

该逻辑用于避免同一物体在连续摄像头帧中被反复播报。

## 后端接口

### 行前预览

- 路径：`POST /api/navigation/preview`
- 调用位置：`app/src/main/java/com/example/voicenavigation/network/TripPreviewService.java`
- 请求参数使用经纬度字符串，格式为 `longitude,latitude`。

### 云端视觉检测

- 路径：`POST /api/detect`
- 调用位置：`app/src/main/java/com/example/voicenavigation/VisionTestActivity.kt`
- 请求类型：`multipart/form-data`
- 表单字段：
  - `image`：JPEG 图像
  - `rotation`：图像旋转角度
  - `sharpness`：清晰度评分

### 数据采集上传

- 路径：`POST /api/upload/sampling_point`
- 调用位置：`app/src/main/java/com/example/voicenavigation/collection/UploadService.kt`

## 常见问题

### 定位提示 KEY 错误

优先检查高德控制台配置：

- `local.properties` 是否填写了 `amap.api.key`。
- 高德 Key 是否为 Android Key。
- 高德 Key 是否绑定包名 `com.example.voicenavigation`。
- 高德 Key 是否绑定当前安装包 SHA1。
- 高德定位、地图、搜索、路线规划服务是否已开通。

### 搜索提示服务错误 1008

通常是高德 Key 权限或绑定信息不正确。检查项同 `KEY错误`，尤其是 Search SDK/路线规划服务权限。

### 设置后端地址后仍无法访问

检查：

- 手机和后端服务是否在同一网络。
- 后端是否监听 `0.0.0.0`，而不是只监听 `127.0.0.1`。
- App 中填写的是基础地址，不要带接口路径，例如填写 `http://192.168.1.100:8000`，不要填写 `/api/detect`。
- Android 9+ 访问 HTTP 需要允许明文流量，当前项目已在 Manifest 中开启。

### 云端检测没有结果

检查：

- 设置页是否填写“云端检测服务地址”。
- 摄像头画面是否过暗或模糊，模糊帧会被清晰度过滤拦截。
- 后端 `/api/detect` 是否支持 multipart 上传。
- 返回的检测框坐标是否与 App 解析格式一致。

## 构建

```powershell
.\gradlew.bat assembleDebug
```

构建成功后，debug APK 通常位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 调试日志

Logcat 可按以下 tag 过滤：

- `MainActivity`：主页面、设置、地图、语音入口。
- `NavigationManager`：定位、路线规划、导航状态。
- `TripPreviewService`：行前预览请求。
- `VisionTestActivity`：视觉检测、本地/云端检测、清晰度过滤。
- `BaiduSpeechManager`：百度语音识别。
- `BaiduTtsManager`：百度语音合成。

## 维护建议

- 不要把真实 API Key 提交到仓库。
- `local.properties` 只用于本机配置。
- 视觉阈值调整时优先修改 `YoloModelConfig.kt`、`ImageQualityAnalyzer.kt` 和 `ObstacleRiskAnalyzer.kt`。
- 后端地址优先通过 App 设置页配置，不建议重新硬编码。
