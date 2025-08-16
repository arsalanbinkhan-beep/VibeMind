package com.arsalankhan.vibemind

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.arsalankhan.vibemind.databinding.ActivityPlaylistDetailBinding
import com.bumptech.glide.Glide

// PlaylistDetailActivity.kt
class PlaylistDetailActivity : BaseActivity() {
    private lateinit var binding: ActivityPlaylistDetailBinding
    private lateinit var songAdapter: SongAdapter
    private lateinit var playlist: Playlist

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        attachMiniPlayer(binding.miniPlayer)

        playlist = intent.getSerializableExtra("PLAYLIST") as Playlist
        setupToolbar()
        setupPlaylistHeader()
        setupSongList()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = playlist.name
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupPlaylistHeader() {
        Glide.with(this)
            .load(playlist.getCoverArt())
            .placeholder(R.drawable.album_art)
            .error(R.drawable.album_art)
            .into(binding.ivPlaylistCover)

        binding.tvPlaylistTitle.text = playlist.name
        binding.tvPlaylistInfo.text = "${playlist.songs.size} songs • ${playlist.artist}"

        binding.btnPlayAll.setOnClickListener {
            if (playlist.songs.isNotEmpty()) {
                PlayerManager.playSong(this, playlist.songs, 0)
                MiniPlayerManager.refresh(this)
            }
        }

        binding.btnShuffle.setOnClickListener {
            if (playlist.songs.isNotEmpty()) {
                val randomIndex = (0 until playlist.songs.size).random()
                PlayerManager.playSong(this, playlist.songs, randomIndex)
                MiniPlayerManager.refresh(this)
            }
        }
    }

    private fun setupSongList() {
        songAdapter = SongAdapter(ArrayList(playlist.songs)) { songs, position ->
            PlayerManager.playSong(this, songs, position)
            MiniPlayerManager.refresh(this)
        }

        binding.recyclerViewSongs.apply {
            layoutManager = LinearLayoutManager(this@PlaylistDetailActivity)
            adapter = songAdapter
            addItemDecoration(
                DividerItemDecoration(
                    this@PlaylistDetailActivity,
                    LinearLayoutManager.VERTICAL
                )
            )
        }
    }
    private fun showAddToPlaylistDialog(song: Song) {
        val playlists = PlaylistManager.getUserPlaylists()

        AlertDialog.Builder(this)
            .setTitle("Add to playlist")
            .setItems(playlists.map { it.name }.toTypedArray()) { _, which ->
                val selectedPlaylist = playlists[which]
                if (!selectedPlaylist.songs.any { it.id == song.id }) {
                    selectedPlaylist.songs.add(song)
                    PlaylistManager.saveUserPlaylists()
                    Toast.makeText(this, "Added to ${selectedPlaylist.name}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Song already in playlist", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}