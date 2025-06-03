package com.neweyes.camera

import android.app.Application
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import com.neweyes.voice.TextToSpeechHelper

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private var cameraManager: CameraManager? = null
    private val ttsHelper = TextToSpeechHelper(application.applicationContext)

    fun initCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        cameraManager = CameraManager(getApplication(), lifecycleOwner, previewView, ttsHelper)
        cameraManager?.startCamera()
    }

    override fun onCleared() {
        super.onCleared()
        cameraManager?.stopCamera()
        ttsHelper.shutdown()
    }
}

