package com.arsalankhan.vibemind

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.recyclerview.widget.LinearLayoutManager
import com.arsalankhan.vibemind.databinding.ActivitySearchBinding

class SearchActivity : BaseActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var allSongs: ArrayList<Song>
    private lateinit var filteredSongs: ArrayList<Song>
    private lateinit var songAdapter: SongAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Attach mini player view to BaseActivity
        attachMiniPlayer(binding.miniPlayerLayout)

        // Get all songs
        allSongs = SongUtils().getAllAudioFiles(this)
        filteredSongs = ArrayList()

        setupRecyclerView()
        setupSearchListener()

        binding.backArrow.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_from_right, R.anim.slide_to_left)
            startActivity(intent, options.toBundle())
        }
    }

    private fun setupRecyclerView() {
        songAdapter = SongAdapter(filteredSongs) { list, index ->
            val selectedSong = list[index]
            if (PlayerManager.currentSong?.path != selectedSong.path) {
                PlayerManager.playSong(this, list, index)
                PlaybackHistoryManager.incrementPlayCount(this, selectedSong.path)
            }

            val intent = Intent(this, MusicPlayerActivity::class.java)
            intent.putExtra("SONG_LIST", list)
            intent.putExtra("SELECTED_INDEX", index)
            startActivity(intent)
        }

        binding.recyclerViewSearchResults.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = songAdapter
        }
    }

    private fun setupSearchListener() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                filteredSongs.clear()

                if (query.isNotEmpty()) {
                    val results = allSongs.filter {
                        it.title.contains(query, ignoreCase = true) ||
                                it.artist.contains(query, ignoreCase = true)
                    }
                    filteredSongs.addAll(results)
                }

                songAdapter.notifyDataSetChanged()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}
