package com.reader.analytics.sync.application

import com.reader.analytics.sync.domain.SyncRunStatus

sealed class CancelResult {
    data object Success : CancelResult()
    data object NotFound : CancelResult()
    data class NotCancellable(val status: SyncRunStatus) : CancelResult()
}