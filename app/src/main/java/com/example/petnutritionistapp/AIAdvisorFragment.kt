package com.example.petnutritionistapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.create
import org.json.JSONObject
import java.io.IOException
import android.util.Log

class AIAdvisorFragment : Fragment() {

    private lateinit var editInput: EditText
    private lateinit var btnSend: Button
    private lateinit var txtResult: TextView

    private val client = OkHttpClient()
    private val apiKey = BuildConfig.OPENAI_API_KEY

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ai_advisor, container, false)

        editInput = view.findViewById(R.id.editInput)
        btnSend = view.findViewById(R.id.btnSend)
        txtResult = view.findViewById(R.id.txtResult)

        btnSend.setOnClickListener {
            val question = editInput.text.toString().trim()
            if (question.isNotEmpty()) {
                askAI(question)
            } else {
                Toast.makeText(requireContext(), "請輸入問題", Toast.LENGTH_SHORT).show()
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
            put("model", "gpt-3.5-turbo")
            put("messages", messagesArray)
            put("temperature", 0.7)
        }

        val mediaType = "application/json".toMediaType()
        val body = create(mediaType, json.toString())

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("OpenAI", "金鑰內容：$apiKey，長度：${apiKey.length}")

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val responseJson = JSONObject(responseBody)
                    val content = responseJson
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    withContext(Dispatchers.Main) {
                        txtResult.text = content.trim()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        txtResult.text = "請求失敗：${response.code}"
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    txtResult.text = "錯誤：${e.message}"
                }
            }
        }
    }
}
