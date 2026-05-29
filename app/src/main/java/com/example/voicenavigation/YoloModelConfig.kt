package com.example.voicenavigation

object YoloModelConfig {
    const val MODEL_ASSET_PATH = "models/yolov8n.onnx"
    const val LABEL_ASSET_PATH = "models/coco80.txt"
    const val INPUT_SIZE = 640

    @JvmField
    var confidenceThreshold: Float = 0.5f

    @JvmField
    var nmsThreshold: Float = 0.45f
}
