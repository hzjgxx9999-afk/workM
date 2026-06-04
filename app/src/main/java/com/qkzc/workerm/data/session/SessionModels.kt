package com.qkzc.workerm.data.session

data class LoginSession(
    val accessToken: String = "",
    val refreshToken: String = "",
    val userId: String = "",
    val userName: String = "",
    val mobile: String = "",
    val userType: String = "",
    val clientType: String = "",
    val roleName: String = "",
    val organizationName: String = "",
    val projectId: String = "",
    val projectName: String = "",
) {
    val isLoggedIn: Boolean
        get() = accessToken.isNotBlank()
}

data class LoginPayload(
    val mobile: String,
    val password: String,
)
