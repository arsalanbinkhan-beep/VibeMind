package com.arsalankhan.vibemind

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arsalankhan.vibemind.databinding.ActivityLicensesBinding

class LicensesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLicensesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLicensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Simple example: set static text
        binding.tvLicenses.text = """
            VibeMind uses the following open-source libraries:

            • Glide - Image Loading
            • ExoPlayer - Media Playback
            • Retrofit - Networking
            • Gson - JSON Parsing
            • Gemni - For The Categories Section

            Licenses are available on their respective GitHub repositories.
        """.trimIndent()
    }
}
