package com.neweyes.chat.groq

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface GroqApi {
    @Headers(
        "Content-Type: application/json",
        "Authorization: Bearer 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"
    )
    @POST("openai/v1/chat/completions")
    fun getChatCompletion(@Body request: ChatRequest): Call<ChatResponse>
}