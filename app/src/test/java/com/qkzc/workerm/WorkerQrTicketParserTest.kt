package com.qkzc.workerm

import com.qkzc.workerm.data.worker.WorkerQrTicketParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkerQrTicketParserTest {

    @Test
    fun parsesPlainTicket() {
        assertEquals("ticket-plain", WorkerQrTicketParser.parseTicket(" ticket-plain "))
    }

    @Test
    fun parsesJsonTicket() {
        assertEquals("ticket-json", WorkerQrTicketParser.parseTicket("""{"ticket":"ticket-json"}"""))
    }

    @Test
    fun parsesUrlTicket() {
        assertEquals(
            "ticket-url",
            WorkerQrTicketParser.parseTicket("https://example.com/worker/qr?ticket=ticket-url&source=worker"),
        )
    }

    @Test
    fun parsesQueryStringTicket() {
        assertEquals("ticket-query", WorkerQrTicketParser.parseTicket("uid=501&ticket=ticket-query&sig=abc"))
    }

    @Test
    fun rejectsBlankContent() {
        assertNull(WorkerQrTicketParser.parseTicket("   "))
    }
}
