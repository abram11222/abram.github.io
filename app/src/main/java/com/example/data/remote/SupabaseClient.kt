package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit/Moshi client for Supabase.
 * Uses Moshi reflection (KotlinJsonAdapterFactory) so the existing Room entity
 * data classes serialize to JSON with their camelCase property names — which
 * match the quoted camelCase columns in the Supabase schema — with no entity
 * annotation changes needed.
 */
object SupabaseClient {

    val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val okHttp: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            // BASIC only — never log full bodies (tokens / user data)
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val api: SupabaseApi by lazy {
        Retrofit.Builder()
            .baseUrl(SupabaseConfig.BASE_URL)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(SupabaseApi::class.java)
    }
}
