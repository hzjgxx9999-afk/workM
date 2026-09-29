package com.qkzc.workerm.ui.legal

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.qkzc.workerm.ui.theme.LoginBody
import com.qkzc.workerm.ui.theme.LoginMuted
import com.qkzc.workerm.ui.theme.LoginPrimaryBlue
import com.qkzc.workerm.ui.theme.LoginTitle
import com.qkzc.workerm.ui.theme.WorkerMTheme

class LegalDocumentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()

        val type = intent.getStringExtra(EXTRA_DOCUMENT_TYPE)
            ?.let { value ->
                LegalDocumentType.entries.firstOrNull { it.intentValue == value }
            }
            ?: LegalDocumentType.SERVICE_AGREEMENT
        val document = LegalDocuments.documentFor(type)

        setContent {
            WorkerMTheme {
                LegalDocumentScreen(
                    document = document,
                    onBack = ::finish,
                )
            }
        }
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    companion object {
        private const val EXTRA_DOCUMENT_TYPE = "extra_document_type"

        fun createIntent(context: Context, type: LegalDocumentType): Intent {
            return Intent(context, LegalDocumentActivity::class.java)
                .putExtra(EXTRA_DOCUMENT_TYPE, type.intentValue)
        }
    }
}

@Composable
private fun LegalDocumentScreen(
    document: LegalDocument,
    onBack: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ComposeColor(0xFFF7F9FC),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(ComposeColor.White)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = LoginTitle,
                    )
                }

                Text(
                    text = document.title,
                    color = LoginTitle,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            HorizontalDivider(color = ComposeColor(0xFFE7EBF1))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 20.dp,
                    top = 20.dp,
                    end = 20.dp,
                    bottom = 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = document.title,
                            color = LoginTitle,
                            fontSize = 26.sp,
                            lineHeight = 34.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "生效日期：${document.effectiveDate}",
                            color = LoginPrimaryBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                items(document.introduction) { paragraph ->
                    LegalParagraph(text = paragraph)
                }

                items(document.sections) { section ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = section.title,
                            color = LoginTitle,
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        LegalParagraph(text = section.body)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "清科筑成股份有限公司",
                        color = LoginMuted,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun LegalParagraph(text: String) {
    Text(
        text = text,
        color = LoginBody,
        fontSize = 15.sp,
        lineHeight = 25.sp,
        style = MaterialTheme.typography.bodyMedium,
    )
}
