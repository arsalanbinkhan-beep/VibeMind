package com.arsalankhan.vibemind

import com.google.ai.client.generativeai.GenerativeModel

class GeminiApi(private val apiKey: String) {

    suspend fun getCategorizedSongs(songList: List<String>): String {
        val generativeModel = GenerativeModel(
            // CHANGE THIS LINE
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )

        val prompt = """
            You are a music classifier.
            Classify the following songs into ONLY these genres: Pop, Rock, Epic, Lofi.
            Do NOT create a "For You" category.

            Respond ONLY with a valid JSON object like this:
            {
              "Pop": ["Song A", "Song B"],
              "Rock": ["Song C"],
              "Epic": [],
              "Lofi": []
            }

            Now, classify the following songs:
            ${songList.joinToString("\n")}
        """.trimIndent()

        val response = generativeModel.generateContent(prompt)
        return response.text ?: ""
    }
}