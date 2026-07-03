package com.qkzc.workerm.ui.login

private val MainlandPhoneRegex = Regex("^1[3-9]\\d{9}$")

fun validateSupervisorLoginInput(phone: String, password: String): String? {
    val trimmedPhone = phone.trim()
    return when {
        trimmedPhone.isEmpty() -> "请输入手机号"
        !MainlandPhoneRegex.matches(trimmedPhone) -> "请输入正确的手机号"
        password.isEmpty() -> "请输入密码"
        else -> null
    }
}
