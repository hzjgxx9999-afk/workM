package com.qkzc.workerm.ui.login

private const val ScrollFallbackHeightDp = 700

fun shouldUseScrollableLoginLayout(availableHeightDp: Int): Boolean {
    return availableHeightDp < ScrollFallbackHeightDp
}
