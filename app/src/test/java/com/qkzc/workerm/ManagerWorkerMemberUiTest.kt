package com.qkzc.workerm

import com.qkzc.workerm.data.worker.ManagerWorker
import com.qkzc.workerm.data.worker.ManagerWorkerFilter
import com.qkzc.workerm.data.worker.ManagerWorkerPageMode
import com.qkzc.workerm.data.worker.ProjectMemberPageState
import com.qkzc.workerm.data.worker.filterForMemberPage
import com.qkzc.workerm.data.worker.toMemberSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class ManagerWorkerMemberUiTest {

    @Test
    fun filterStatusMatchesBackendBindStatus() {
        assertEquals(null, ManagerWorkerFilter.ALL.backendStatus)
        assertEquals(null, ManagerWorkerFilter.TEAM_LEADER.backendStatus)
        assertEquals(null, ManagerWorkerFilter.WORKER.backendStatus)
        assertEquals("ENTERING", ManagerWorkerFilter.ENTERING.backendStatus)
        assertEquals("EXITED", ManagerWorkerFilter.EXITED.backendStatus)
    }

    @Test
    fun globalMemberPageDoesNotRequireProjectId() {
        val global = ProjectMemberPageState.fromArgument(projectId = 0L)
        val project = ProjectMemberPageState.fromArgument(projectId = 88L)

        assertEquals(ManagerWorkerPageMode.GLOBAL, global.mode)
        assertEquals(null, global.workerQueryProjectId)
        assertEquals("工人管理", global.title)
        assertEquals(ManagerWorkerPageMode.PROJECT, project.mode)
        assertEquals(88L, project.workerQueryProjectId)
        assertEquals("项目成员", project.title)
    }

    @Test
    fun filtersCanShowLeaderWorkersAndOrdinaryWorkersSeparately() {
        val workers = listOf(
            worker(10, leaderId = 10, bindStatus = "ACTIVE"),
            worker(11, leaderId = 10, bindStatus = "ACTIVE"),
            worker(12, leaderId = 10, bindStatus = "ENTERING"),
            worker(13, leaderId = 0, bindStatus = "EXITED"),
        )

        assertEquals(listOf(10L), workers.filterForMemberPage(ManagerWorkerFilter.TEAM_LEADER).map { it.workerUserId })
        assertEquals(listOf(11L, 12L, 13L), workers.filterForMemberPage(ManagerWorkerFilter.WORKER).map { it.workerUserId })
        assertEquals(listOf(12L), workers.filterForMemberPage(ManagerWorkerFilter.ENTERING).map { it.workerUserId })
        assertEquals(listOf(13L), workers.filterForMemberPage(ManagerWorkerFilter.EXITED).map { it.workerUserId })
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
