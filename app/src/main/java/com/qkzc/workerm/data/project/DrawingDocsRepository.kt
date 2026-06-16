package com.qkzc.workerm.data.project

import com.qkzc.workerm.BuildConfig
import com.qkzc.workerm.data.network.ApiClient
import com.qkzc.workerm.data.network.ManagerProjectFileVo
import com.qkzc.workerm.data.network.SupervisorApi
import com.qkzc.workerm.data.network.bearerToken
import com.qkzc.workerm.data.network.requireSuccess
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

class DrawingDocsRepository(
    private val api: SupervisorApi = ApiClient.supervisorApi,
) {

    suspend fun loadDrawingDocs(token: String, projectId: Long): List<DrawingDoc> {
        val response = api.manageProjectDrawingDocs(bearerToken(token), projectId, 50)
        requireSuccess(response.code, response.msg)
        return response.data
            .orEmpty()
            .mapIndexed { index, file -> file.toDomain(projectId, index) }
    }

    suspend fun uploadDrawing(
        token: String,
        projectId: Long,
        fileName: String,
        mimeType: String?,
        bytes: ByteArray,
    ): DrawingDoc {
        val requestBody = bytes.toRequestBody((mimeType ?: "application/octet-stream").toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", fileName, requestBody)
        val textMediaType = "text/plain".toMediaTypeOrNull()
        val response = api.uploadProjectDrawingDoc(
            token = bearerToken(token),
            projectId = projectId,
            file = filePart,
            category = "DRAWING".toRequestBody(textMediaType),
            remark = "移动端上传".toRequestBody(textMediaType),
        )
        requireSuccess(response.code, response.msg)
        return response.data?.toDomain(projectId, 0)?.copy(uploadedFromMobile = true, uploadedBy = "我")
            ?: error("图纸上传成功但未返回文件信息")
    }

    private fun ManagerProjectFileVo.toDomain(projectId: Long, index: Int): DrawingDoc {
        val detectedFileType = fileType.orEmpty().ifBlank { inferFileType(fileName.orEmpty()) }
        val rawPreviewUrl = previewUrl.orEmpty()
            .ifBlank { objectKey.orEmpty().toPreviewStreamUrl() }
            .ifBlank { objectUrl.orEmpty().ifBlank { downloadUrl.orEmpty() } }
        return DrawingDoc(
            docId = fileId ?: (projectId * 10_000 + index + 1),
            projectId = projectId,
            fileName = fileName.orEmpty().ifBlank { "未命名文件" },
            fileType = detectedFileType,
            fileSize = fileSize ?: 0L,
            category = category.orEmpty(),
            createTime = createTime.orEmpty(),
            rawUrl = rawPreviewUrl,
            previewPageUrl = buildPreviewPageUrl(detectedFileType, rawPreviewUrl, fileName.orEmpty()),
            uploadedBy = uploaderName.orEmpty(),
            uploadedFromMobile = false,
        )
    }

    private fun buildPreviewPageUrl(fileType: String, rawUrl: String, fileName: String): String {
        if (rawUrl.isBlank()) {
            return ""
        }
        if (fileType.lowercase(Locale.ROOT) !in setOf("dwg", "dxf")) {
            return toBackendAbsoluteUrl(rawUrl)
        }
        return appendQueryParameters(
            BuildConfig.CAD_PREVIEW_BASE_URL,
            "url" to toBackendAbsoluteUrl(rawUrl),
            "name" to fileName.ifBlank { "drawing" },
        )
    }

    private fun String.toPreviewStreamUrl(): String {
        val objectName = trim().trimStart('/')
        if (objectName.isBlank()) {
            return ""
        }
        return "/common/file/preview?path=${encodeQueryValue(objectName)}"
    }

    private fun toBackendAbsoluteUrl(url: String): String {
        val value = url.trim()
        if (value.isBlank() || value.startsWith("http://") || value.startsWith("https://")) {
            return value
        }
        val base = BuildConfig.SUPERVISOR_BASE_URL.trimEnd('/')
        val path = if (value.startsWith("/")) value else "/$value"
        return "$base$path"
    }

    private fun appendQueryParameters(baseUrl: String, vararg params: Pair<String, String>): String {
        val separator = if (baseUrl.contains("?")) "&" else "?"
        return buildString {
            append(baseUrl)
            append(separator)
            params.forEachIndexed { index, (key, value) ->
                if (index > 0) {
                    append("&")
                }
                append(encodeQueryValue(key))
                append("=")
                append(encodeQueryValue(value))
            }
        }
    }

    private fun encodeQueryValue(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20")
    }

    private fun inferFileType(fileName: String, mimeType: String? = null): String {
        val byName = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (byName.isNotBlank()) return byName
        return mimeType.orEmpty().substringAfterLast('/').lowercase(Locale.ROOT)
    }
}

data class DrawingDoc(
    val docId: Long,
    val projectId: Long,
    val fileName: String,
    val fileType: String,
    val fileSize: Long,
    val category: String,
    val createTime: String,
    val rawUrl: String,
    val previewPageUrl: String,
    val uploadedBy: String,
    val uploadedFromMobile: Boolean,
) {
    val supportsCadPreview: Boolean
        get() = fileType.lowercase(Locale.ROOT) in setOf("dwg", "dxf")

    val canOpenPreview: Boolean
        get() = previewPageUrl.isNotBlank()

    val displayType: String
        get() = fileType.uppercase(Locale.ROOT)
}
