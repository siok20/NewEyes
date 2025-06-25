package com.neweyes.camera.service

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*

interface AnalyzeApi {
    @Multipart
    @POST("/image-analyze-gemini/")
    fun sendImage(
        @Part file: MultipartBody.Part
    ): Call<ImageAnalyzeResponse>
}

