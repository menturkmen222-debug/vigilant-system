package com.whiteboard.animator.data.network

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Interface for AI Service compatible with OpenAI API structure.
 */
interface AiService {

    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun createCompletion(@Body request: CompletionRequest): CompletionResponse

    // Request Models
    data class CompletionRequest(
        val model: String = "gpt-3.5-turbo",
        val messages: List<Message>,
        val temperature: Double = 0.7
    )

    data class Message(
        val role: String,
        val content: String
    )

    // Response Models
    data class CompletionResponse(
        val id: String,
        val choices: List<Choice>
    )

    data class Choice(
        val index: Int,
        val message: Message
    )
}
