package com.qkzc.workerm

import com.qkzc.workerm.ui.login.validateSupervisorLoginInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SupervisionLoginValidatorTest {

    @Test
    fun emptyPhoneRequiresPhoneMessage() {
        assertEquals("请输入手机号", validateSupervisorLoginInput("", "123456"))
    }

    @Test
    fun invalidPhoneRequiresCorrectPhoneMessage() {
        assertEquals("请输入正确的手机号", validateSupervisorLoginInput("12345", "123456"))
    }

    @Test
    fun emptyPasswordRequiresPasswordMessage() {
        assertEquals("请输入密码", validateSupervisorLoginInput("13800138000", ""))
    }

    @Test
    fun validPhoneAndPasswordPassValidation() {
        assertNull(validateSupervisorLoginInput("13800138000", "123456"))
    }
}
