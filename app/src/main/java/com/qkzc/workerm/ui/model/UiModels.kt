package com.qkzc.workerm.ui.model

import androidx.annotation.DrawableRes

data class DashboardMetricUi(
    val label: String,
    val value: String,
    val trend: String,
    val backgroundColor: String,
)

data class InfoRowUi(
    val title: String,
    val subtitle: String,
    val tag: String? = null,
    val trailing: String? = null,
)

data class QuickActionUi(
    val title: String,
    @param:DrawableRes val iconRes: Int,
    val accentColor: String,
    val route: String,
)

data class FeatureCardUi(
    val title: String,
    val description: String,
    val phase: String,
)
