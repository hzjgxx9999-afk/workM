package com.qkzc.workerm.data.project

import java.net.URI
import java.net.URL
import java.net.URLEncoder

object ProjectCoverUrlResolver {

    fun resolve(baseUrl: String, rawUrl: String): String {
        val value = rawUrl.trim()
        if (value.isBlank()) return ""
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return resolveExternalUrl(baseUrl, value)
        }
        if (value.startsWith("/common/file/redirect?path=")) {
            return resolveAgainstBase(baseUrl, value)
        }
        if (value.startsWith("/profile/")) {
            return resolveAgainstBase(baseUrl, value)
        }
        return resolveAgainstBase(baseUrl, "/common/file/redirect?path=${encodeObjectPath(value)}")
    }

    private fun resolveExternalUrl(baseUrl: String, value: String): String {
        val objectPath = runCatching {
            val uri = URI.create(value)
            val host = uri.host.orEmpty().lowercase()
            val query = uri.rawQuery.orEmpty()
            val hasSignature = query.contains("OSSAccessKeyId=") ||
                query.contains("Signature=") ||
                query.contains("Expires=")
            val isOssHost = host.contains("aliyuncs.com") || host.contains(".oss-")
            val objectPath = extractStorageObjectPath(uri.path.orEmpty())
            if (hasSignature && (isOssHost || objectPath != null)) {
                objectPath
            } else {
                null
            }
        }.getOrNull()
        return if (objectPath.isNullOrBlank()) {
            value
        } else {
            resolveAgainstBase(baseUrl, "/common/file/redirect?path=${encodeObjectPath(objectPath)}")
        }
    }

    private fun encodeObjectPath(rawPath: String): String {
        val normalizedPath = rawPath
            .trim()
            .replace('\\', '/')
            .trimStart('/')
            .removePrefix("uploads/")
            .let { path ->
                if (path == rawPath.trim().replace('\\', '/').trimStart('/')) path else "upload/$path"
            }
        return URLEncoder.encode(normalizedPath, Charsets.UTF_8.name())
    }

    private fun extractStorageObjectPath(rawPath: String): String? {
        val path = rawPath.trim().replace('\\', '/').trimStart('/')
        if (path.isBlank()) return null
        if (path.startsWith("uploads/")) return "upload/" + path.removePrefix("uploads/")
        if (isStorageObjectPath(path)) return path
        val prefixes = listOf("/upload/", "/uploads/", "/profile/", "/avatar/", "/download/")
        val match = prefixes.firstOrNull { path.contains(it) } ?: return null
        val objectPath = path.substring(path.indexOf(match) + 1)
        return if (objectPath.startsWith("uploads/")) {
            "upload/" + objectPath.removePrefix("uploads/")
        } else {
            objectPath
        }
    }

    private fun isStorageObjectPath(path: String): Boolean {
        return path.startsWith("upload/") ||
            path.startsWith("uploads/") ||
            path.startsWith("profile/") ||
            path.startsWith("avatar/") ||
            path.startsWith("download/")
    }

    private fun resolveAgainstBase(baseUrl: String, path: String): String {
        return runCatching {
            URL(URL(baseUrl), path).toString()
        }.getOrDefault(path)
    }
}
