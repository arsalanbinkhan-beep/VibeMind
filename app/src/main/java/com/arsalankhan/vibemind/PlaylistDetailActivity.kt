package com.arsalankhan.vibemind

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.arsalankhan.vibemind.databinding.ActivityPlaylistDetailBinding
import com.bumptech.glide.Glide

class PlaylistDetailActivity : BaseActivity() {
    private lateinit var binding: ActivityPlaylistDetailBinding
    private lateinit var songAdapter: SongAdapter
    private lateinit var playlist: Playlist
    private lateinit var allSongs: List<Song>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        attachMiniPlayer(binding.miniPlayer)

        // Get playlist ID
        val playlistId = intent.getLongExtra("PLAYLIST_ID", -1L)

        // Handle Liked Songs playlist specifically - use nullable type first
        val loadedPlaylist: Playlist? = if (playlistId == PlaylistManager.LIKED_PLAYLIST_ID) {
            PlaylistManager.getLikedSongsPlaylist()
        } else {
            PlaylistManager.getPlaylistById(playlistId)
        }

        // Check if playlist was found
        if (loadedPlaylist == null) {
            Toast.makeText(this, "Playlist not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Now assign to the non-null variable
        playlist = loadedPlaylist

        Log.d("PlaylistDetail", "Playlist loaded: ${playlist.name} with ${playlist.songs.size} songs")

        setupToolbar()
        setupPlaylistHeader()
        setupSongList()

        binding.btnAddSongs.setOnClickListener {
            showAddSongsDialog()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
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
                val shuffledSongs = playlist.songs.shuffled()
                PlayerManager.playSong(this, shuffledSongs, 0)
                MiniPlayerManager.refresh(this)
                Toast.makeText(this, "Shuffling ${playlist.songs.size} songs", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSongList() {
        songAdapter = SongAdapter(ArrayList(playlist.songs)) { songs, position ->
            PlayerManager.playSong(this, songs, position)
            MiniPlayerManager.refresh(this)
        }

        // Add long click listener to remove songs
        songAdapter.setOnItemLongClickListener { song, position ->
            if (playlist.isUserCreated) {
                showRemoveSongDialog(song, position)
                true
            } else {
                false
            }
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

    private fun showAddSongsDialog() {
        val allSongs = SongUtils().getAllAudioFiles(this)
        val currentPlaylistSongIds = playlist.songs.map { it.id }.toSet()
        val availableSongs = allSongs.filter { !currentPlaylistSongIds.contains(it.id) }

        if (availableSongs.isEmpty()) {
            Toast.makeText(this, "No songs available to add", Toast.LENGTH_SHORT).show()
            return
        }

        // Create a list of song display names for the dialog
        val songDisplayNames = availableSongs.map { "${it.title} - ${it.artist}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Add songs to playlist")
            .setItems(songDisplayNames) { _, which ->
                val selectedSong = availableSongs[which]
                // Use PlaylistManager to properly add the song
                PlaylistManager.addSongToPlaylist(this, playlist.id, selectedSong)

                // Also update the local playlist for immediate UI update
                playlist.songs.add(selectedSong)
                songAdapter.updateSongs(ArrayList(playlist.songs))

                Toast.makeText(this, "Added '${selectedSong.title}' to playlist", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRemoveSongDialog(song: Song, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Remove Song")
            .setMessage("Remove '${song.title}' from this playlist?")
            .setPositiveButton("Remove") { _, _ ->
                PlaylistManager.removeSongFromPlaylist(this, playlist.id, song.id)
                playlist.songs.removeAt(position)
                songAdapter.notifyItemRemoved(position)
                Toast.makeText(this, "Song removed", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_playlist_detail, menu)
        val deleteItem = menu.findItem(R.id.action_delete_playlist)
        deleteItem.isVisible = playlist.isUserCreated
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete_playlist -> {
                showDeletePlaylistDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDeletePlaylistDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Playlist")
            .setMessage("Are you sure you want to delete '${playlist.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                PlaylistManager.deletePlaylist(playlist.id)
                Toast.makeText(this, "Playlist deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}