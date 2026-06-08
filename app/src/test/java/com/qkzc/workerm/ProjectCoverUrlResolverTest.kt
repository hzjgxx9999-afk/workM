package com.qkzc.workerm

import com.qkzc.workerm.data.project.ProjectCoverUrlResolver
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectCoverUrlResolverTest {

    @Test
    fun resolvesProfilePathAgainstBackendHost() {
        val actual = ProjectCoverUrlResolver.resolve(
            baseUrl = "http://192.168.3.166:8080/",
            rawUrl = "/profile/upload/2026/06/04/cover.jpg",
        )

        assertEquals("http://192.168.3.166:8080/profile/upload/2026/06/04/cover.jpg", actual)
    }

    @Test
    fun resolvesUploadObjectPathThroughFileRedirect() {
        val actual = ProjectCoverUrlResolver.resolve(
            baseUrl = "http://192.168.3.166:8080/",
            rawUrl = "/uploads/project/p001.jpg",
        )

        assertEquals(
            "http://192.168.3.166:8080/common/file/redirect?path=upload%2Fproject%2Fp001.jpg",
            actual,
        )
    }

    @Test
    fun resolvesBackendRedirectPathAgainstBackendHost() {
        val actual = ProjectCoverUrlResolver.resolve(
            baseUrl = "http://192.168.3.166:8080/",
            rawUrl = "/common/file/redirect?path=upload%2Fproject%2Fp001.jpg",
        )

        assertEquals(
            "http://192.168.3.166:8080/common/file/redirect?path=upload%2Fproject%2Fp001.jpg",
            actual,
        )
    }

    @Test
    fun resolvesSignedOssUrlThroughBackendRedirect() {
        val actual = ProjectCoverUrlResolver.resolve(
            baseUrl = "http://192.168.3.166:8080/",
            rawUrl = "https://bucket.oss-cn-shenzhen.aliyuncs.com/upload/project/p001.jpg" +
                "?OSSAccessKeyId=tmp&Signature=sig&Expires=1",
        )

        assertEquals(
            "http://192.168.3.166:8080/common/file/redirect?path=upload%2Fproject%2Fp001.jpg",
            actual,
        )
    }

    @Test
    fun resolvesPathStyleSignedOssUrlThroughBackendRedirect() {
        val actual = ProjectCoverUrlResolver.resolve(
            baseUrl = "http://192.168.3.166:8080/",
            rawUrl = "https://oss-cn-shenzhen.aliyuncs.com/bucket/upload/project/p001.jpg" +
                "?OSSAccessKeyId=tmp&Signature=sig&Expires=1",
        )

        assertEquals(
            "http://192.168.3.166:8080/common/file/redirect?path=upload%2Fproject%2Fp001.jpg",
            actual,
        )
    }

    @Test
    fun keepsAbsoluteUrlUnchanged() {
        val actual = ProjectCoverUrlResolver.resolve(
            baseUrl = "http://192.168.3.166:8080/",
            rawUrl = "https://cdn.example.com/project/p001.jpg",
        )

        assertEquals("https://cdn.example.com/project/p001.jpg", actual)
    }
}
