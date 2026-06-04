package com.qkzc.workerm.data.network

class ApiException(message: String) : RuntimeException(message)

fun bearerToken(rawToken: String): String = "Bearer $rawToken"

fun requireSuccess(code: Int, message: String?) {
    if (code != 200) {
        throw ApiException(message?.takeIf { it.isNotBlank() } ?: "接口请求失败")
    }
}
