package com.qkzc.workerm.data.network

import com.qkzc.workerm.BuildConfig
import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.qkzc.workerm.data.dispatch.DispatchApi

object ApiClient {

    @Volatile
    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.BASIC
        }
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        .addInterceptor(
            SessionReplacementInterceptor(
                {
                    requireNotNull(applicationContext) {
                        "ApiClient must be initialized from Application.onCreate"
                    }
                },
            ),
        )
        .addInterceptor(loggingInterceptor)
        .build()
    }

    val supervisorApi: SupervisorApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.SUPERVISOR_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupervisorApi::class.java)
    }

    val dispatchApi: DispatchApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.SUPERVISOR_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DispatchApi::class.java)
    }
}
