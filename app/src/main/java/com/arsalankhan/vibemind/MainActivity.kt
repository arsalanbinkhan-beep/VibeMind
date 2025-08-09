package com.arsalankhan.vibemind

import android.Manifest
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.arsalankhan.vibemind.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var allSongs: ArrayList<Song>

    private lateinit var popSongs: List<Song>
    private lateinit var rockSongs: List<Song>
    private lateinit var epicSongs: List<Song>
    private lateinit var lofiSongs: List<Song>
    private lateinit var forYouSongs: List<Song>

    private val geminiApiKey = "AIzaSyBmYo9Id92Fso_8umH0IqURYLL-bX9ODi4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        attachMiniPlayer(binding.miniPlayer)

        setupNavBarListeners()
        checkAndLoadSongs()
    }

    private fun setupNavBarListeners() {
        binding.ivHome.setOnClickListener {}

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

        lifecycleScope.launch {
            try {
                val songTitles = allSongs.map { it.title }
                val categorizedSongsJson = withContext(Dispatchers.IO) {
                    GeminiApi(geminiApiKey).getCategorizedSongs(songTitles)
                }
                val categorizedSongs = parseCategorizedSongsJson(categorizedSongsJson)

                // Filter songs based on the Gemini response with prefixes
                popSongs = allSongs.filter { categorizedSongs["Pop"]?.contains(it.title) == true }
                rockSongs = allSongs.filter { categorizedSongs["Rock"]?.contains(it.title) == true }
                epicSongs = allSongs.filter { categorizedSongs["Epic"]?.contains(it.title) == true }
                lofiSongs = allSongs.filter { categorizedSongs["Lofi"]?.contains(it.title) == true }

                forYouSongs = allSongs.shuffled().take(5)
                updateUI()

            } catch (e: Exception) {
                Log.e("GeminiApi", "Error categorizing songs: ${e.message}", e)
                // Fallback logic if the API call fails
                // Note: Without the 'category' property in the Song data class,
                // a proper fallback would require a different method, but we'll
                // keep a basic random selection here.
                popSongs = allSongs.shuffled().take(5)
                rockSongs = allSongs.shuffled().take(5)
                epicSongs = allSongs.shuffled().take(5)
                lofiSongs = allSongs.shuffled().take(5)
                forYouSongs = allSongs.shuffled()
                updateUI() }
        }
    }

    private fun updateUI() {
        val recommendedSongs = getRecommendedSongs()

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

        if (PlayerManager.currentSong == null) {
            loadLastPlayedSong()
        }
    }

    private fun testGeminiApi() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    GeminiApi(geminiApiKey).getCategorizedSongs(listOf("Song 1", "Song 2"))
                }
                Log.d("GeminiTest", "API Response: $response")
                JSONObject(response)
                Log.d("GeminiTest", "JSON response is valid!")
            } catch (e: Exception) {
                Log.e("GeminiTest", "Gemini API Test Failed: ${e.message}")
            }
        }
    }

    private fun getRecommendedSongs(): List<Song> {
        if (allSongs.size <= 5) return allSongs
        return allSongs.shuffled().distinctBy { it.title }.take(5)
    }

    private fun updateLastPlayedSection() {
        val prefs = getSharedPreferences("LAST_PLAYED", MODE_PRIVATE)
        val jsonList = prefs.getString("songs", "[]")
        val songList = mutableListOf<Song>()

        try {
            val jsonArray = org.json.JSONArray(jsonList)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val song = allSongs.find { it.id == obj.getLong("id") }
                if (song != null) {
                    songList.add(song)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

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
            LastPlayedAdapter(ArrayList(songList), songClickListener)
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

    private fun parseCategorizedSongsJson(jsonString: String): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()
        try {
            // Step 1: Remove the markdown code block identifiers.
            // The API often wraps its JSON response in a markdown code block.
            val cleanedJsonString = jsonString
                .replace("```json", "")
                .replace("```", "")
                .trim()

            // Step 2: Now that the string is clean, attempt to parse it.
            val jsonObject = JSONObject(cleanedJsonString)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val category = keys.next()
                val jsonArray = jsonObject.getJSONArray(category)
                val songTitles = (0 until jsonArray.length()).map {
                    jsonArray.getString(it)
                }
                result[category] = songTitles
            }
        } catch (e: Exception) {
            // Log the cleaned string for debugging purposes
            Log.e("JSON_PARSING", "Error parsing JSON from Gemini. Cleaned: $jsonString. Error: ${e.message}")
        }
        return result
    }

    private fun saveLastPlayedSong(song: Song) {
        val prefs = getSharedPreferences("LAST_PLAYED", MODE_PRIVATE)
        val jsonList = prefs.getString("songs", "[]")
        val songList = mutableListOf<Song>()

        try {
            val jsonArray = org.json.JSONArray(jsonList)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                songList.add(
                    Song(
                        id = obj.getLong("id"),
                        title = obj.getString("title"),
                        artist = obj.getString("artist"),
                        path = obj.getString("path"),
                        duration = obj.getLong("duration"),
                        albumId = obj.getLong("albumId")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Remove if already exists
        songList.removeAll { it.id == song.id }

        // Add to top
        songList.add(0, song)

        // Keep only 3
        if (songList.size > 3) {
            songList.subList(3, songList.size).clear()
        }

        // Save back
        val newJsonArray = org.json.JSONArray()
        songList.forEach {
            val obj = org.json.JSONObject().apply {
                put("id", it.id)
                put("title", it.title)
                put("artist", it.artist)
                put("path", it.path)
                put("duration", it.duration)
                put("albumId", it.albumId)
            }
            newJsonArray.put(obj)
        }

        prefs.edit().putString("songs", newJsonArray.toString()).apply()
    }


    private fun loadLastPlayedSong() {
        val prefs = getSharedPreferences("LAST_PLAYED", MODE_PRIVATE)
        val jsonList = prefs.getString("songs", "[]")

        try {
            val jsonArray = org.json.JSONArray(jsonList)
            if (jsonArray.length() > 0) {
                val obj = jsonArray.getJSONObject(0)
                val song = allSongs.find { it.id == obj.getLong("id") }
                if (song != null) {
                    val songIndex = allSongs.indexOfFirst { it.id == song.id }
                    if (songIndex != -1) {
                        PlayerManager.playSong(this, allSongs, songIndex)
                        MiniPlayerManager.refresh(this)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
}