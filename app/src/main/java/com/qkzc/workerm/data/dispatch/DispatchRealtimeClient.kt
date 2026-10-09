package com.qkzc.workerm.data.dispatch

import android.os.Handler
import android.os.Looper
import com.qkzc.workerm.BuildConfig
import com.qkzc.workerm.data.network.ApiClient
import com.qkzc.workerm.data.network.bearerToken
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response

class DispatchRealtimeClient(private val onChanged: () -> Unit) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val client = ApiClient.okHttpClient.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    @Volatile private var started = false
    private var token: String = ""
    private var call: Call? = null

    fun start(accessToken: String) {
        token = accessToken
        if (started || token.isBlank()) return
        started = true
        connect()
    }

    fun stop() {
        started = false
        mainHandler.removeCallbacksAndMessages(this)
        call?.cancel()
        call = null
    }

    private fun connect() {
        if (!started || call != null) return
        val request = Request.Builder()
            .url(BuildConfig.SUPERVISOR_BASE_URL.trimEnd('/') + "/app/dispatch/events")
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .header("Authorization", bearerToken(token))
            .build()
        call = client.newCall(request).also { current ->
            current.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) = reconnect(call)

                override fun onResponse(call: Call, response: Response) {
                    try {
                        response.use {
                            if (response.isSuccessful) {
                                val source = response.body.source()
                                var eventName: String? = null
                                while (started && !source.exhausted()) {
                                    val line = source.readUtf8Line() ?: break
                                    when {
                                        line.startsWith("event:") -> eventName = line.substringAfter(':').trim()
                                        line.isBlank() -> {
                                            if (eventName == "dispatch.changed") mainHandler.post(onChanged)
                                            eventName = null
                                        }
                                    }
                                }
                            }
                        }
                    } catch (_: IOException) {
                        // The next connection plus REST polling recovers missed events.
                    } finally {
                        reconnect(call)
                    }
                }
            })
        }
    }

    private fun reconnect(completedCall: Call) {
        if (call === completedCall) call = null
        if (started) mainHandler.postAtTime({ connect() }, this, System.currentTimeMillis() + 3_000L)
    }
}
