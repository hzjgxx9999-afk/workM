package com.qkzc.workerm

import com.qkzc.workerm.data.worker.ManagerWorker
import com.qkzc.workerm.data.worker.ManagerWorkerFilter
import com.qkzc.workerm.data.worker.toMemberSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class ManagerWorkerMemberUiTest {

    @Test
    fun filterStatusMatchesBackendBindStatus() {
        assertEquals(null, ManagerWorkerFilter.ALL.backendStatus)
        assertEquals("ACTIVE", ManagerWorkerFilter.ACTIVE.backendStatus)
        assertEquals("ENTERING", ManagerWorkerFilter.ENTERING.backendStatus)
        assertEquals("EXITED", ManagerWorkerFilter.EXITED.backendStatus)
    }

    @Test
    fun summaryCountsWorkersByProjectMemberBusinessStatus() {
        val workers = listOf(
            worker(1, leaderId = 10, bindStatus = "ACTIVE"),
            worker(2, leaderId = 10, bindStatus = "ENTERING"),
            worker(3, leaderId = 11, bindStatus = "EXITED"),
            worker(4, leaderId = 0, bindStatus = "CANCELLED"),
        )

        val summary = workers.toMemberSummary()

        assertEquals(4, summary.totalWorkers)
        assertEquals(2, summary.teamLeaderCount)
        assertEquals(1, summary.activeWorkers)
        assertEquals(1, summary.enteringWorkers)
        assertEquals(1, summary.exitedWorkers)
        assertEquals(1, summary.abnormalWorkers)
    }

    private fun worker(
        workerUserId: Long,
        leaderId: Long,
        bindStatus: String,
    ): ManagerWorker {
        return ManagerWorker(
            workerUserId = workerUserId,
            projectId = 100,
            leaderId = leaderId,
            bindStatus = bindStatus,
        )
    }
}
