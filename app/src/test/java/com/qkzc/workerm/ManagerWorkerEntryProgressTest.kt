package com.qkzc.workerm

import com.qkzc.workerm.data.worker.ManagerWorker
import com.qkzc.workerm.data.worker.entryProgressText
import com.qkzc.workerm.data.worker.entryStatusText
import com.qkzc.workerm.data.worker.entrySteps
import org.junit.Assert.assertEquals
import org.junit.Test

class ManagerWorkerEntryProgressTest {

    @Test
    fun entryStepsMapBackendStatusesToReadableChineseLabels() {
        val worker = ManagerWorker(
            workerUserId = 501,
            projectId = 10,
            entryStatus = "IN_PROGRESS",
            identityStatus = "COMPLETED",
            safetyTrainingStatus = "IN_PROGRESS",
            healthCheckStatus = "NOT_STARTED",
            entryContractStatus = "NOT_STARTED",
            insuranceStatus = "NOT_STARTED",
        )

        val steps = worker.entrySteps()

        assertEquals("身份认证", steps[0].title)
        assertEquals("已完成", steps[0].statusText)
        assertEquals("安全培训", steps[1].title)
        assertEquals("进行中", steps[1].statusText)
        assertEquals("健康检查", steps[2].title)
        assertEquals("未开始", steps[2].statusText)
        assertEquals("合同签署", steps[3].title)
        assertEquals("未开始", steps[3].statusText)
        assertEquals("保险签署", steps[4].title)
        assertEquals("未开始", steps[4].statusText)
    }

    @Test
    fun entryProgressTitleUsesCompletedCountAndOverallStatus() {
        val worker = ManagerWorker(
            workerUserId = 501,
            projectId = 10,
            entryStatus = "IN_PROGRESS",
            identityStatus = "COMPLETED",
            safetyTrainingStatus = "COMPLETED",
            healthCheckStatus = "NOT_STARTED",
            entryContractStatus = "NOT_STARTED",
            insuranceStatus = "NOT_STARTED",
        )

        assertEquals("入场流程（2/5 进行中）", worker.entryProgressText())
    }

    @Test
    fun blankOverallStatusIsDerivedFromStepStatuses() {
        val worker = ManagerWorker(
            workerUserId = 501,
            projectId = 10,
            identityStatus = "COMPLETED",
            safetyTrainingStatus = "COMPLETED",
            healthCheckStatus = "COMPLETED",
            entryContractStatus = "COMPLETED",
            insuranceStatus = "COMPLETED",
        )

        assertEquals("已完成", worker.entryStatusText())
        assertEquals("入场流程（5/5 已完成）", worker.entryProgressText())
    }
}
