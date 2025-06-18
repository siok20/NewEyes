package com.neweyes.voice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class VoiceViewModel(application: Application) : AndroidViewModel(application),
    SpeechRecognizerHelper.Listener {

    private val voicePartialText = MutableLiveData<String>()
    val voiceText: LiveData<String> = voicePartialText

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val helper: SpeechRecognizerHelper =
        SpeechRecognizerHelper(application.applicationContext, this)

    fun startListening() {
        helper.startListening()
    }

    fun stopListening() {
        helper.stopListening()
    }

    override fun onPartialResult(text: String) {
        voicePartialText.postValue(text)
    }

    override fun onFinalResult(text: String) {
        voicePartialText.postValue(text)
    }

    override fun onError(errorMsg: String) {
        _error.postValue(errorMsg)
    }

    override fun onCleared() {
        super.onCleared()
        helper.destroy()
    }
}
