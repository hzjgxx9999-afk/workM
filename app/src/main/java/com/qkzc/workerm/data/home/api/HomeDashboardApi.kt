package com.qkzc.workerm.data.home.api

import com.qkzc.workerm.data.home.model.ApiEnvelope
import com.qkzc.workerm.data.home.model.HomeDashboardRequest
import com.qkzc.workerm.data.home.model.HomeDashboardResponse

interface HomeDashboardApi {
    suspend fun getDashboard(request: HomeDashboardRequest): ApiEnvelope<HomeDashboardResponse>
}
