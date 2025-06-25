package com.neweyes.camera.service

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val retrofit = Retrofit.Builder()
    .baseUrl("https://siok-support-groq-neweyes.hf.space\n") // Cambia a tu IP/puerto
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val analyzeApi = retrofit.create(AnalyzeApi::class.java)
