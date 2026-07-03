package com.qkzc.workerm.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qkzc.workerm.R
import com.qkzc.workerm.ui.theme.WorkerMTheme

@Composable
fun HomeOverviewSection(
    uiState: HomeOverviewUiState,
    onCardClick: (OverviewType) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        HomeOverviewHeader(
            dateText = (uiState as? HomeOverviewUiState.Success)?.dateText,
        )

        Spacer(modifier = Modifier.height(HomeOverviewDimens.HeaderToCardsGap))

        when (uiState) {
            HomeOverviewUiState.Loading -> HomeOverviewLoading()
            is HomeOverviewUiState.Error -> HomeOverviewError(
                message = stringResource(uiState.messageRes),
                onRetryClick = onRetryClick,
            )
            is HomeOverviewUiState.Success -> OverviewCards(
                items = uiState.items,
                onCardClick = onCardClick,
            )
        }
    }
}

@Composable
private fun HomeOverviewHeader(
    dateText: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_overview_title),
            modifier = Modifier.weight(1f),
            color = HomeOverviewColors.Title,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (dateText != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = dateText,
                color = HomeOverviewColors.Date,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OverviewCards(
    items: List<OverviewItem>,
    onCardClick: (OverviewType) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val requiredWidth = HomeOverviewDimens.CardMinWidth * items.size +
            HomeOverviewDimens.CardGap * (items.size - 1)
        val canFitSingleRow = maxWidth >= requiredWidth

        if (canFitSingleRow) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HomeOverviewDimens.CardGap),
            ) {
                items.forEach { item ->
                    OverviewCard(
                        item = item,
                        onClick = onCardClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HomeOverviewDimens.CardGap),
                contentPadding = PaddingValues(horizontal = 0.dp),
            ) {
                items(
                    items = items,
                    key = { item -> item.type.name },
                ) { item ->
                    OverviewCard(
                        item = item,
                        onClick = onCardClick,
                        modifier = Modifier.width(HomeOverviewDimens.CardMinWidth),
                    )
                }
            }
        }
    }
}

@Composable
fun OverviewCard(
    item: OverviewItem,
    onClick: (OverviewType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .height(HomeOverviewDimens.CardHeight)
            .widthIn(min = HomeOverviewDimens.CardMinWidth)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = { onClick(item.type) },
            )
            .semantics {
                role = Role.Button
            },
        shape = RoundedCornerShape(HomeOverviewDimens.CardCornerRadius),
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = HomeOverviewDimens.CardShadowElevation,
        border = BorderStroke(1.dp, HomeOverviewColors.CardBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = item.iconType.imageVector(),
                contentDescription = stringResource(item.iconContentDescriptionRes),
                tint = item.iconType.tintColor(),
                modifier = Modifier.size(HomeOverviewDimens.IconSize),
            )

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = stringResource(item.titleRes),
                color = HomeOverviewColors.Title,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.quantity.toString(),
                    color = HomeOverviewColors.Value,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(item.unitRes),
                    color = HomeOverviewColors.Value,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun HomeOverviewLoading(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(HomeOverviewDimens.StatusHeight),
        shape = RoundedCornerShape(HomeOverviewDimens.CardCornerRadius),
        color = Color.White,
        border = BorderStroke(1.dp, HomeOverviewColors.CardBorder),
        shadowElevation = HomeOverviewDimens.CardShadowElevation,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.home_overview_loading),
                color = HomeOverviewColors.Date,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.sp,
            )
        }
    }
}

@Composable
private fun HomeOverviewError(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(HomeOverviewDimens.StatusHeight),
        shape = RoundedCornerShape(HomeOverviewDimens.CardCornerRadius),
        color = Color.White,
        border = BorderStroke(1.dp, HomeOverviewColors.CardBorder),
        shadowElevation = HomeOverviewDimens.CardShadowElevation,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_overview_error_title),
                    color = HomeOverviewColors.Title,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = message,
                    color = HomeOverviewColors.Date,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            TextButton(onClick = onRetryClick) {
                Text(
                    text = stringResource(R.string.home_overview_retry),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    letterSpacing = 0.sp,
                )
            }
        }
    }
}

private fun OverviewIconType.imageVector(): ImageVector {
    return when (this) {
        OverviewIconType.ProjectDocument -> Icons.Filled.Description
        OverviewIconType.WorkerGroup -> Icons.Filled.Groups
        OverviewIconType.ApprovalDocument -> Icons.Filled.AssignmentTurnedIn
        OverviewIconType.WarningTriangle -> Icons.Filled.Warning
    }
}

private fun OverviewIconType.tintColor(): Color {
    return when (this) {
        OverviewIconType.ProjectDocument,
        OverviewIconType.WorkerGroup -> HomeOverviewColors.IconBlue
        OverviewIconType.ApprovalDocument -> HomeOverviewColors.IconDark
        OverviewIconType.WarningTriangle -> HomeOverviewColors.IconRed
    }
}

private object HomeOverviewDimens {
    val HeaderToCardsGap = 12.dp
    val CardGap = 8.dp
    val CardMinWidth = 76.dp
    val CardHeight = 86.dp
    val CardCornerRadius = 14.dp
    val CardShadowElevation = 2.dp
    val IconSize = 24.dp
    val StatusHeight = 86.dp
}

private object HomeOverviewColors {
    val PageBackground = Color(0xFFF3F6FA)
    val Title = Color(0xFF1C2733)
    val Date = Color(0xFF66788A)
    val Value = Color(0xFF1C2733)
    val CardBorder = Color(0xFFE4EBF5)
    val IconBlue = Color(0xFF1677FF)
    val IconDark = Color(0xFF1C1F24)
    val IconRed = Color(0xFFFF5B52)
}

@Preview(
    name = "Home overview - normal phone",
    widthDp = 393,
    heightDp = 132,
    showBackground = true,
    backgroundColor = 0xFFF3F6FA,
)
@Composable
private fun HomeOverviewSectionPreview() {
    WorkerMTheme {
        Box(
            modifier = Modifier
                .background(HomeOverviewColors.PageBackground)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            HomeOverviewSection(
                uiState = previewOverviewState(),
                onCardClick = {},
                onRetryClick = {},
            )
        }
    }
}

@Preview(
    name = "Home overview - narrow phone",
    widthDp = 300,
    heightDp = 132,
    showBackground = true,
    backgroundColor = 0xFFF3F6FA,
)
@Composable
private fun HomeOverviewSectionNarrowPreview() {
    WorkerMTheme {
        Box(
            modifier = Modifier
                .background(HomeOverviewColors.PageBackground)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            HomeOverviewSection(
                uiState = previewOverviewState(),
                onCardClick = {},
                onRetryClick = {},
            )
        }
    }
}

private fun previewOverviewState(): HomeOverviewUiState.Success {
    return HomeOverviewUiState.Success(
        dateText = "2024-05-20  \u661f\u671f\u4e00",
        items = listOf(
            OverviewItem(
                type = OverviewType.UnderConstructionProjects,
                titleRes = R.string.home_overview_project_title,
                quantity = 12,
                unitRes = R.string.home_overview_unit_count,
                iconType = OverviewIconType.ProjectDocument,
                iconContentDescriptionRes = R.string.home_overview_project_icon_cd,
            ),
            OverviewItem(
                type = OverviewType.OnSiteWorkers,
                titleRes = R.string.home_overview_worker_title,
                quantity = 328,
                unitRes = R.string.home_overview_unit_person,
                iconType = OverviewIconType.WorkerGroup,
                iconContentDescriptionRes = R.string.home_overview_worker_icon_cd,
            ),
            OverviewItem(
                type = OverviewType.TodayApprovals,
                titleRes = R.string.home_overview_approval_title,
                quantity = 15,
                unitRes = R.string.home_overview_unit_item,
                iconType = OverviewIconType.ApprovalDocument,
                iconContentDescriptionRes = R.string.home_overview_approval_icon_cd,
            ),
            OverviewItem(
                type = OverviewType.RiskWarnings,
                titleRes = R.string.home_overview_warning_title,
                quantity = 8,
                unitRes = R.string.home_overview_unit_record,
                iconType = OverviewIconType.WarningTriangle,
                iconContentDescriptionRes = R.string.home_overview_warning_icon_cd,
            ),
        ),
    )
}
