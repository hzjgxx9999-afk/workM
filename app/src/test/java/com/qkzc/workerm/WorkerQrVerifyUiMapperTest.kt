package com.qkzc.workerm

import com.qkzc.workerm.data.worker.WorkerQrVerifyResult
import com.qkzc.workerm.ui.worker.WorkerQrVerifyUiMapper
import com.qkzc.workerm.ui.worker.WorkerQrVerifyUiTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkerQrVerifyUiMapperTest {

    @Test
    fun passResultMapsBackendEnumsToReadableVerificationCard() {
        val state = WorkerQrVerifyUiMapper.fromResult(
            result = WorkerQrVerifyResult(
                pass = true,
                status = "PASS",
                message = "核验通过",
                workerName = "胡忠健",
                mobile = "15212349678",
                idCardNo = "130433199901012931",
                workTypeName = "电工",
                projectName = "济南地铁4号线装修",
                teamName = "周巨樟班组",
                leaderName = "周巨樟",
                entryStatus = "COMPLETED",
                safetyTrainingStatus = "COMPLETED",
                healthCheckStatus = "COMPLETED",
                contractStatus = "COMPLETED",
            ),
            scanTime = "2026-06-26 18:00",
        )

        assertEquals(WorkerQrVerifyUiTone.PASS, state.tone)
        assertEquals("核验通过", state.statusTitle)
        assertEquals("允许进入项目现场", state.statusSubtitle)
        assertEquals("胡忠健", state.workerName)
        assertEquals("电工", state.workType)
        assertEquals("所属班组：周巨樟班组", state.teamLine)
        assertEquals("在场", state.presenceText)
        assertEquals("152****9678", state.mobile)
        assertEquals("130***********2931", state.idCardNo)
        assertEquals("已完成", state.entryValue)
        assertEquals("流程已完成", state.entryHint)
        assertEquals("已完成", state.trainingValue)
        assertEquals("已满足要求", state.trainingHint)
        assertFalse(state.showFailureReason)
        assertEquals("扫描时间：2026-06-26 18:00        点击继续扫码 >", state.footerText)
    }

    @Test
    fun failedExitedResultShowsExplicitFailureReason() {
        val state = WorkerQrVerifyUiMapper.fromResult(
            result = WorkerQrVerifyResult(
                ticketValid = false,
                pass = false,
                status = "VERIFY_FAILED",
                message = "已离场",
                workerName = "胡忠健",
                workTypeName = "电工",
                projectName = "济南地铁4号线装修",
                teamName = "周巨樟班组",
                leaderName = "周巨樟",
                entryStatus = "COMPLETED",
                safetyTrainingStatus = "COMPLETED",
                healthCheckStatus = "COMPLETED",
                contractStatus = "COMPLETED",
            ),
            scanTime = "2026-06-26 18:00",
        )

        assertEquals(WorkerQrVerifyUiTone.FAIL, state.tone)
        assertEquals("核验失败", state.statusTitle)
        assertEquals("该工人已完成离场", state.statusSubtitle)
        assertEquals("离场", state.presenceText)
        assertTrue(state.showFailureReason)
        assertEquals("工人已完成离场，不能作为当前在场人员核验通过。", state.failureReason)
        assertEquals("已离场", state.entryValue)
        assertEquals("已离场不放行", state.entryHint)
    }

    @Test
    fun moduleHintsFollowEachRawStatusEvenWhenPassFlagIsInconsistent() {
        val state = WorkerQrVerifyUiMapper.fromResult(
            result = WorkerQrVerifyResult(
                pass = true,
                status = "PASS",
                message = "核验通过",
                workerName = "胡忠健",
                entryStatus = "NOT_STARTED",
                safetyTrainingStatus = "NOT_STARTED",
                healthCheckStatus = "NOT_STARTED",
                contractStatus = "NOT_STARTED",
            ),
            scanTime = "2026-06-26 18:00",
        )

        assertEquals(WorkerQrVerifyUiTone.FAIL, state.tone)
        assertEquals("核验失败", state.statusTitle)
        assertEquals("当前项目入场资料未完成", state.statusSubtitle)
        assertEquals("当前项目入场资料未完成，不能作为在场人员核验通过。", state.failureReason)
        assertEquals("未开始", state.entryValue)
        assertEquals("尚未开始", state.entryHint)
        assertEquals("未开始", state.trainingValue)
        assertEquals("尚未开始", state.trainingHint)
        assertEquals("未开始", state.healthValue)
        assertEquals("尚未开始", state.healthHint)
        assertEquals("未开始", state.contractValue)
        assertEquals("尚未签署", state.contractHint)
    }

    @Test
    fun invalidTicketWithoutExpiredSignalStaysFailure() {
        val state = WorkerQrVerifyUiMapper.fromResult(
            result = WorkerQrVerifyResult(
                ticketValid = false,
                pass = false,
                status = "VERIFY_FAILED",
                message = "二维码无效",
                workerName = "胡忠健",
            ),
            scanTime = "2026-06-26 18:00",
        )

        assertEquals(WorkerQrVerifyUiTone.FAIL, state.tone)
        assertEquals("核验失败", state.statusTitle)
        assertEquals("二维码无效", state.statusSubtitle)
        assertEquals("二维码无效", state.failureReason)
    }

    @Test
    fun waitingStateUsesScanInstructionsWithoutFakeWorkerData() {
        val state = WorkerQrVerifyUiMapper.waiting()

        assertEquals(WorkerQrVerifyUiTone.WAITING, state.tone)
        assertEquals("对准二维码", state.statusTitle)
        assertEquals("识别后自动核验", state.statusSubtitle)
        assertEquals("等待扫码", state.workerName)
        assertEquals("请扫描工人端个人二维码", state.workType)
        assertEquals("管理端按当前项目范围核验", state.teamLine)
        assertEquals("待扫", state.presenceText)
        assertEquals("--", state.projectName)
        assertEquals("等待扫码", state.entryValue)
        assertEquals("扫描工人二维码", state.entryHint)
    }
}
