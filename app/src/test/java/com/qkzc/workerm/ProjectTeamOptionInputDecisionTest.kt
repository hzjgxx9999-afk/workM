package com.qkzc.workerm

import com.qkzc.workerm.ui.project.decideProjectTeamOptionInput
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectTeamOptionInputDecisionTest {

    private val options = listOf(
        Option("Alice  13800000000"),
        Option("Steel worker"),
    )

    @Test
    fun clearingSelectedTextSearchesAgainAndKeepsDropdownOpen() {
        val decision = options.decideProjectTeamOptionInput("") { it.label }

        assertNull(decision.selectedOption)
        assertTrue(decision.shouldSearch)
        assertTrue(decision.shouldShowDropdown)
    }

    @Test
    fun selectingExactOptionDoesNotSearchOrReopenDropdown() {
        val decision = options.decideProjectTeamOptionInput("Steel worker") { it.label }

        assertSame(options[1], decision.selectedOption)
        assertFalse(decision.shouldSearch)
        assertFalse(decision.shouldShowDropdown)
    }

    private data class Option(val label: String)
}
