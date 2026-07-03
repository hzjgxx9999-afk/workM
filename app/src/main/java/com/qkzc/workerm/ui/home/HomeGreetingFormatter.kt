package com.qkzc.workerm.ui.home

fun formatHomeGreeting(hourOfDay: Int, realName: String): String {
    val greeting = when (hourOfDay) {
        in 5..11 -> "上午好"
        in 12..17 -> "下午好"
        else -> "晚上好"
    }
    val displayName = realName.trim().ifBlank { "项目经理" }
    return "$greeting，$displayName"
}
