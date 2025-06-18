package com.neweyes.chat

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val retrofit = Retrofit.Builder()
    .baseUrl("https://api.groq.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val groqApi = retrofit.create(GroqApi::class.java)