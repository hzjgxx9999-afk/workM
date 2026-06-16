package com.qkzc.workerm

import com.qkzc.workerm.data.network.AjaxResp
import com.qkzc.workerm.data.network.ManagerProjectFileVo
import com.qkzc.workerm.data.network.SupervisorApi
import com.qkzc.workerm.data.project.DrawingDocsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class DrawingDocsRepositoryTest {

    @Test
    fun cadDrawingPreviewPageUsesAbsoluteBackendPreviewStreamUrl() = runBlocking {
        val api = FakeDrawingApi(
            files = listOf(
                ManagerProjectFileVo(
                    fileId = 1,
                    fileName = "plan.dwg",
                    fileType = "dwg",
                    previewUrl = "/common/file/preview?path=app%2Fproject-drawing%2F100%2Fplan.dwg",
                    downloadUrl = "/common/file/redirect?path=app%2Fproject-drawing%2F100%2Fplan.dwg",
                    objectUrl = "app/project-drawing/100/plan.dwg",
                ),
            ),
        )
        val repository = DrawingDocsRepository(api)

        val docs = repository.loadDrawingDocs("token", 100L)

        assertEquals("Bearer token", api.lastToken)
        assertEquals(1, docs.size)
        val pageUri = URI.create(docs.first().previewPageUrl)
        assertEquals(BuildConfig.CAD_PREVIEW_BASE_URL, "${pageUri.scheme}://${pageUri.authority}${pageUri.path}")
        val queryParams = pageUri.query.split("&")
            .map { it.substringBefore("=") to URLDecoder.decode(it.substringAfter("=", ""), StandardCharsets.UTF_8) }
            .toMap()
        assertEquals(
            "${BuildConfig.SUPERVISOR_BASE_URL.trimEnd('/')}/common/file/preview?path=app/project-drawing/100/plan.dwg",
            queryParams["url"],
        )
        assertEquals("plan.dwg", queryParams["name"])
        assertTrue(docs.first().rawUrl.startsWith("/common/file/preview"))
    }

    private class FakeDrawingApi(
        private val files: List<ManagerProjectFileVo>,
    ) : SupervisorApi by EmptySupervisorApi() {
        var lastToken: String? = null

        override suspend fun manageProjectDrawingDocs(
            token: String,
            projectId: Long,
            limit: Int?,
        ): AjaxResp<List<ManagerProjectFileVo>> {
            lastToken = token
            return AjaxResp(code = 200, msg = "ok", data = files)
        }
    }
}
