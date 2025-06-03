package com.neweyes

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.neweyes.databinding.ActivityVoiceBinding
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.neweyes.camera.CameraViewModel
import com.neweyes.voice.VoiceViewModel
import android.Manifest.permission.ACCESS_FINE_LOCATION
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng


class VoiceActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityVoiceBinding

    private val cameraViewModel: CameraViewModel by viewModels()
    private val viewModel: VoiceViewModel by viewModels()
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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

        // Iniciar mapa de Google
        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(binding.mapContainer.id, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        enableMyLocation()

        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    val cameraPosition = CameraPosition.Builder()
                        .target(currentLatLng)
                        .zoom(17f)
                        .build()
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                }
            }
        } else {
            // Opcional: manejar caso sin permiso
            Toast.makeText(this, "Permiso de ubicación no concedido", Toast.LENGTH_SHORT).show()
        }
    }



    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this,ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        } else {
            requestPermissions(arrayOf(ACCESS_FINE_LOCATION), 1001)
        }
    }
}
