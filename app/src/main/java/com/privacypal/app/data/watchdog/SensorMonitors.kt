package com.privacypal.app.data.watchdog

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Handler
import android.os.Looper
import com.privacypal.app.data.apps.Clock
import com.privacypal.app.domain.watchdog.Sensor
import com.privacypal.app.domain.watchdog.SensorEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Reports when any app starts or stops recording audio. Android does not tell normal apps WHICH app
 * records, only that the set of active recordings changed. Needs no permission.
 */
interface MicMonitor {
    /** Cold flow: registers the system callback while collected. */
    val events: Flow<SensorEvent>
}

/**
 * Reports when a camera goes into use (unavailable) or is released (available), per camera id. Needs
 * no permission; PrivacyPal never opens a camera itself, so every signal is another app's.
 */
interface CameraMonitor {
    val events: Flow<SensorEvent>
}

class AudioManagerMicMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) : MicMonitor {

    override val events: Flow<SensorEvent> = callbackFlow {
        val audioManager = context.getSystemService(AudioManager::class.java)
        var recording = false
        fun update(configs: List<AudioRecordingConfiguration>) {
            val nowRecording = configs.isNotEmpty()
            if (nowRecording == recording) return
            recording = nowRecording
            val type = if (nowRecording) SensorEvent.Type.STARTED else SensorEvent.Type.STOPPED
            trySend(SensorEvent(Sensor.MICROPHONE, type, MIC_SOURCE, clock.now()))
        }
        val callback = object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) = update(configs)
        }
        audioManager.registerAudioRecordingCallback(callback, Handler(Looper.getMainLooper()))
        // A recording already running when we start watching counts from now.
        update(audioManager.activeRecordingConfigurations)
        awaitClose { audioManager.unregisterAudioRecordingCallback(callback) }
    }

    private companion object {
        const val MIC_SOURCE = "mic"
    }
}

class CameraManagerCameraMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) : CameraMonitor {

    override val events: Flow<SensorEvent> = callbackFlow {
        val cameraManager = context.getSystemService(CameraManager::class.java)
        val handler = Handler(Looper.getMainLooper())
        val inUse = mutableSetOf<String>()
        val torchOn = mutableSetOf<String>()
        fun send(type: SensorEvent.Type, cameraId: String) =
            trySend(SensorEvent(Sensor.CAMERA, type, cameraId, clock.now()))

        val availability = object : CameraManager.AvailabilityCallback() {
            override fun onCameraUnavailable(cameraId: String) {
                // The flashlight holds the camera on some phones; that is not someone using the camera.
                if (cameraId in torchOn || !inUse.add(cameraId)) return
                send(SensorEvent.Type.STARTED, cameraId)
            }

            override fun onCameraAvailable(cameraId: String) {
                if (inUse.remove(cameraId)) send(SensorEvent.Type.STOPPED, cameraId)
            }
        }
        val torch = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                if (!enabled) {
                    torchOn.remove(cameraId)
                    return
                }
                torchOn.add(cameraId)
                // The torch callback can arrive after "unavailable": undo that start.
                if (inUse.remove(cameraId)) send(SensorEvent.Type.CANCELLED, cameraId)
            }
        }
        cameraManager.registerTorchCallback(torch, handler)
        cameraManager.registerAvailabilityCallback(availability, handler)
        awaitClose {
            cameraManager.unregisterAvailabilityCallback(availability)
            cameraManager.unregisterTorchCallback(torch)
        }
    }
}
