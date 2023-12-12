package com.example.terraforming

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.image.ImageSize
import com.aallam.openai.api.image.ImageURL
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.squareup.picasso.Picasso
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity() {
    private lateinit var etQuestion: EditText
    private lateinit var btnAsk: Button
    private lateinit var ivPicture: ImageView


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        etQuestion = findViewById(R.id.etQuestion)
        btnAsk = findViewById(R.id.btnAsk)
        ivPicture = findViewById(R.id.ivPicture)

        val openAI = OpenAI(
            token = "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"
        )

        btnAsk.setOnClickListener {
            val question = etQuestion.text.toString()

            // CoroutineScope tied to the lifecycle of the activity with Main dispatcher for UI updates
            CoroutineScope(Dispatchers.Main).launch {
                // Use withContext to switch to IO dispatcher for network call
                val images = openAI.imageURL( // or openAI.imageJSON
                    creation = ImageCreation(
                        prompt = question,
                        model = ModelId("dall-e-2"),
                        n = 1,
                        size = ImageSize.is1024x1024
                    )
                )
                Log.i("Mine", images[0].url)
                var url = images[0].url
                Picasso.get().load(url).into(ivPicture)
            }
        }
    }
}
