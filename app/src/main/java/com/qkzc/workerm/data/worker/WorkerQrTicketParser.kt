package com.qkzc.workerm.data.worker

import com.google.gson.JsonParser
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object WorkerQrTicketParser {

    fun parseTicket(content: String?): String? {
        val trimmed = content?.trim().orEmpty()
        if (trimmed.isEmpty()) return null

        parseJsonTicket(trimmed)?.let { return it }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return parseQueryTicket(trimmed.substringAfter("?", missingDelimiterValue = ""))
        }

        if (trimmed.contains("=")) {
            parseQueryTicket(trimmed.substringAfter("?", trimmed))?.let { return it }
        }

        return trimmed
    }

    private fun parseJsonTicket(content: String): String? {
        if (!content.startsWith("{")) return null
        val json = runCatching { JsonParser.parseString(content).asJsonObject }.getOrNull() ?: return null
        return json.get("ticket")?.takeIf { !it.isJsonNull }?.asString?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun parseQueryTicket(query: String): String? {
        if (query.isBlank()) return null
        return query.split("&")
            .asSequence()
            .mapNotNull { part ->
                val index = part.indexOf("=")
                if (index <= 0 || index >= part.length - 1) return@mapNotNull null
                val key = decode(part.substring(0, index))
                val value = decode(part.substring(index + 1)).trim()
                key to value
            }
            .firstOrNull { (key, value) -> key == "ticket" && value.isNotEmpty() }
            ?.second
    }

    private fun decode(value: String): String {
        return URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }
}
