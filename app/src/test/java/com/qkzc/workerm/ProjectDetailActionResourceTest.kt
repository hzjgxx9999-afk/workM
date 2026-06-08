package com.qkzc.workerm

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document

class ProjectDetailActionResourceTest {

    @Test
    fun projectDetailActionCardsExposeClickTargets() {
        val detail = readXml("res/layout/activity_project_detail.xml")
        val ids = detail.androidAttributeValues("id")

        assertTrue(ids.contains("@+id/project_materials_action"))
        assertTrue(ids.contains("@+id/drawing_docs_action"))
        assertTrue(ids.contains("@+id/construction_log_action"))
        assertTrue(ids.contains("@+id/project_report_action"))
    }

    @Test
    fun projectSubpageActivitiesAreDeclared() {
        val manifest = readXml("AndroidManifest.xml")
        val activities = manifest.androidAttributeValues("name")

        assertTrue(activities.contains(".ui.project.ProjectMaterialsActivity"))
        assertTrue(activities.contains(".ui.video.DrawingDocsActivity"))
        assertTrue(activities.contains(".ui.project.ConstructionLogActivity"))
        assertTrue(activities.contains(".ui.project.ProjectReportActivity"))
    }

    @Test
    fun projectSubpageLayoutsHaveExpectedSections() {
        assertLayoutContains(
            "activity_project_materials.xml",
            listOf("项目资料", "基本信息", "合同资料", "查看合同"),
        )
        assertLayoutContains(
            "activity_drawing_docs.xml",
            listOf("图纸文档", "图纸预览", "版本记录", "上传图纸"),
        )
        assertLayoutContains(
            "activity_construction_log.xml",
            listOf("施工日志", "今日记录", "日志列表", "新增日志"),
        )
        assertLayoutContains(
            "activity_project_report.xml",
            listOf("导出报表", "数据统计", "报表类型", "立即导出"),
        )
    }

    private fun assertLayoutContains(name: String, expectedTexts: List<String>) {
        val text = readResource("res/layout/$name").readText()
        expectedTexts.forEach { expected ->
            assertTrue("$name should contain $expected", text.contains(expected))
        }
    }

    private fun readXml(relativePath: String): Document {
        val file = readResource(relativePath)
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
    }

    private fun readResource(relativePath: String): File {
        return sequenceOf(
            File("src/main/$relativePath"),
            File("app/src/main/$relativePath"),
        ).firstOrNull(File::isFile) ?: error("Missing resource file: $relativePath")
    }

    private fun Document.androidAttributeValues(name: String): List<String> {
        val values = mutableListOf<String>()
        val nodes = getElementsByTagName("*")
        for (index in 0 until nodes.length) {
            val attributes = nodes.item(index).attributes ?: continue
            val value = attributes.getNamedItem("android:$name")?.nodeValue ?: continue
            values += value
        }
        return values
    }
}
