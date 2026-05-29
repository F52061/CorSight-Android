package com.corsight.vision.tools

import android.content.Context
import com.corsight.inference.ModelRegistry
import com.corsight.inference.YoloV8OnnxEngine
import com.corsight.vision.Frame
import com.corsight.vision.ToolResult
import com.corsight.vision.VisionTool

class GenericDetectionTool : VisionTool {
    override val id: String = "generic_detection"
    override val displayName: String = "本地视觉避障检测"

    companion object {
        const val MODEL_ID = "yolov8"
    }

    override fun onActivate(context: Context) {
        if (ModelRegistry.getDetector(MODEL_ID) == null) {
            val configClass = Class.forName("com.example.voicenavigation.YoloModelConfig")
            val modelPath = configClass.getField("MODEL_ASSET_PATH").get(null) as String
            val labelPath = configClass.getField("LABEL_ASSET_PATH").get(null) as String
            val confidence = configClass.getField("confidenceThreshold").getFloat(null)
            val nms = configClass.getField("nmsThreshold").getFloat(null)
            val engine = YoloV8OnnxEngine(modelPath, labelPath, confidence, nms)
            ModelRegistry.register(MODEL_ID, engine)
            engine.load(context)
        }
    }

    override fun onDeactivate() {
        ModelRegistry.release(MODEL_ID)
    }

    override fun process(frame: Frame): ToolResult {
        val detector = ModelRegistry.getDetector(MODEL_ID) ?: return ToolResult.Nothing
        val detections = detector.detect(frame.bitmap, frame.rotationDegrees)
        return if (detections.isNotEmpty()) {
            ToolResult.Detections(detections)
        } else {
            ToolResult.Nothing
        }
    }
}
