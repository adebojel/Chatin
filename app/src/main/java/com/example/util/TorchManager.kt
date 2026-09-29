package com.example.util

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TorchManager(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private var cameraIdWithFlash: String? = null

    init {
        findCameraWithFlash()
    }

    private fun findCameraWithFlash() {
        try {
            cameraManager?.cameraIdList?.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraIdWithFlash = id
                    return
                }
            }
            if (cameraIdWithFlash == null && cameraManager?.cameraIdList?.isNotEmpty() == true) {
                cameraIdWithFlash = cameraManager.cameraIdList[0]
            }
        } catch (e: Exception) {
            Log.e("TorchManager", "Error querying cameras: ${e.message}")
        }
    }

    fun toggleTorch(): Boolean {
        val newState = !_isTorchOn.value
        return setTorch(newState)
    }

    fun setTorch(enable: Boolean): Boolean {
        val camId = cameraIdWithFlash
        if (cameraManager == null || camId == null) {
            _isTorchOn.value = enable
            return false
        }
        return try {
            cameraManager.setTorchMode(camId, enable)
            _isTorchOn.value = enable
            true
        } catch (e: CameraAccessException) {
            Log.w("TorchManager", "Camera access exception toggling torch: ${e.message}")
            _isTorchOn.value = enable
            false
        } catch (e: Exception) {
            Log.e("TorchManager", "General exception toggling torch: ${e.message}")
            _isTorchOn.value = enable
            false
        }
    }
}
