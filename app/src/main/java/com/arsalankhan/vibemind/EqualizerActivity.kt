package com.arsalankhan.vibemind

import android.content.SharedPreferences
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.arsalankhan.vibemind.databinding.ActivityEqualizerBinding
import kotlin.math.roundToInt

class EqualizerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEqualizerBinding
    private var equalizer: Equalizer? = null
    private lateinit var prefs: SharedPreferences

    private val seekBars = mutableListOf<SeekBar>()
    private val gainLabels = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEqualizerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("equalizer_settings", MODE_PRIVATE)


        val audioSessionId = intent.getIntExtra("AUDIO_SESSION_ID", -1)
        if (audioSessionId == -1) {
            Toast.makeText(this, "Play a song first.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupEqualizer(audioSessionId)
        setupPresetButtons()
    }

    private fun setupEqualizer(sessionId: Int) {
        try {
            equalizer = Equalizer(0, sessionId).apply { enabled = true }

            val eq = equalizer ?: return
            val lower = eq.bandLevelRange[0].toInt()
            val upper = eq.bandLevelRange[1].toInt()
            val range = upper - lower

            binding.equalizerSlidersContainer.removeAllViews()
            seekBars.clear()
            gainLabels.clear()

            // Min dB
            binding.equalizerSlidersContainer.addView(makeDbLabel("${lower / 100} dB"))

            for (i in 0 until eq.numberOfBands) {
                val band = i.toShort()
                val center = eq.getCenterFreq(band) / 1000
                val savedLevel = prefs.getInt("band_$i", eq.getBandLevel(band).toInt())

                val gainLabel = makeText("${savedLevel / 100} dB", 14)
                val seekBar = makeSeekBar(range, savedLevel - lower).apply {
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                            if (fromUser) {
                                val newLevel = (progress + lower).toShort()
                                eq.setBandLevel(band, newLevel)
                                gainLabel.text = "${newLevel / 100} dB"
                                highlightPreset(binding.btnPresetCustom)
                            }
                        }

                        override fun onStopTrackingTouch(sb: SeekBar?) {
                            val newLevel = ((sb?.progress ?: 0) + lower).toShort()
                            prefs.edit().putInt("band_$i", newLevel.toInt()).apply()
                        }

                        override fun onStartTrackingTouch(sb: SeekBar?) {}
                    })
                }

                val freqLabel = makeText("${center}Hz", 12)

                val column = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                    addView(gainLabel)
                    addView(seekBar)
                    addView(freqLabel)
                }

                seekBars += seekBar
                gainLabels += gainLabel
                binding.equalizerSlidersContainer.addView(column)
            }

            // Max dB
            binding.equalizerSlidersContainer.addView(makeDbLabel("${upper / 100} dB"))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Equalizer init failed.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupPresetButtons() {
        val eq = equalizer ?: return
        val (min, max) = eq.bandLevelRange
        val mid = ((min + max) / 2).toShort()

        val presets = mapOf(
            getString(R.string.equalizer_preset_balanced) to List(seekBars.size) { mid },
            getString(R.string.equalizer_preset_bass_boost) to List(seekBars.size) { i ->
                when (i) { 0 -> (max * 0.7).toInt().toShort(); 1 -> (max * 0.5).toInt().toShort(); else -> mid }
            },
            getString(R.string.equalizer_preset_smooth) to List(seekBars.size) { i ->
                when (i) { 0 -> (mid * 0.8).toInt().toShort(); seekBars.size - 1 -> (max * 0.8).toInt().toShort(); else -> mid }
            },
            getString(R.string.equalizer_preset_dynamic) to List(seekBars.size) { i ->
                when (i) { 0 -> (max * 0.6).toInt().toShort(); seekBars.size - 1 -> (max * 0.7).toInt().toShort(); else -> (min * 0.5).toInt().toShort() }
            },
            getString(R.string.equalizer_preset_clear) to List(seekBars.size) { i ->
                when (i) { 0 -> (min * 0.6).toInt().toShort(); 1 -> (min * 0.4).toInt().toShort(); seekBars.size - 1 -> (max * 0.9).toInt().toShort(); else -> mid }
            },
            getString(R.string.equalizer_preset_treble_boost) to List(seekBars.size) { i ->
                when (i) { seekBars.size - 1 -> (max * 0.7).toInt().toShort(); seekBars.size - 2 -> (max * 0.5).toInt().toShort(); else -> mid }
            }
        )

        fun bind(btn: Button, preset: String) {
            btn.setOnClickListener { applyPreset(preset, presets[preset]!!) }
        }

        with(binding) {
            bind(btnPresetBalanced, getString(R.string.equalizer_preset_balanced))
            bind(btnPresetBassBoost, getString(R.string.equalizer_preset_bass_boost))
            bind(btnPresetSmooth, getString(R.string.equalizer_preset_smooth))
            bind(btnPresetDynamic, getString(R.string.equalizer_preset_dynamic))
            bind(btnPresetClear, getString(R.string.equalizer_preset_clear))
            bind(btnPresetTrebleBoost, getString(R.string.equalizer_preset_treble_boost))
            btnPresetCustom.setOnClickListener { highlightPreset(btnPresetCustom) }
        }
    }

    private fun applyPreset(name: String, levels: List<Short>) {
        val eq = equalizer ?: return
        val lower = eq.bandLevelRange[0].toInt()

        levels.forEachIndexed { i, level ->
            seekBars[i].progress = level.toInt() - lower
            eq.setBandLevel(i.toShort(), level)
            gainLabels[i].text = "${level / 100} dB"
            prefs.edit().putInt("band_$i", level.toInt()).apply()
        }

        resetButtonColors()
        when (name) {
            getString(R.string.equalizer_preset_balanced) -> highlightPreset(binding.btnPresetBalanced)
            getString(R.string.equalizer_preset_bass_boost) -> highlightPreset(binding.btnPresetBassBoost)
            getString(R.string.equalizer_preset_smooth) -> highlightPreset(binding.btnPresetSmooth)
            getString(R.string.equalizer_preset_dynamic) -> highlightPreset(binding.btnPresetDynamic)
            getString(R.string.equalizer_preset_clear) -> highlightPreset(binding.btnPresetClear)
            getString(R.string.equalizer_preset_treble_boost) -> highlightPreset(binding.btnPresetTrebleBoost)
        }
    }

    // --- Helpers ---
    private fun makeSeekBar(max: Int, progress: Int) = SeekBar(this).apply {
        this.max = max
        this.progress = progress
        thumb = ContextCompat.getDrawable(context, R.drawable.equalizer_thumb)
        progressDrawable = ContextCompat.getDrawable(context, R.drawable.equalizer_track)
        rotation = 270f

        layoutParams = LinearLayout.LayoutParams(
            80.dpToPx(), // thickness of each vertical bar
            0,           // height controlled by weight
            1f           // fill available vertical space
        )

        // Force min size so it doesn't disappear after rotation
        minimumWidth = 80.dpToPx()
        minimumHeight = 500.dpToPx()
    }

    private fun makeText(text: String, sp: Int) = TextView(this).apply {
        this.text = text
        setTextColor(ContextCompat.getColor(this@EqualizerActivity, R.color.equalizer_text_dark))
        textSize = sp.toFloat()
        gravity = Gravity.CENTER_HORIZONTAL
    }

    private fun makeDbLabel(text: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        addView(makeText(text, 12))
    }

    private fun highlightPreset(button: Button) {
        button.background = ContextCompat.getDrawable(this, R.drawable.button_background_custom)
        button.setTextColor(ContextCompat.getColor(this, R.color.equalizer_custom_button_text))
    }

    private fun resetButtonColors() {
        listOf(
            binding.btnPresetBalanced, binding.btnPresetBassBoost,
            binding.btnPresetSmooth, binding.btnPresetDynamic,
            binding.btnPresetClear, binding.btnPresetTrebleBoost,
            binding.btnPresetCustom
        ).forEach {
            it.background = ContextCompat.getDrawable(this, R.drawable.button_background_preset)
            it.setTextColor(ContextCompat.getColor(this, R.color.equalizer_preset_button_text))
        }
    }

    private fun Int.dpToPx() = (this * resources.displayMetrics.density).roundToInt()

    override fun onDestroy() {
        equalizer?.release()
        super.onDestroy()
    }
}
