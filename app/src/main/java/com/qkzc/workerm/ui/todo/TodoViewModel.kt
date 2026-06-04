package com.qkzc.workerm.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qkzc.workerm.data.approval.ApprovalRepository
import com.qkzc.workerm.data.approval.model.ApprovalFilter
import com.qkzc.workerm.data.approval.model.ApprovalItem
import com.qkzc.workerm.data.approval.model.ApprovalSummary
import com.qkzc.workerm.data.session.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ApprovalUiState(
    val loading: Boolean = false,
    val filter: ApprovalFilter = ApprovalFilter.PENDING,
    val summary: ApprovalSummary = ApprovalSummary(0, 0, 0),
    val approvals: List<ApprovalItem> = emptyList(),
    val errorMessage: String? = null,
)

class TodoViewModel(
    private val approvalRepository: ApprovalRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApprovalUiState())
    val uiState: StateFlow<ApprovalUiState> = _uiState.asStateFlow()

    fun load(filter: ApprovalFilter = _uiState.value.filter) {
        _uiState.update { it.copy(loading = true, filter = filter, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                val session = sessionStore.sessionFlow.first()
                approvalRepository.loadPage(
                    token = session.accessToken,
                    projectId = null,
                    filter = filter,
                )
            }.onSuccess { (summary, approvals) ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        summary = summary,
                        approvals = approvals,
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        approvals = emptyList(),
                        errorMessage = throwable.message ?: "审批列表加载失败",
                    )
                }
            }
        }
    }

    fun audit(item: ApprovalItem, approve: Boolean, remark: String) {
        _uiState.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                val session = sessionStore.sessionFlow.first()
                approvalRepository.audit(
                    token = session.accessToken,
                    item = item,
                    approve = approve,
                    remark = remark,
                )
            }.onSuccess {
                load(_uiState.value.filter)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        loading = false,
                        errorMessage = throwable.message ?: "审批操作失败",
                    )
                }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    class Factory(
        private val approvalRepository: ApprovalRepository,
        private val sessionStore: SessionStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TodoViewModel(approvalRepository, sessionStore) as T
        }
    }
}
