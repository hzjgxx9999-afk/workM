package com.qkzc.workerm

import com.qkzc.workerm.ui.legal.LegalDocumentType
import com.qkzc.workerm.ui.legal.LegalDocuments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalDocumentsTest {

    @Test
    fun bothDocumentsUseRequiredEffectiveDate() {
        assertEquals("2026年07月13日", LegalDocuments.serviceAgreement.effectiveDate)
        assertEquals("2026年07月13日", LegalDocuments.privacyPolicy.effectiveDate)
    }

    @Test
    fun serviceAgreementIsForSupervisionAppAndCompany() {
        val introduction = LegalDocuments.serviceAgreement.introduction.joinToString()
        assertTrue(introduction.contains("清科筑成股份有限公司"))
        assertTrue(introduction.contains("监管端 App"))
    }

    @Test
    fun documentTypeReturnsMatchingDocument() {
        assertEquals(
            "服务协议",
            LegalDocuments.documentFor(LegalDocumentType.SERVICE_AGREEMENT).title,
        )
        assertEquals(
            "隐私政策",
            LegalDocuments.documentFor(LegalDocumentType.PRIVACY_POLICY).title,
        )
    }
}
