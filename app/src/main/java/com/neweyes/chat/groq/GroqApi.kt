package com.neweyes.chat.groq

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface GroqApi {
    @Headers(
        "Content-Type: application/json",
        "Authorization: Bearer YOUR_GROQ_API_KEY"
    )
    @POST("openai/v1/chat/completions")
    fun getChatCompletion(@Body request: ChatRequest): Call<ChatResponse>
}