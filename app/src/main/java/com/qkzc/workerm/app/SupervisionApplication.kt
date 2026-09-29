package com.qkzc.workerm.app

import android.app.Application
import com.qkzc.workerm.data.network.ApiClient

class SupervisionApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.initialize(this)
    }
}
