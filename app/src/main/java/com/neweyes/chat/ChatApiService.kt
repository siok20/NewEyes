package com.neweyes.chat

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