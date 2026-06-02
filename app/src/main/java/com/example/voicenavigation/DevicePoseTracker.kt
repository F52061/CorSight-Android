package com.example.voicenavigation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.abs

data class DevicePoseSnapshot(
    val pitchDegrees: Float,
    val rollDegrees: Float,
    val azimuthDegrees: Float,
    val updatedAtMillis: Long,
    val source: String
) {
    val isRecent: Boolean
        get() = SystemClock.elapsedRealtime() - updatedAtMillis <= RECENT_THRESHOLD_MS

    companion object {
        private const val RECENT_THRESHOLD_MS = 1_500L

        fun unavailable(): DevicePoseSnapshot {
            return DevicePoseSnapshot(
                pitchDegrees = 0f,
                rollDegrees = 0f,
                azimuthDegrees = 0f,
                updatedAtMillis = 0L,
                source = "unavailable"
            )
        }
    }
}

class DevicePoseTracker(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private val accelValues = FloatArray(3)
    private val magnetValues = FloatArray(3)

    @Volatile
    private var latestSnapshot = DevicePoseSnapshot.unavailable()

    fun start() {
        rotationVector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        if (rotationVector == null) {
            accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun snapshot(): DevicePoseSnapshot = latestSnapshot

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> updateFromRotationVector(event.values)
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelValues, 0, accelValues.size)
                updateFromAccelMag()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetValues, 0, magnetValues.size)
                updateFromAccelMag()
            }
            Sensor.TYPE_GYROSCOPE -> {
                // Placeholder: pure gyro integration needs drift correction.
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun updateFromRotationVector(values: FloatArray) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
        updateOrientation("rotation_vector")
    }

    private fun updateFromAccelMag() {
        if (isZero(accelValues) || isZero(magnetValues)) return
        if (!SensorManager.getRotationMatrix(rotationMatrix, null, accelValues, magnetValues)) return
        updateOrientation("accel_mag")
    }

    private fun updateOrientation(source: String) {
        SensorManager.getOrientation(rotationMatrix, orientation)
        latestSnapshot = DevicePoseSnapshot(
            azimuthDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat(),
            pitchDegrees = Math.toDegrees(orientation[1].toDouble()).toFloat(),
            rollDegrees = Math.toDegrees(orientation[2].toDouble()).toFloat(),
            updatedAtMillis = SystemClock.elapsedRealtime(),
            source = source
        )
    }

    private fun isZero(values: FloatArray): Boolean {
        return values.all { abs(it) < 0.0001f }
    }
}
