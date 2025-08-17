package com.arsalankhan.vibemind

import android.Manifest
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.arsalankhan.vibemind.databinding.ActivityMainBinding
import java.io.Serializable // Import for Serializable

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var allSongs: ArrayList<Song>
    private lateinit var popSongs: List<Song>
    private lateinit var rockSongs: List<Song>
    private lateinit var epicSongs: List<Song>
    private lateinit var lofiSongs: List<Song>
    private lateinit var forYouSongs: List<Song>

    override fun onCreate(savedInstanceState: Bundle?) {
        window.decorView.setOnApplyWindowInsetsListener(null)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Attach the mini player to BaseActivity
        attachMiniPlayer(binding.miniPlayer)


        binding.miniPlayer.root.setOnClickListener {
            val intent = Intent(this, MusicPlayerActivity::class.java)

            // Pass the current song list and index from your PlayerManager
            PlayerManager.songList?.let { songList ->
                intent.putExtra("SONG_LIST", songList as java.io.Serializable)
                intent.putExtra("SELECTED_INDEX", PlayerManager.currentIndex)
            }

            startActivity(intent)
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_up, R.anim.slide_out_down) // Changed to slide animations
            startActivity(intent, options.toBundle())


        }

        setupNavBarListeners()
        checkAndLoadSongs()

        if (PlayerManager.currentSong == null) {
            loadLastPlayedSong()
        }
    }

    private fun setupNavBarListeners() {
        binding.ivHome.setOnClickListener {}

        binding.ivAlbum.setOnClickListener {
            val intent = Intent(this, PlaylistAlbumActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_from_right, R.anim.slide_to_left)
            startActivity(intent, options.toBundle())
        }

        binding.ivSearch.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_from_right, R.anim.slide_to_left)
            startActivity(intent, options.toBundle())
        }

        binding.ivSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_from_right, R.anim.slide_to_left)
            startActivity(intent, options.toBundle())
        }
    }

    private fun checkAndLoadSongs() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Use TIRAMISU constant
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
        } else {
            loadAndCategorizeSongs()
        }
    }

    private fun loadAndCategorizeSongs() {
        allSongs = SongUtils().getAllAudioFiles(this)

        popSongs = allSongs.filter { it.category == "Pop" }
        rockSongs = allSongs.filter { it.category == "Rock" }
        epicSongs = allSongs.filter { it.category == "Epic" }
        lofiSongs = allSongs.filter { it.category == "Lofi" }
        forYouSongs = allSongs.filter { it.category == "For You" }

        val recommendedSongs = getRecommendedSongs()

        val songClickListener = { list: ArrayList<Song>, index: Int ->
            val selectedSong = list[index]
            if (PlayerManager.currentSong?.path != selectedSong.path) {
                PlayerManager.playSong(this, list, index)
                PlaybackHistoryManager.incrementPlayCount(this, selectedSong.path)
                saveLastPlayedSong(selectedSong)
                updateLastPlayedSection()
                MiniPlayerManager.refresh(this) // Refresh mini player UI
            }
        }

        binding.recyclerViewSongs.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = SongAdapter(ArrayList(popSongs), songClickListener)
        }

        binding.recyclerViewRecommendedSongs.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = RecommendedSongAdapter(ArrayList(recommendedSongs), songClickListener)
        }

        binding.recyclerViewLastPlayedSongs.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        updateLastPlayedSection()
        setupCategoryButtonListeners(songClickListener)
    }

    private fun getRecommendedSongs(): List<Song> {
        if (allSongs.size <= 5) return allSongs
        return allSongs.shuffled().distinctBy { it.title }.take(5)
    }

    private fun updateLastPlayedSection() {
        val topPlayed = PlaybackHistoryManager.getTopPlayedSongs(this, allSongs)
            .take(3) // Limit to last 3 songs only

        val songClickListener = { list: ArrayList<Song>, index: Int ->
            val selectedSong = list[index]
            if (PlayerManager.currentSong?.path != selectedSong.path) {
                PlayerManager.playSong(this, list, index)
                PlaybackHistoryManager.incrementPlayCount(this, selectedSong.path)
                saveLastPlayedSong(selectedSong)
                updateLastPlayedSection()
                MiniPlayerManager.refresh(this)
            }
        }

        binding.recyclerViewLastPlayedSongs.adapter =
            LastPlayedAdapter(ArrayList(topPlayed), songClickListener)
    }


    private fun setupCategoryButtonListeners(songClickListener: (ArrayList<Song>, Int) -> Unit) {
        // Assuming you have IDs for your category buttons in activity_main.xml
        // e.g., android:id="@+id/categoryPop"
        binding.categoryPop.setOnClickListener {
            binding.recyclerViewSongs.adapter = SongAdapter(ArrayList(popSongs), songClickListener)
        }
        binding.categoryRock.setOnClickListener {
            binding.recyclerViewSongs.adapter = SongAdapter(ArrayList(rockSongs), songClickListener)
        }
        binding.categoryEpic.setOnClickListener {
            binding.recyclerViewSongs.adapter = SongAdapter(ArrayList(epicSongs), songClickListener)
        }
        binding.categoryLofi.setOnClickListener {
            binding.recyclerViewSongs.adapter = SongAdapter(ArrayList(lofiSongs), songClickListener)
        }
        binding.categoryForYou.setOnClickListener {
            binding.recyclerViewSongs.adapter = SongAdapter(ArrayList(forYouSongs), songClickListener)
        }
    }

    private fun saveLastPlayedSong(song: Song) {
        val prefs = getSharedPreferences("LAST_PLAYED", MODE_PRIVATE)
        prefs.edit().apply {
            putLong("id", song.id)
            putString("title", song.title)
            putString("artist", song.artist)
            putString("path", song.path)
            putLong("duration", song.duration)
            putLong("albumId", song.albumId)
            putString("category", song.category)
            apply()
        }
    }

    private fun loadLastPlayedSong() {
        val prefs = getSharedPreferences("LAST_PLAYED", MODE_PRIVATE)
        val id = prefs.getLong("id", -1L)
        val title = prefs.getString("title", null)
        val artist = prefs.getString("artist", null)
        val path = prefs.getString("path", null)
        val duration = prefs.getLong("duration", -1L)
        val albumId = prefs.getLong("albumId", -1L)
        val category = prefs.getString("category", null)

        if (id != -1L && !title.isNullOrEmpty() && !artist.isNullOrEmpty() &&
            !path.isNullOrEmpty() && duration > 0 && albumId != -1L && !category.isNullOrEmpty()) {

            val song = Song(
                id = id,
                title = title,
                artist = artist,
                path = path,
                duration = duration,
                albumId = albumId,
                category = category
            )

            PlayerManager.playSong(this, listOf(song), 0)
            MiniPlayerManager.refresh(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadAndCategorizeSongs()
        }
    }

    //make the play pause button change when pressed
    override fun onResume() {
        super.onResume()
        // Refresh the mini player UI when returning to MainActivity
        MiniPlayerManager.refresh(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear the mini player binding to avoid memory leaks
        miniPlayerBinding?.let {
            MiniPlayerManager.bindMiniPlayer(this, it)
        }
    }

    // Use OnBackPressedDispatcher for modern back handling
    override fun onBackPressed() {
        if (miniPlayerBinding?.root?.visibility == android.view.View.VISIBLE) {
            miniPlayerBinding?.root?.visibility = android.view.View.GONE
        } else {
            // Use the dispatcher for backward compatibility
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        // Handle the up navigation
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    //give the mini player a click listener to open the music player activity and animation of slide up





}