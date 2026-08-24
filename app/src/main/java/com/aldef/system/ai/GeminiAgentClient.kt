package com.aldef.system.ai

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Client untuk berkomunikasi dengan Google Gemini API (gemini-3.7-flash)
 * Menggunakan Function Calling untuk kontrol perangkat Android.
 */
class GeminiAgentClient(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.7-flash:generateContent?key=$apiKey"

    suspend fun processVoiceCommand(userInput: String): AgentResponse = withContext(Dispatchers.IO) {
        val payload = buildRequestPayload(userInput)
        val body = payload.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(endpoint)
            .post(body)
            .header("User-Agent", "aistudio-build")
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            parseGeminiResponse(responseBody)
        } catch (e: Exception) {
            AgentResponse(
                replyText = "Maaf, terjadi kendala saat memproses: ${e.message}",
                functionCalls = emptyList()
            )
        }
    }

    private fun buildRequestPayload(userPrompt: String): JsonObject {
        val root = JsonObject()

        // 1. System Instruction
        val systemInstruction = JsonObject()
        val sysParts = JsonArray()
        val sysPart = JsonObject()
        sysPart.addProperty("text", "Anda adalah Aldef Voice AI Agent untuk smartphone Android. " +
                "Tugas Anda memetakan bahasa natural pengguna ke tool smartphone Android yang sesuai (open_app, toggle_system, send_whatsapp, set_volume, set_alarm, create_note, search_youtube). " +
                "Berikan respon teks singkat dan ramah dalam Bahasa Indonesia.")
        sysParts.add(sysPart)
        systemInstruction.add("parts", sysParts)
        root.add("systemInstruction", systemInstruction)

        // 2. Contents
        val contents = JsonArray()
        val userContent = JsonObject()
        userContent.addProperty("role", "user")
        val userParts = JsonArray()
        val userPart = JsonObject()
        userPart.addProperty("text", userPrompt)
        userParts.add(userPart)
        userContent.add("parts", userParts)
        contents.add(userContent)
        root.add("contents", contents)

        // 3. Tools / Function Declarations
        val toolsArray = JsonArray()
        val toolObj = JsonObject()
        val declarationsArray = JsonArray()
        declarationsArray.add(buildOpenAppTool())
        declarationsArray.add(buildToggleSystemTool())
        declarationsArray.add(buildSendWhatsAppTool())
        declarationsArray.add(buildSetVolumeTool())
        declarationsArray.add(buildSetAlarmTool())
        declarationsArray.add(buildCreateNoteTool())
        declarationsArray.add(buildSearchYouTubeTool())
        toolObj.add("functionDeclarations", declarationsArray)
        toolsArray.add(toolObj)
        root.add("tools", toolsArray)

        return root
    }

    private fun buildOpenAppTool(): JsonObject {
        val tool = JsonObject()
        tool.addProperty("name", "open_app")
        tool.addProperty("description", "Membuka aplikasi Android (whatsapp, youtube, settings, camera, spotify, dll)")
        val params = JsonObject()
        params.addProperty("type", "OBJECT")
        val props = JsonObject()
        val prop = JsonObject()
        prop.addProperty("type", "STRING")
        prop.addProperty("description", "Nama aplikasi")
        props.add("appName", prop)
        params.add("properties", props)
        val req = JsonArray()
        req.add("appName")
        params.add("required", req)
        tool.add("parameters", params)
        return tool
    }

    private fun buildToggleSystemTool(): JsonObject {
        val tool = JsonObject()
        tool.addProperty("name", "toggle_system")
        tool.addProperty("description", "Mengontrol senter (flashlight), wifi, bluetooth, dnd, dll")
        val params = JsonObject()
        params.addProperty("type", "OBJECT")
        val props = JsonObject()
        val feat = JsonObject()
        feat.addProperty("type", "STRING")
        feat.addProperty("description", "flashlight / wifi / bluetooth")
        props.add("feature", feat)
        val state = JsonObject()
        state.addProperty("type", "STRING")
        state.addProperty("description", "on / off / toggle")
        props.add("state", state)
        params.add("properties", props)
        val req = JsonArray()
        req.add("feature")
        req.add("state")
        params.add("required", req)
        tool.add("parameters", params)
        return tool
    }

    private fun buildSendWhatsAppTool(): JsonObject {
        val tool = JsonObject()
        tool.addProperty("name", "send_whatsapp")
        tool.addProperty("description", "Mengirim pesan WhatsApp ke nomor atau kontak")
        val params = JsonObject()
        params.addProperty("type", "OBJECT")
        val props = JsonObject()
        val rec = JsonObject()
        rec.addProperty("type", "STRING")
        props.add("recipient", rec)
        val msg = JsonObject()
        msg.addProperty("type", "STRING")
        props.add("message", msg)
        params.add("properties", props)
        val req = JsonArray()
        req.add("recipient")
        req.add("message")
        params.add("required", req)
        tool.add("parameters", params)
        return tool
    }

    private fun buildSetVolumeTool(): JsonObject {
        val tool = JsonObject()
        tool.addProperty("name", "set_volume")
        tool.addProperty("description", "Mengatur level volume 0-100%")
        val params = JsonObject()
        params.addProperty("type", "OBJECT")
        val props = JsonObject()
        val lvl = JsonObject()
        lvl.addProperty("type", "INTEGER")
        props.add("level", lvl)
        params.add("properties", props)
        val req = JsonArray()
        req.add("level")
        params.add("required", req)
        tool.add("parameters", params)
        return tool
    }

    private fun buildSetAlarmTool(): JsonObject {
        val tool = JsonObject()
        tool.addProperty("name", "set_alarm")
        tool.addProperty("description", "Memasang alarm waktu HH:mm")
        val params = JsonObject()
        params.addProperty("type", "OBJECT")
        val props = JsonObject()
        val time = JsonObject()
        time.addProperty("type", "STRING")
        props.add("time", time)
        val label = JsonObject()
        label.addProperty("type", "STRING")
        props.add("label", label)
        params.add("properties", props)
        val req = JsonArray()
        req.add("time")
        params.add("required", req)
        tool.add("parameters", params)
        return tool
    }

    private fun buildCreateNoteTool(): JsonObject {
        val tool = JsonObject()
        tool.addProperty("name", "create_note")
        tool.addProperty("description", "Membuat catatan atau memo baru")
        val params = JsonObject()
        params.addProperty("type", "OBJECT")
        val props = JsonObject()
        val title = JsonObject()
        title.addProperty("type", "STRING")
        props.add("title", title)
        val content = JsonObject()
        content.addProperty("type", "STRING")
        props.add("content", content)
        params.add("properties", props)
        val req = JsonArray()
        req.add("title")
        params.add("required", req)
        tool.add("parameters", params)
        return tool
    }

    private fun buildSearchYouTubeTool(): JsonObject {
        val tool = JsonObject()
        tool.addProperty("name", "search_youtube")
        tool.addProperty("description", "Mencari video atau lagu di YouTube")
        val params = JsonObject()
        params.addProperty("type", "OBJECT")
        val props = JsonObject()
        val q = JsonObject()
        q.addProperty("type", "STRING")
        q.addProperty("description", "Kata kunci pencarian")
        props.add("query", q)
        params.add("properties", props)
        val req = JsonArray()
        req.add("query")
        params.add("required", req)
        tool.add("parameters", params)
        return tool
    }

    private fun parseGeminiResponse(json: String): AgentResponse {
        return try {
            val root = gson.fromJson(json, JsonObject::class.java) ?: return AgentResponse("Respon kosong dari server.", emptyList())
            val candidates = root.getAsJsonArray("candidates") ?: return AgentResponse("Tidak ada respon", emptyList())
            if (candidates.size() == 0) return AgentResponse("Tidak ada respon", emptyList())
            val firstCandidate = candidates.get(0).asJsonObject
            val content = firstCandidate.getAsJsonObject("content") ?: return AgentResponse("Respon kosong", emptyList())
            val parts = content.getAsJsonArray("parts") ?: return AgentResponse("Part kosong", emptyList())

            var replyText = ""
            val functionCalls = mutableListOf<ActionCall>()

            for (i in 0 until parts.size()) {
                val element = parts.get(i)
                if (element != null && element.isJsonObject) {
                    val partObj = element.asJsonObject
                    if (partObj.has("text")) {
                        replyText += partObj.get("text").asString
                    }
                    if (partObj.has("functionCall")) {
                        val callObj = partObj.getAsJsonObject("functionCall")
                        val name = callObj.get("name").asString
                        val args = if (callObj.has("args") && callObj.get("args").isJsonObject) {
                            callObj.getAsJsonObject("args")
                        } else {
                            JsonObject()
                        }
                        functionCalls.add(ActionCall(name, args))
                    }
                }
            }

            AgentResponse(replyText.ifEmpty { "Siap, perintah sedang dijalankan." }, functionCalls)
        } catch (e: Exception) {
            AgentResponse("Kendala: ${e.message}", emptyList())
        }
    }
}

data class ActionCall(val name: String, val args: JsonObject)
data class AgentResponse(val replyText: String, val functionCalls: List<ActionCall>)