package com.example.petnutritionistapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class AIAdvisorFragment : Fragment() {

    private lateinit var editInput: EditText
    private lateinit var btnSend: Button
    private lateinit var txtResult: TextView

    private val client = OkHttpClient()
    private val apiKey = BuildConfig.OPENAI_API_KEY

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_ai_advisor, container, false)

        editInput = view.findViewById(R.id.editInput)
        btnSend = view.findViewById(R.id.btnSend)
        txtResult = view.findViewById(R.id.txtResult)

        btnSend.setOnClickListener {
            val question = editInput.text.toString().trim()
            if (question.isEmpty()) {
                Toast.makeText(requireContext(), "請輸入問題", Toast.LENGTH_SHORT).show()
            } else if (apiKey.isNullOrBlank()) {
                Toast.makeText(requireContext(), "找不到 API 金鑰，請確認 local.properties", Toast.LENGTH_SHORT).show()
            } else {
                askAI(question)
            }
        }

        return view
    }

    private fun askAI(question: String) {
        val url = "https://api.openai.com/v1/chat/completions"

        val messagesArray = org.json.JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", "你是專業的狗狗營養顧問")
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", question)
            })
        }

        val json = JSONObject().apply {
            put("model", "gpt-3.5-turbo")   // 你的原設定，保留
            put("messages", messagesArray)
            put("temperature", 0.7)
        }

        val body = json.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        // 用 viewLifecycleOwner.lifecycleScope 避免 Fragment lifecycle 洩漏
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                        val content = JSONObject(responseBody)
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                            .trim()

                        withContext(Dispatchers.Main) {
                            txtResult.text = content
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            txtResult.text = "請求失敗：${response.code} ${response.message}"
                        }
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    txtResult.text = "網路錯誤：${e.message ?: "unknown"}"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    txtResult.text = "發生例外：${e.message ?: "unknown"}"
                }
            }
        }
    }
}
