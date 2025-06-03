package com.neweyes

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.neweyes.databinding.ActivityVoiceBinding
import androidx.activity.viewModels
import com.neweyes.camera.CameraViewModel
import com.neweyes.voice.VoiceViewModel

class VoiceActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVoiceBinding

    private val cameraViewModel: CameraViewModel by viewModels()
    private val viewModel: VoiceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraViewModel.initCamera(binding.previewView, this)

        viewModel.voiceText.observe(this) { text ->
            binding.tvDestino.text = text
        }

        viewModel.error.observe(this) { errorMsg ->
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
        }

        binding.btnMic.setOnClickListener {
            viewModel.startListening()
        }

    }
}
