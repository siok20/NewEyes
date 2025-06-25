package com.neweyes

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.neweyes.camera.CameraViewModel
import com.neweyes.camera.Posiciones
import com.neweyes.databinding.ActivityCameraBinding
import com.neweyes.voice.TextToSpeechHelper
import com.neweyes.voice.VoiceViewModel

class CameraActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 1002
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1003
    }

    private lateinit var binding: ActivityCameraBinding
    private val cameraViewModel: CameraViewModel by viewModels()
    private val voiceViewModel: VoiceViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var map: GoogleMap

    private lateinit var ttsHelper: TextToSpeechHelper

    private lateinit var database: DatabaseReference
    private val firebaseLocations = mutableListOf<Posiciones>()
    private lateinit var locationsAdapter: ArrayAdapter<String>

    private val localPointsLocations = mutableListOf<Posiciones>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ttsHelper = TextToSpeechHelper(this)
        database = FirebaseDatabase.getInstance().reference.child("points")
        locationsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 1) Verificar permiso de cámara antes de inicializar
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            cameraViewModel.initCamera(binding.previewView, this)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }

        // 2) Observadores para reconocimiento de voz
        voiceViewModel.voiceText.observe(this) { text ->
            binding.tvDestino.text = text
        }
        voiceViewModel.error.observe(this) { errorMsg ->
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
        }

        // 3) Botón micrófono: verifica permiso de audio
        binding.btnMic.setOnClickListener {
            Toast.makeText(this, "Toca en el mapa para seleccionar una ubicación y añadirla.", Toast.LENGTH_LONG).show()
            map.setOnMapClickListener { latLng ->
                // Cuando el usuario toca el mapa, le pedimos un nombre para la ubicación
                showAddLocationDialog(latLng)
                map.setOnMapClickListener(null) // Remover el listener después de seleccionar una ubicación
            }
        }

        // 4) Cargar Google Maps
        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(binding.mapContainer.id, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadInitialData()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        checkLocationPermissionAndEnable()
    }
    private fun loadInitialData() {

        // Parte 1: Cargar datos en la lista local para comenzar.
        // Estos datos se usarán si Firebase aún no ha cargado.
        // Iniciar la lectura de ubicaciones desde Firebase
        readLocationsFromFirebase()
    }
    private fun showAddLocationDialog(latLng: LatLng) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Añadir Nueva Ubicación")

        val input = EditText(this)
        input.hint = "Descripción rápida del evento"
        ttsHelper.speak("Descripción rápida del evento")
        builder.setView(input)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val locationName = input.text.toString().trim()
            if (locationName.isNotEmpty()) {
                val newLocation = Posiciones(
                    latitude = latLng.latitude,
                    longitude = latLng.longitude,
                    description = locationName,
                    status = true
                )
                writeNewLocationToFirebase(newLocation)
            } else {
                Toast.makeText(this, "El nombre de la ubicación no puede estar vacío.", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }
        builder.show()
    }

    // Función para escribir una nueva ubicación en Firebase
    private fun writeNewLocationToFirebase(posicion: Posiciones) {
        val newLocationRef = database.push() // Genera una nueva clave única para la ubicación
        newLocationRef.setValue(posicion)
            .addOnSuccessListener {
                Toast.makeText(this, "Ubicación '${posicion.description}' añadida a Firebase.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al añadir ubicación: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("Firebase", "Error al escribir en Firebase: ${e.message}")
            }
    }

    // Función para leer ubicaciones desde Firebase
    private fun readLocationsFromFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
            override fun onDataChange(snapshot: DataSnapshot) {
                firebaseLocations.clear() // Limpiar la lista existente
                locationsAdapter.clear() // Limpiar el adaptador del ListView

                for (childSnapshot in snapshot.children) {
                    val pos = childSnapshot.getValue(Posiciones::class.java)
                    pos?.let {
                        firebaseLocations.add(it)
                        locationsAdapter.add(it.description) // Añadir la dirección al adaptador del ListView
                    }
                }
                locationsAdapter.notifyDataSetChanged() // Notificar al adaptador que los datos han cambiado
                createMarkers() // Volver a dibujar los marcadores con los datos actualizados de Firebase
                // Este bloque es un fallback, si Firebase está vacío, usa la lista local.
                // Se ejecuta después de intentar leer de Firebase.
                if (firebaseLocations.isEmpty() ) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val latLng = LatLng(location.latitude, location.longitude)
                            val cameraPosition = CameraPosition.Builder()
                                .target(latLng)
                                .zoom(17f)
                                .build()
                            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                        }
                    }
                }
            }

            @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al leer de Firebase: ${error.message}")
                Toast.makeText(this@CameraActivity, "Error al cargar ubicaciones de Firebase.", Toast.LENGTH_SHORT).show()
                // En caso de error o sin datos en Firebase, usa la lista local como fallback para la Parte 1
                if (firebaseLocations.isEmpty() && localPointsLocations.isNotEmpty()) {
                    firebaseLocations.addAll(localPointsLocations)
                    localPointsLocations.forEach { locationsAdapter.add(it.description) }
                    locationsAdapter.notifyDataSetChanged()
                    createMarkers()
                }
            }
        })
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun createMarkers() {
        // Limpiar marcadores existentes para evitar duplicados al actualizar
        map.clear()

        // Añadir marcador para "Yo"
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                val cameraPosition = CameraPosition.Builder()
                    .target(latLng)
                    .zoom(17f)
                    .build()
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
        }

        // Añadir marcadores para las ubicaciones de Estudiante (usará la lista de Firebase)
        val markers = firebaseLocations.forEach { pos ->
            map.addMarker(MarkerOptions().position(pos.posicion).title(pos.description))
        }

        map.setOnMarkerClickListener { marker ->
            // Aquí decides qué hacer cuando hagan clic en un marcador
            val title = marker.title
            val position = marker.position
            // Por ejemplo, mostrar un Toast
            ttsHelper.speak(title.toString())
            Toast.makeText(this, "Marcador clickeado: $title en $position", Toast.LENGTH_SHORT).show()

            true
        }

    }

    private fun checkLocationPermissionAndEnable() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            enableLocationFeatures()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableLocationFeatures() {
        map.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                val cameraPosition = CameraPosition.Builder()
                    .target(latLng)
                    .zoom(17f)
                    .build()
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
            }
        }
    }

    // Maneja la respuesta de TODOS los permisos que pedimos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            // ----------------------------- Cámara -----------------------------
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Se concedió el permiso de cámara: inicializamos CameraX
                    cameraViewModel.initCamera(binding.previewView, this)
                } else {
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
            }

            // --------------------------- Micrófono ----------------------------
            RECORD_AUDIO_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Se concedió RECORD_AUDIO: iniciamos reconocimiento de voz
                    voiceViewModel.startListening()
                } else {
                    Toast.makeText(this, "Permiso de micrófono denegado", Toast.LENGTH_SHORT).show()
                }
            }

            // -------------------------- Ubicación -----------------------------
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableLocationFeatures()
                } else {
                    Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
