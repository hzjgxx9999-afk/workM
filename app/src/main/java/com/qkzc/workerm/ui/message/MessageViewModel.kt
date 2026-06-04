package com.qkzc.workerm.ui.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qkzc.workerm.data.aiwarning.AiWarningFilter
import com.qkzc.workerm.data.aiwarning.AiWarningRepository
import com.qkzc.workerm.data.aiwarning.model.AiWarningDetail
import com.qkzc.workerm.data.aiwarning.model.AiWarningHandleStatus
import com.qkzc.workerm.data.aiwarning.model.AiWarningItem
import com.qkzc.workerm.data.aiwarning.model.AiWarningRiskLevel
import com.qkzc.workerm.data.network.ApiException
import com.qkzc.workerm.data.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AiWarningTab {
    ALL,
    HIGH,
    PENDING,
    HANDLED,
}

data class AiWarningUiState(
    val loading: Boolean = false,
    val actionLoading: Boolean = false,
    val selectedTab: AiWarningTab = AiWarningTab.ALL,
    val totalCount: Long = 0,
    val unreadCount: Int = 0,
    val pendingCount: Int = 0,
    val handledCount: Int = 0,
    val highCount: Int = 0,
    val maxRiskScore: Int = 0,
    val warnings: List<AiWarningItem> = emptyList(),
    val selectedDetail: AiWarningDetail? = null,
    val errorMessage: String? = null,
)

class MessageViewModel(
    private val repository: AiWarningRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiWarningUiState())
    val uiState: StateFlow<AiWarningUiState> = _uiState.asStateFlow()

    fun load(tab: AiWarningTab = _uiState.value.selectedTab) {
        _uiState.update {
            it.copy(
                loading = true,
                actionLoading = false,
                selectedTab = tab,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            runCatching {
                val page = repository.loadWarnings(
                    token = requireToken(),
                    filter = tab.toFilter(),
                )
                page
            }.onSuccess { page ->
                val rows = page.rows
                _uiState.update {
                    it.copy(
                        loading = false,
                        actionLoading = false,
                        totalCount = page.total,
                        unreadCount = page.unreadCount,
                        pendingCount = rows.count { row -> row.handleStatus == AiWarningHandleStatus.PENDING },
                        handledCount = rows.count { row -> row.handleStatus == AiWarningHandleStatus.HANDLED },
                        highCount = rows.count { row -> row.riskLevel == AiWarningRiskLevel.HIGH },
                        maxRiskScore = rows.mapNotNull(AiWarningItem::riskScore).maxOrNull() ?: 0,
                        warnings = rows,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        actionLoading = false,
                        warnings = emptyList(),
                        errorMessage = throwable.message ?: "AI预警加载失败",
                    )
                }
            }
        }
    }

    fun openDetail(item: AiWarningItem) {
        if (item.warningId == 0L) return
        _uiState.update { it.copy(actionLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                val token = requireToken()
                val detail = repository.detail(token, item.warningId)
                if (!detail.item.readFlag) {
                    repository.markRead(token, item.warningId)
                }
                detail.copy(item = detail.item.copy(readFlag = true))
            }.onSuccess { detail ->
                _uiState.update {
                    it.copy(
                        actionLoading = false,
                        selectedDetail = detail,
                        unreadCount = (it.unreadCount - if (item.readFlag) 0 else 1).coerceAtLeast(0),
                        warnings = it.warnings.markRead(item.warningId),
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        actionLoading = false,
                        errorMessage = throwable.message ?: "AI预警详情加载失败",
                    )
                }
            }
        }
    }

    fun markRead(item: AiWarningItem) {
        if (item.warningId == 0L || item.readFlag) return
        _uiState.update { it.copy(actionLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                repository.markRead(requireToken(), item.warningId)
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        actionLoading = false,
                        unreadCount = (it.unreadCount - 1).coerceAtLeast(0),
                        warnings = it.warnings.markRead(item.warningId),
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        actionLoading = false,
                        errorMessage = throwable.message ?: "标记已读失败",
                    )
                }
            }
        }
    }

    fun handle(item: AiWarningItem, handleStatus: String = AiWarningHandleStatus.HANDLED) {
        if (item.warningId == 0L) return
        _uiState.update { it.copy(actionLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                repository.handle(requireToken(), item.warningId, handleStatus)
            }.onSuccess {
                load(_uiState.value.selectedTab)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        actionLoading = false,
                        errorMessage = throwable.message ?: "AI预警处理失败",
                    )
                }
            }
        }
    }

    fun handleDetail(detail: AiWarningDetail, handleStatus: String) {
        handle(detail.item, handleStatus)
    }

    fun consumeDetail() {
        _uiState.update { it.copy(selectedDetail = null) }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private suspend fun requireToken(): String {
        val token = sessionStore.sessionFlow.first().accessToken
        return token.takeIf { it.isNotBlank() } ?: throw ApiException("请先登录")
    }

    private fun AiWarningTab.toFilter(): AiWarningFilter {
        return when (this) {
            AiWarningTab.ALL -> AiWarningFilter()
            AiWarningTab.HIGH -> AiWarningFilter(riskLevel = AiWarningRiskLevel.HIGH)
            AiWarningTab.PENDING -> AiWarningFilter(handleStatus = AiWarningHandleStatus.PENDING)
            AiWarningTab.HANDLED -> AiWarningFilter(handleStatus = AiWarningHandleStatus.HANDLED)
        }
    }

    private fun List<AiWarningItem>.markRead(warningId: Long): List<AiWarningItem> {
        return map { item ->
            if (item.warningId == warningId) {
                item.copy(readFlag = true)
            } else {
                item
            }
        }
    }

    class Factory(
        private val repository: AiWarningRepository,
        private val sessionStore: SessionStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MessageViewModel(repository, sessionStore) as T
        }
    }
}
