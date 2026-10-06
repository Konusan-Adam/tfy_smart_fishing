package com.example.service

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CameraFlashManager(private val context: Context) {

    private val tag = "CameraFlashManager"
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var cameraId: String? = null
    private var strobeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        findCameraWithFlash()
    }

    private fun findCameraWithFlash() {
        try {
            cameraManager?.let { cm ->
                for (id in cm.cameraIdList) {
                    val characteristics = cm.getCameraCharacteristics(id)
                    val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                    val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                    // Prefer back camera with flash
                    if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraId = id
                        break
                    } else if (hasFlash && cameraId == null) {
                        cameraId = id
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Camera access check failed: ${e.message}")
        }
    }

    /**
     * Gece avı flaşörü: Çadırı ve av yerini aydınlatacak yüksek hızlı flaşör döngüsü (Strobe)
     */
    fun startStrobe() {
        if (strobeJob?.isActive == true) return
        val targetCamId = cameraId ?: return

        strobeJob = scope.launch {
            var torchState = false
            try {
                while (isActive) {
                    torchState = !torchState
                    try {
                        cameraManager?.setTorchMode(targetCamId, torchState)
                    } catch (e: Exception) {
                        Log.e(tag, "Torch mode toggle failed: ${e.message}")
                        break
                    }
                    delay(90) // High-speed night strobe frequency
                }
            } finally {
                // Ensure torch is cleanly turned off upon cancellation
                try {
                    cameraManager?.setTorchMode(targetCamId, false)
                } catch (e: Exception) {
                    Log.e(tag, "Torch reset error: ${e.message}")
                }
            }
        }
    }

    /**
     * Flaşörü anında söndürür
     */
    fun stopStrobe() {
        strobeJob?.cancel()
        strobeJob = null
        val targetCamId = cameraId ?: return
        try {
            cameraManager?.setTorchMode(targetCamId, false)
        } catch (e: Exception) {
            Log.e(tag, "Torch stop error: ${e.message}")
        }
    }
}
