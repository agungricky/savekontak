package com.example.wacontactcollector.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reads an explicitly configured, authorized CRM/WhatsApp Business Platform endpoint.
 * This class never opens WhatsApp chat databases or scrapes chat lists.
 *
 * Accepted response formats:
 *   [{"phone":"0812...","name":"Customer"}]
 *   {"customers":[{"phone":"...","name":"..."}]}
 */
class OfficialApiClient {
    fun fetch(endpoint: String, bearerToken: String): List<RemoteCustomer> {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            if (bearerToken.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
        }
        try {
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            return parseCustomers(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseCustomers(body: String): List<RemoteCustomer> {
        val trimmed = body.trim()
        val array = if (trimmed.startsWith("[")) {
            JSONArray(trimmed)
        } else {
            JSONObject(trimmed).optJSONArray("customers") ?: JSONArray()
        }
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val phone = item.optString("phone").ifBlank { item.optString("wa_id") }
                if (phone.isNotBlank()) {
                    add(RemoteCustomer(phone, item.optString("name").takeIf { it.isNotBlank() }))
                }
            }
        }
    }
}
