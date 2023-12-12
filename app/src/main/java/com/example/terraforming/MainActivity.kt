package com.example.terraforming

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() {
    private lateinit var etQuestion: EditText
    private lateinit var tvReply: TextView
    private lateinit var btnAsk: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        etQuestion = findViewById(R.id.etQuestion)
        tvReply = findViewById(R.id.tvReply)
        btnAsk = findViewById(R.id.btnAsk)

        val openai = OpenAI(
            token = "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"
        )

        btnAsk.setOnClickListener {
            val question = etQuestion.text.toString()
            Toast.makeText(this, question, Toast.LENGTH_SHORT).show()

            // CoroutineScope tied to the lifecycle of the activity with Main dispatcher for UI updates
            CoroutineScope(Dispatchers.Main).launch {
                // Use withContext to switch to IO dispatcher for network call
                val completion: ChatCompletion = withContext(Dispatchers.IO) {
                    val chatCompletionRequest = ChatCompletionRequest(
                        model = ModelId("gpt-3.5-turbo"),
                        messages = listOf(
                            ChatMessage(
                                role = ChatRole.System,
                                content = "You are a helpful assistant!"
                            ),
                            ChatMessage(
                                role = ChatRole.User,
                                content = question
                            )
                        )
                    )
                    // Make the network call to the API
                    openai.chatCompletion(chatCompletionRequest)
                }

                // Update the UI with the API response
                tvReply.text = completion.choices?.joinToString { it.message.content ?: "" }

            }
        }
    }
}