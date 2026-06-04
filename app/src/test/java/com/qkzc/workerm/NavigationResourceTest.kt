package com.qkzc.workerm

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationResourceTest {

    @Test
    fun bottomNavigationLabelsMatchCurrentSections() {
        val menu = readMainNavigationMenu()

        assertEquals(listOf("首页", "项目", "成员", "消息", "审批"), menu.titles)
        assertEquals("成员", menu.item("@+id/nav_todo").title)
        assertEquals("审批", menu.item("@+id/nav_profile").title)
    }

    @Test
    fun bottomNavigationIconsMatchCurrentSections() {
        val menu = readMainNavigationMenu()

        assertEquals("@drawable/ic_people", menu.item("@+id/nav_todo").icon)
        assertEquals("@drawable/ic_approval", menu.item("@+id/nav_profile").icon)
    }

    private fun readMainNavigationMenu(): NavigationMenu {
        val file = sequenceOf(
            File("src/main/res/menu/menu_main_navigation.xml"),
            File("app/src/main/res/menu/menu_main_navigation.xml"),
        ).first(File::isFile)
        val strings = readStrings()
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("item")
        val items = buildMap {
            for (index in 0 until nodes.length) {
                val node = nodes.item(index)
                val attributes = node.attributes
                val id = attributes.getNamedItem("android:id").nodeValue
                put(
                    id,
                    NavigationMenuItem(
                        title = resolveString(attributes.getNamedItem("android:title").nodeValue, strings),
                        icon = attributes.getNamedItem("android:icon").nodeValue,
                    ),
                )
            }
        }
        return NavigationMenu(items)
    }

    private fun readStrings(): Map<String, String> {
        val file = sequenceOf(
            File("src/main/res/values/strings.xml"),
            File("app/src/main/res/values/strings.xml"),
        ).first(File::isFile)
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string")
        return buildMap {
            for (index in 0 until nodes.length) {
                val node = nodes.item(index)
                put(node.attributes.getNamedItem("name").nodeValue, node.textContent)
            }
        }
    }

    private fun resolveString(value: String, strings: Map<String, String>): String {
        return if (value.startsWith("@string/")) {
            checkNotNull(strings[value.removePrefix("@string/")])
        } else {
            value
        }
    }

    private data class NavigationMenu(private val items: Map<String, NavigationMenuItem>) {
        val titles: List<String> = items.values.map(NavigationMenuItem::title)

        fun item(id: String): NavigationMenuItem = checkNotNull(items[id])
    }

    private data class NavigationMenuItem(
        val title: String,
        val icon: String,
    )
}
