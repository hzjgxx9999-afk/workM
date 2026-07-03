package com.qkzc.workerm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LoginPrimaryBlue,
    onPrimary = Color.White,
    background = LoginBackgroundTop,
    onBackground = LoginTitle,
    surface = Color.White,
    onSurface = LoginTitle,
)

@Composable
fun WorkerMTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = WorkerMTypography,
        content = content,
    )
}
