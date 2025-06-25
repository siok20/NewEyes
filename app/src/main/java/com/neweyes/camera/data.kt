package com.neweyes.camera

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.Exclude

data class Posiciones(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var description: String = "",
    var status: Boolean = false
)


{

    constructor() : this(0.0, 0.0, "", false)

    @get:Exclude
    val posicion: LatLng
        get() = LatLng(latitude, longitude)

    fun setLatLng(latLng: LatLng) {
        this.latitude = latLng.latitude
        this.longitude = latLng.longitude
    }
}