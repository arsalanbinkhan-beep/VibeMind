package com.arsalankhan.vibemind

import android.content.SharedPreferences
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import com.arsalankhan.vibemind.databinding.ActivityEqualizerBinding
import kotlin.math.roundToInt

class EqualizerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEqualizerBinding
    private var equalizer: Equalizer? = null
    private lateinit var prefs: SharedPreferences

    private val seekBars = mutableListOf<SeekBar>()
    private val gainLabels = mutableListOf<TextView>()
    private var minLevel = 0
    private var maxLevel = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEqualizerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("equalizer_settings", MODE_PRIVATE)

        try {
            // Get audio session ID from PlayerManager
            val audioSessionId = PlayerManager.getAudioSessionId()
            if (audioSessionId == -1) {
                Toast.makeText(this, "Play a song first", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            setupEqualizer(audioSessionId)
            setupPresetButtons()
        } catch (e: Exception) {
            Toast.makeText(this, "Equalizer not supported", Toast.LENGTH_SHORT).show()
            finish()
        }
    }



    private fun setupEqualizer(sessionId: Int) {
        try {
            equalizer = Equalizer(0, sessionId).apply {
                enabled = true
                minLevel = bandLevelRange[0].toInt() / 100  // Convert to dB
                maxLevel = bandLevelRange[1].toInt() / 100  // Convert to dB
            }

            val eq = equalizer ?: return

            binding.equalizerSlidersContainer.removeAllViews()
            seekBars.clear()
            gainLabels.clear()

            // Create frequency bands (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz)
            val frequencies = listOf(60, 230, 910, 3600, 14000)

            for (i in frequencies.indices) {
                val freq = frequencies[i]
                val savedLevel = prefs.getInt("band_$i", 0) // Default to 0 dB

                // Create gain label
                val gainLabel = TextView(this).apply {
                    text = "${savedLevel} dB"
                    setTextColor(ContextCompat.getColor(this@EqualizerActivity, R.color.white))
                    textSize = 12f
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 8.dpToPx(), 0, 8.dpToPx())
                    }
                }

                // Create seekbar
                // Create seekbar
                val seekBar = SeekBar(this).apply {
                    max = maxLevel - minLevel // Range from min to max dB
                    progress = savedLevel - minLevel // Convert to seekbar position

                    // Give a large width since after rotation it becomes height
                    layoutParams = LinearLayout.LayoutParams(
                        200.dpToPx(), // This becomes the visible height after rotation
                        60.dpToPx()   // This becomes the width
                    ).apply {
                        gravity = Gravity.CENTER
                    }

                    rotation = 270f // Make vertical

                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                            if (fromUser) {
                                val dBLevel = progress + minLevel
                                gainLabel.text = "${dBLevel} dB"

                                // Convert back to Android Equalizer units (centibels)
                                val androidLevel = (dBLevel * 100).toShort()
                                eq.setBandLevel(i.toShort(), androidLevel)

                                highlightPreset(binding.btnPresetCustom)
                            }
                        }

                        override fun onStopTrackingTouch(sb: SeekBar?) {
                            val dBLevel = (sb?.progress ?: 0) + minLevel
                            prefs.edit().putInt("band_$i", dBLevel).apply()
                        }

                        override fun onStartTrackingTouch(sb: SeekBar?) {}
                    })
                }


                // Create frequency label
                val freqLabel = TextView(this).apply {
                    text = if (freq >= 1000) "${freq / 1000}kHz" else "${freq}Hz"
                    setTextColor(ContextCompat.getColor(this@EqualizerActivity, R.color.equalizer_text_dark))
                    textSize = 12f
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 8.dpToPx(), 0, 0)
                    }
                }

                // Create column for each band
                val column = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        1f
                    ).apply {
                        gravity = Gravity.CENTER
                    }

                    addView(gainLabel)
                    addView(seekBar)
                    addView(freqLabel)
                }

                seekBars.add(seekBar)
                gainLabels.add(gainLabel)
                binding.equalizerSlidersContainer.addView(column)

                // Set initial level
                val androidLevel = (savedLevel * 100).toShort()
                eq.setBandLevel(i.toShort(), androidLevel)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Equalizer initialization failed", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupPresetButtons() {
        val presets = mapOf(
            binding.btnPresetBalanced to listOf(0, 0, 0, 0, 0),      // Balanced
            binding.btnPresetBassBoost to listOf(12, 8, 0, -4, -8),  // Bass Boost
            binding.btnPresetSmooth to listOf(4, 2, 0, 2, 4),        // Smooth
            binding.btnPresetDynamic to listOf(8, 4, 0, -4, -8),     // Dynamic
            binding.btnPresetClear to listOf(-4, -2, 0, 4, 8),       // Clear
            binding.btnPresetTrebleBoost to listOf(-8, -4, 0, 8, 12) // Treble Boost
        )

        presets.forEach { (button, levels) ->
            button.setOnClickListener {
                applyPreset(levels)
                highlightPreset(button)
            }
        }

        binding.btnPresetCustom.setOnClickListener {
            highlightPreset(it as Button)
        }
    }

    private fun applyPreset(levels: List<Int>) {
        val eq = equalizer ?: return

        levels.forEachIndexed { i, dBLevel ->
            // Clamp the value within min/max range
            val clampedLevel = dBLevel.coerceIn(minLevel, maxLevel)

            // Update seekbar
            seekBars[i].progress = clampedLevel - minLevel

            // Update label
            gainLabels[i].text = "${clampedLevel} dB"

            // Update equalizer (convert to centibels)
            val androidLevel = (clampedLevel * 100).toShort()
            eq.setBandLevel(i.toShort(), androidLevel)

            // Save to preferences
            prefs.edit().putInt("band_$i", clampedLevel).apply()
        }
    }

    private fun highlightPreset(button: Button) {
        // Reset all buttons
        listOf(
            binding.btnPresetBalanced, binding.btnPresetBassBoost,
            binding.btnPresetSmooth, binding.btnPresetDynamic,
            binding.btnPresetClear, binding.btnPresetTrebleBoost,
            binding.btnPresetCustom
        ).forEach {
            it.setBackgroundColor(ContextCompat.getColor(this, R.color.equalizer_preset_button))
            it.setTextColor(ContextCompat.getColor(this, R.color.white))
        }

        // Highlight selected button
        button.setBackgroundColor(ContextCompat.getColor(this, R.color.equalizer_custom_button))
        button.setTextColor(ContextCompat.getColor(this, R.color.black))
    }

    private fun Int.dpToPx(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            resources.displayMetrics
        ).roundToInt()
    }

    override fun onDestroy() {
        equalizer?.release()
        super.onDestroy()
    }
}