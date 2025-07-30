package com.arsalankhan.vibemind

import android.Manifest
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.arsalankhan.vibemind.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var allSongs: ArrayList<Song>

    private lateinit var popSongs: List<Song>
    private lateinit var rockSongs: List<Song>
    private lateinit var epicSongs: List<Song>
    private lateinit var lofiSongs: List<Song>
    private lateinit var forYouSongs: List<Song>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavBarListeners()
        checkAndLoadSongs()
        setupMiniPlayerClick()
    }

    private fun setupNavBarListeners() {
        binding.ivHome.setOnClickListener {
            // Already on Home
        }

        binding.ivAlbum.setOnClickListener {
            val intent = Intent(this, PlaylistAlbumActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out)
            startActivity(intent, options.toBundle())
        }

        binding.ivSearch.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out)
            startActivity(intent, options.toBundle())
        }

        binding.ivSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out)
            startActivity(intent, options.toBundle())
        }
    }

    private fun checkAndLoadSongs() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

        // Categorize songs
        popSongs = allSongs.filter { it.category == "Pop" }
        rockSongs = allSongs.filter { it.category == "Rock" }
        epicSongs = allSongs.filter { it.category == "Epic" }
        lofiSongs = allSongs.filter { it.category == "Lofi" }
        forYouSongs = allSongs.filter { it.category == "For You" }

        // ✅ Common click listener
        val songClickListener = { songList: ArrayList<Song>, index: Int ->
            val intent = Intent(this, MusicPlayerActivity::class.java)
            intent.putExtra("SONG_LIST", songList)
            intent.putExtra("SELECTED_INDEX", index)
            startActivity(intent)
        }

        // Show Pop songs by default
        binding.recyclerViewSongs.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = SongAdapter(ArrayList(popSongs), songClickListener)
        }

        binding.recyclerViewRecommendedSongs.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = SongAdapter(ArrayList(rockSongs), songClickListener)
        }

        binding.recyclerViewLastPlayedSongs.apply {
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = SongAdapter(ArrayList(forYouSongs), songClickListener)
        }

        setupCategoryButtonListeners(songClickListener)
    }

    private fun setupCategoryButtonListeners(songClickListener: (ArrayList<Song>, Int) -> Unit) {
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

    private fun setupMiniPlayerClick() {
        binding.miniPlayer.setOnClickListener {
            // For now, open the first song (improve with playback tracking later)
            val intent = Intent(this, MusicPlayerActivity::class.java)
            intent.putExtra("SONG_LIST", allSongs)
            intent.putExtra("SELECTED_INDEX", 0)
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadAndCategorizeSongs()
        }
    }
}
