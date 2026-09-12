package com.college.attendance.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // 192.168.0.104 connects physical devices on the same Wi-Fi network as your Mac
    // 10.0.2.2 is also supported for standard Android Studio Emulator
    private var baseUrl = "http://192.168.0.104:3000/api/"
    private var authToken: String? = null

    fun setBaseUrl(url: String) {
        baseUrl = if (url.endsWith("/")) url else "$url/"
        retrofitInstance = null
    }

    fun setAuthToken(token: String?) {
        authToken = token
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            authToken?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }
            chain.proceed(requestBuilder.build())
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private var retrofitInstance: Retrofit? = null

    val apiService: AttendanceApiService
        get() {
            if (retrofitInstance == null) {
                retrofitInstance = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return retrofitInstance!!.create(AttendanceApiService::class.java)
        }
}
