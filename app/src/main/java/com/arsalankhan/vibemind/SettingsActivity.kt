package com.arsalankhan.vibemind

import android.app.ActivityOptions
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.media3.common.Player
import com.arsalankhan.vibemind.databinding.ActivitySettingsBinding
import java.io.File

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    // ✅ Modern folder picker (replaces startActivityForResult/onActivityResult)
    private val folderPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                Toast.makeText(this, "Folder selected: $uri", Toast.LENGTH_LONG).show()
            }
        }

    // 💡 NEW: A listener to react to player state changes
    private val playerStateListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateEqualizerButtonState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("app_settings", MODE_PRIVATE)

        setupBackButton()
        setupAudioQualitySettings()
        setupCrossfadeSettings()
        setupPlaybackSettings()
        setupLibrarySettings()
        setupStorageCacheSettings()
        setupNotificationSettings()
        setupHeadphoneSettings()
        setupAboutSection()
        setupEqualizerSettings() // This is now a one-time setup
    }

    // 💡 NEW: Register the listener when the activity becomes visible
    override fun onStart() {
        super.onStart()
        PlayerManager.getPlayer()?.addListener(playerStateListener)
        updateEqualizerButtonState()
    }

    // 💡 NEW: Unregister the listener when the activity is not visible
    override fun onStop() {
        super.onStop()
        PlayerManager.getPlayer()?.removeListener(playerStateListener)
    }

    // 💡 NEW: A dedicated function to update the button's state
    private fun updateEqualizerButtonState() {
        val audioSessionId = PlayerManager.getAudioSessionId()
        if (audioSessionId == -1) {
            binding.radioCustomEqualizer.isEnabled = false
            binding.radioCustomEqualizer.text = getString(R.string.setting_equalizer_custom_disabled)
        } else {
            binding.radioCustomEqualizer.isEnabled = true
            binding.radioCustomEqualizer.text = getString(R.string.setting_equalizer_custom)
        }
    }

    // 🔙 Back button
    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(
                this,
                R.anim.slide_from_right,
                R.anim.slide_to_left
            )
            startActivity(intent, options.toBundle())
            finish()
        }
    }

    // 💡 MODIFIED: The equalizer setup now only handles the click logic
    private fun setupEqualizerSettings() {
        val equalizerPref = prefs.getInt("equalizer_preference", 0)
        if (equalizerPref == 0) {
            binding.radioSystemEqualizer.isChecked = true
        } else {
            binding.radioCustomEqualizer.isChecked = true
        }

        // This will now use the up-to-date state from the listener
        binding.radioGroupEqualizer.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioSystemEqualizer -> {
                    prefs.edit { putInt("equalizer_preference", 0) }
                    val intent = Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(android.media.audiofx.AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                        putExtra(android.media.audiofx.AudioEffect.EXTRA_AUDIO_SESSION, PlayerManager.getAudioSessionId())
                    }
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "System equalizer not found.", Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.radioCustomEqualizer -> {
                    if (binding.radioCustomEqualizer.isEnabled) {
                        prefs.edit { putInt("equalizer_preference", 1) }
                        val audioSessionId = PlayerManager.getAudioSessionId()
                        val intent = Intent(this, EqualizerActivity::class.java).apply {
                            putExtra("AUDIO_SESSION_ID", audioSessionId)
                        }
                        startActivity(intent)
                    } else {
                        // Re-select the system equalizer if the custom one is disabled
                        binding.radioSystemEqualizer.isChecked = true
                    }
                }
            }
        }
    }

    // 🎵 Audio Quality
    private fun setupAudioQualitySettings() {
        val audioQualityAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.audio_quality_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerAudioQuality.adapter = audioQualityAdapter
        binding.spinnerAudioQuality.setSelection(prefs.getInt("audio_quality", 1))
        binding.spinnerAudioQuality.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                prefs.edit { putInt("audio_quality", pos) }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // 🎚 Crossfade
    private fun setupCrossfadeSettings() {
        binding.seekBarCrossfade.progress = prefs.getInt("crossfade_duration", 0)
        updateCrossfadeText(binding.seekBarCrossfade.progress)

        binding.seekBarCrossfade.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                updateCrossfadeText(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                prefs.edit { putInt("crossfade_duration", sb?.progress ?: 0) }
            }
        })
    }

    private fun updateCrossfadeText(seconds: Int) {
        binding.tvCrossfadeDuration.text = if (seconds == 0) {
            getString(R.string.setting_crossfade_duration_default)
        } else {
            resources.getQuantityString(R.plurals.seconds, seconds, seconds)
        }
    }

    // ▶ Playback
    private fun setupPlaybackSettings() {
        val shakeEnabled = prefs.getBoolean("shake_to_change", false)
        val shakeSensitivity = prefs.getInt("shake_sensitivity", 50)

        binding.switchShakeToChangeSong.isChecked = shakeEnabled
        binding.seekBarShakeSensitivity.progress = shakeSensitivity
        binding.tvShakeSensitivity.text = getString(R.string.shake_sensitivity, shakeSensitivity)

        binding.seekBarShakeSensitivity.visibility = if (shakeEnabled) View.VISIBLE else View.GONE
        binding.tvShakeSensitivity.visibility = if (shakeEnabled) View.VISIBLE else View.GONE

        binding.switchGaplessPlayback.isChecked = prefs.getBoolean("gapless_playback", true)
        binding.switchGaplessPlayback.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("gapless_playback", checked) }
        }

        binding.switchReplayGain.isChecked = prefs.getBoolean("replay_gain", false)
        binding.switchReplayGain.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("replay_gain", checked) }
        }

        binding.switchShakeToChangeSong.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("shake_to_change", checked) }
            binding.seekBarShakeSensitivity.visibility = if (checked) View.VISIBLE else View.GONE
            binding.tvShakeSensitivity.visibility = if (checked) View.VISIBLE else View.GONE
        }

        binding.seekBarShakeSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvShakeSensitivity.text = getString(R.string.shake_sensitivity, progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                prefs.edit { putInt("shake_sensitivity", sb?.progress ?: 50) }
            }
        })
    }

    // 📂 Library
    private fun setupLibrarySettings() {
        binding.switchAutoScan.isChecked = prefs.getBoolean("auto_scan", true)
        binding.switchAutoScan.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("auto_scan", checked) }
        }

        val sortOrderAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.sort_order_options,
            android.R.layout.simple_spinner_item
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerSortOrder.adapter = sortOrderAdapter
        binding.spinnerSortOrder.setSelection(prefs.getInt("sort_order", 0))
        binding.spinnerSortOrder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                prefs.edit { putInt("sort_order", pos) }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.switchShowHiddenAudio.isChecked = prefs.getBoolean("show_hidden", false)
        binding.switchShowHiddenAudio.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("show_hidden", checked) }
        }

        binding.btnScanLocalMusic.setOnClickListener {
            val cursor = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media.TITLE),
                MediaStore.Audio.Media.IS_MUSIC + "!= 0",
                null,
                when (prefs.getInt("sort_order", 0)) {
                    0 -> MediaStore.Audio.Media.TITLE + " ASC"
                    1 -> MediaStore.Audio.Media.ARTIST + " ASC"
                    2 -> MediaStore.Audio.Media.DATE_ADDED + " DESC"
                    3 -> "CASE WHEN ${MediaStore.Audio.Media.DURATION} > 0 THEN 1 ELSE 0 END DESC"
                    else -> MediaStore.Audio.Media.TITLE + " ASC"
                }
            )
            val count = cursor?.count ?: 0
            cursor?.close()
            Toast.makeText(this, "Found $count songs", Toast.LENGTH_LONG).show()
        }

        binding.btnChooseFolders.setOnClickListener {
            folderPicker.launch(null)
        }
    }

    // 🗑 Storage/Cache
    private fun setupStorageCacheSettings() {
        binding.btnClearLastPlayed.setOnClickListener {
            val lastPlayedPrefs = getSharedPreferences("LAST_PLAYED", MODE_PRIVATE)
            lastPlayedPrefs.edit().clear().apply()
            Toast.makeText(this, "Last played songs cleared", Toast.LENGTH_SHORT).show()
        }

        binding.btnClearAlbumArtCache.setOnClickListener {
            clearCacheDir(cacheDir)
            clearCacheDir(externalCacheDir)
            Toast.makeText(this, "Album art cache cleared", Toast.LENGTH_SHORT).show()
        }

        binding.btnClearHistory.setOnClickListener {
            prefs.edit { remove("playback_history") }
            Toast.makeText(this, "Playback history cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearCacheDir(dir: File?) {
        dir?.listFiles()?.forEach { it.deleteRecursively() }
    }

    // 🔔 Notifications
    private fun setupNotificationSettings() {
        binding.switchShowMediaControls.isChecked = prefs.getBoolean("show_media_controls", true)
        binding.switchShowMediaControls.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("show_media_controls", checked) }
            if (checked && PlayerManager.currentSong != null) {
                NotificationManager.updateNotification(this@SettingsActivity)
            } else {
                NotificationManager.stopNotificationService(this@SettingsActivity)
            }
        }

        val style = prefs.getInt("notification_style", 0)
        if (style == 0) binding.radioCompactStyle.isChecked = true else binding.radioExpandedStyle.isChecked = true
        binding.radioGroupNotificationStyle.setOnCheckedChangeListener { _, id ->
            val newStyle = if (id == R.id.radioCompactStyle) 0 else 1
            prefs.edit { putInt("notification_style", newStyle) }
            if (PlayerManager.currentSong != null) {
                NotificationManager.updateNotification(this@SettingsActivity)
            }
        }
    }

    // 🎧 Headphones
    private fun setupHeadphoneSettings() {
        val resumeOnHeadphone = prefs.getBoolean("resume_on_headphone", true)
        val pauseOnDisconnect = prefs.getBoolean("pause_on_headphone_disconnect", true)
        val bluetoothAutoPlay = prefs.getBoolean("bluetooth_autoplay", true)

        binding.switchResumeOnHeadphonePlug.isChecked = resumeOnHeadphone
        binding.switchPauseOnHeadphoneDisconnect.isChecked = pauseOnDisconnect
        binding.switchBluetoothAutoPlay.isChecked = bluetoothAutoPlay

        if (resumeOnHeadphone) {
            HeadphoneControls.enableResumeOnPlug(this)
        } else {
            HeadphoneControls.disableResumeOnPlug(this)
        }

        if (pauseOnDisconnect) {
            HeadphoneControls.enablePauseOnUnplug(this)
        } else {
            HeadphoneControls.disablePauseOnUnplug(this)
        }

        if (bluetoothAutoPlay) {
            HeadphoneControls.enableBluetoothAutoPlay(this)
        } else {
            HeadphoneControls.disableBluetoothAutoPlay(this)
        }

        binding.switchResumeOnHeadphonePlug.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("resume_on_headphone", checked) }
            if (checked) {
                HeadphoneControls.enableResumeOnPlug(this@SettingsActivity)
            } else {
                HeadphoneControls.disableResumeOnPlug(this@SettingsActivity)
            }
        }

        binding.switchPauseOnHeadphoneDisconnect.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("pause_on_headphone_disconnect", checked) }
            if (checked) {
                HeadphoneControls.enablePauseOnUnplug(this@SettingsActivity)
            } else {
                HeadphoneControls.disablePauseOnUnplug(this@SettingsActivity)
            }
        }

        binding.switchBluetoothAutoPlay.setOnCheckedChangeListener { _, checked ->
            prefs.edit { putBoolean("bluetooth_autoplay", checked) }
            if (checked) {
                HeadphoneControls.enableBluetoothAutoPlay(this@SettingsActivity)
            } else {
                HeadphoneControls.disableBluetoothAutoPlay(this@SettingsActivity)
            }
        }
    }

    // ℹ About
    private fun setupAboutSection() {
        binding.tvAppVersion.text = getString(R.string.about_app_version, "1.0.0")
        binding.tvDeveloperInfo.text = getString(R.string.about_developer_info)

        binding.tvRateApp.setOnClickListener {
            val uri = Uri.parse("market://details?id=$packageName")
            val goToMarket = Intent(Intent.ACTION_VIEW, uri)
            try { startActivity(goToMarket) }
            catch (e: Exception) { Toast.makeText(this, "Play Store not found", Toast.LENGTH_SHORT).show() }
        }

        binding.tvFeedbackBugReport.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:arsalanbinkhan@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "Feedback for VibeMind")
            }
            startActivity(Intent.createChooser(emailIntent, "Send Feedback"))
        }

        binding.tvOpenSourceLicenses.setOnClickListener {
            startActivity(Intent(this, LicensesActivity::class.java))
        }
    }
}