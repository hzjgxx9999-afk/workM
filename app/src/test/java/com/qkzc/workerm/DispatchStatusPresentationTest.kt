package com.qkzc.workerm

import com.qkzc.workerm.data.dispatch.DispatchStatusPresentation
import org.junit.Assert.assertEquals
import org.junit.Test

class DispatchStatusPresentationTest {
    @Test
    fun mapsBackendStatusesToManagerLabels() {
        assertEquals("待分派", DispatchStatusPresentation.label("PENDING_DELEGATION"))
        assertEquals("处理中", DispatchStatusPresentation.label("IN_PROGRESS"))
        assertEquals("待汇总", DispatchStatusPresentation.label("PENDING_LEADER_SUMMARY"))
        assertEquals("待验收", DispatchStatusPresentation.label("PENDING_MANAGER_ACCEPTANCE"))
        assertEquals("已完成", DispatchStatusPresentation.label("COMPLETED"))
        assertEquals("已取消", DispatchStatusPresentation.label("CANCELLED"))
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
