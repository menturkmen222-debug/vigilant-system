package com.whiteboard.animator.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.whiteboard.animator.data.network.AiService
import com.whiteboard.animator.data.preferences.PreferenceManager
import com.whiteboard.animator.data.common.ApiKeyManager
import com.whiteboard.animator.data.model.ApiProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api.openai.com/" // Default, can be overridden if needed for other providers

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(apiKeyManager: ApiKeyManager): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val requestBuilder = request.newBuilder()
            
            // Determine provider based on URL or header (default to OPENAI for now)
            // Ideally, the service interface would tag the request with the provider.
            // For V2, we assume OpenAI as primary.
            val provider = ApiProvider.OPENAI
            
            // Blocking fetch safe on network thread
            val apiKey = runBlocking {
                apiKeyManager.getWorkingKey(provider)
            }

            if (!apiKey.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            val response = chain.proceed(requestBuilder.build())
            
            // Handle 401/429 - rotate key
            if (response.code == 401 || response.code == 429) {
                if (!apiKey.isNullOrBlank()) {
                    runBlocking { apiKeyManager.markKeyAsFailed(apiKey) }
                    response.close() // Close failed response
                    
                    // Retry with new key
                    val newKey = runBlocking { apiKeyManager.getWorkingKey(provider) }
                    if (!newKey.isNullOrBlank() && newKey != apiKey) {
                        // Re-build request with new key
                         val retryRequest = request.newBuilder()
                            .header("Authorization", "Bearer $newKey")
                            .build()
                        return@Interceptor chain.proceed(retryRequest)
                    }
                }
            }
            
            response
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideAiService(okHttpClient: OkHttpClient, json: Json): AiService {
        val contentType = "application/json".toMediaType()
        
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(AiService::class.java)
    }
}
