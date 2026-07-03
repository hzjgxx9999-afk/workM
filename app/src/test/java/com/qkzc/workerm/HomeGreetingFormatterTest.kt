package com.qkzc.workerm

import com.qkzc.workerm.ui.home.formatHomeGreeting
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeGreetingFormatterTest {

    @Test
    fun formatsGreetingWithRealNameByHour() {
        assertEquals("上午好，王经理", formatHomeGreeting(hourOfDay = 9, realName = "王经理"))
        assertEquals("下午好，王经理", formatHomeGreeting(hourOfDay = 14, realName = "王经理"))
        assertEquals("晚上好，王经理", formatHomeGreeting(hourOfDay = 21, realName = "王经理"))
    }

    @Test
    fun usesManagerFallbackWhenRealNameIsBlank() {
        assertEquals("上午好，项目经理", formatHomeGreeting(hourOfDay = 10, realName = " "))
    }
}
