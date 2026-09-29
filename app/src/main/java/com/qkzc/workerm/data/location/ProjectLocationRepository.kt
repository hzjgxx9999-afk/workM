package com.qkzc.workerm.data.location

import com.qkzc.workerm.data.network.ApiClient
import com.qkzc.workerm.data.network.bearerToken
import com.qkzc.workerm.data.network.requireSuccess

class ProjectLocationRepository {
    suspend fun geofence(token: String, projectId: Long): ProjectGeofence? {
        val response = ApiClient.supervisorApi.manageProjectGeofence(bearerToken(token), projectId)
        requireSuccess(response.code, response.msg)
        return response.data
    }

    suspend fun saveGeofence(token: String, projectId: Long, request: ProjectGeofenceSaveRequest): ProjectGeofence {
        val response = ApiClient.supervisorApi.saveManageProjectGeofence(bearerToken(token), projectId, request)
        requireSuccess(response.code, response.msg)
        return requireNotNull(response.data) { "围栏保存后未返回数据" }
    }

    suspend fun disableGeofence(token: String, projectId: Long) {
        val response = ApiClient.supervisorApi.disableManageProjectGeofence(bearerToken(token), projectId)
        requireSuccess(response.code, response.msg)
    }
}

data class ProjectGeofence(
    val projectId: Long = 0,
    val fenceType: String = "CIRCLE",
    val centerLongitude: Double? = null,
    val centerLatitude: Double? = null,
    val radius: Int? = null,
    val polygonPoints: String? = null,
    val status: String = "ENABLE",
)

data class ProjectGeofenceSaveRequest(
    val fenceType: String,
    val centerLongitude: Double? = null,
    val centerLatitude: Double? = null,
    val radius: Int? = null,
    val polygonPoints: List<LocationPoint>? = null,
)

data class LocationPoint(val longitude: Double, val latitude: Double)
