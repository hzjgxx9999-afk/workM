package com.qkzc.workerm.ui.login

private val MainlandPhoneRegex = Regex("^1[3-9]\\d{9}$")

fun validateSupervisorLoginInput(
    phone: String,
    password: String,
    agreementAccepted: Boolean,
): String? {
    val trimmedPhone = phone.trim()
    return when {
        trimmedPhone.isEmpty() -> "请输入手机号"
        !MainlandPhoneRegex.matches(trimmedPhone) -> "请输入正确的手机号"
        password.isEmpty() -> "请输入密码"
        !agreementAccepted -> "请先阅读并勾选同意《服务协议》和《隐私政策》"
        else -> null
    }
}
