package com.neweyes.camera

import android.app.Application
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private var cameraManager: CameraManager? = null

    fun initCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        cameraManager = CameraManager(getApplication(), lifecycleOwner, previewView)
        cameraManager?.startCamera()
    }
}
