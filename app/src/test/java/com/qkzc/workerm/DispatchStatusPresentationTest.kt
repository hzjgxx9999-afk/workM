package com.qkzc.workerm

import com.qkzc.workerm.data.dispatch.DispatchStatusPresentation
import org.junit.Assert.assertEquals
import org.junit.Test

class DispatchStatusPresentationTest {
    @Test
    fun mapsBackendStatusesToManagerLabels() {
        assertEquals("待班组长接收", DispatchStatusPresentation.label("PENDING_DELEGATION"))
        assertEquals("待重新指派", DispatchStatusPresentation.label("PENDING_REASSIGNMENT"))
        assertEquals("待原班组整改", DispatchStatusPresentation.label("PENDING_TEAM_REWORK"))
        assertEquals("处理中", DispatchStatusPresentation.label("IN_PROGRESS"))
        assertEquals("待汇总", DispatchStatusPresentation.label("PENDING_LEADER_SUMMARY"))
        assertEquals("待验收", DispatchStatusPresentation.label("PENDING_MANAGER_ACCEPTANCE"))
        assertEquals("已完成", DispatchStatusPresentation.label("COMPLETED"))
        assertEquals("已取消", DispatchStatusPresentation.label("CANCELLED"))
        assertEquals("已到达现场", DispatchStatusPresentation.label("ARRIVED"))
    }

    @Test
    fun overdueTakesPriorityOverBusinessStatus() {
        assertEquals("已超时", DispatchStatusPresentation.label("IN_PROGRESS", overdue = true))
    }

    @Test
    fun managerCanReassignOrCancelOnlyBeforeTerminalState() {
        assertEquals(true, DispatchStatusPresentation.canReassign("IN_PROGRESS"))
        assertEquals(true, DispatchStatusPresentation.canCancel("PENDING_MANAGER_ACCEPTANCE"))
        assertEquals(false, DispatchStatusPresentation.canReassign("COMPLETED"))
        assertEquals(false, DispatchStatusPresentation.canCancel("CANCELLED"))
    }
}
