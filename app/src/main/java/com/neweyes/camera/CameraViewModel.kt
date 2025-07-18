package com.neweyes.camera

import android.app.Application
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import com.neweyes.vibration.VibrationManager
import com.neweyes.voice.TextToSpeechHelper

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private var cameraManager: CameraManager? = null
    private val ttsHelper = TextToSpeechHelper(application.applicationContext)
    private var vibrateHelper = VibrationManager(application.applicationContext)

    fun initCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        cameraManager = CameraManager(getApplication(), lifecycleOwner, previewView, ttsHelper, vibrateHelper)
        cameraManager?.startCamera()
    }

    fun clean(){
        cameraManager?.stopCamera()
        ttsHelper.shutdown()
    }

    override fun onCleared() {
        super.onCleared()
        clean()
    }
}

