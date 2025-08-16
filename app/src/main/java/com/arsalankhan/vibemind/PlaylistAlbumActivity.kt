package com.arsalankhan.vibemind

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import com.arsalankhan.vibemind.databinding.PlaylistAlbumBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PlaylistAlbumActivity : BaseActivity() {

    private lateinit var binding: PlaylistAlbumBinding
    private lateinit var forYouAdapter: PlaylistAdapter
    private lateinit var yourPlaylistsAdapter: PlaylistAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PlaylistAlbumBinding.inflate(layoutInflater)
        setContentView(binding.root)
        attachMiniPlayer(binding.miniPlayerLayout)

        setupToolbar()
        setupPlaylistSections()
        setupCreateButton()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            navigateBackToMain()
        }
    }

    private fun setupCreateButton() {
        binding.btnCreatePlaylist.setOnClickListener {
            showCreatePlaylistDialog()
        }
    }

    private fun showCreatePlaylistDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_playlist, null)
        val editText = dialogView.findViewById<EditText>(R.id.et_playlist_name)

        MaterialAlertDialogBuilder(this)
            .setTitle("Create New Playlist")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val playlistName = editText.text.toString().trim()
                if (playlistName.isNotEmpty()) {
                    handlePlaylistCreation(playlistName)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun handlePlaylistCreation(name: String) {
        val newPlaylist = PlaylistManager.createNewPlaylist(name)
        refreshPlaylistViews()
    }

    private fun setupPlaylistSections() {
        PlaylistManager.initialize(this)
        val allSongs = SongUtils().getAllAudioFiles(this)

        // For You section
        forYouAdapter = PlaylistAdapter(createForYouPlaylists(allSongs)) { playlist ->
            openPlaylistDetail(playlist)
        }

        binding.rvPlaylistForYou.apply {
            layoutManager = LinearLayoutManager(
                this@PlaylistAlbumActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = forYouAdapter
        }

        // Your Playlists section
        refreshPlaylistViews()
    }

    private fun createForYouPlaylists(allSongs: List<Song>): List<Playlist> {
        return listOf(
            PlaylistManager.getForYouPlaylist(this, allSongs),
            PlaylistManager.getMostPlayedPlaylist(this, allSongs),
            PlaylistManager.getLikedSongsPlaylist()
        )
    }

    private fun refreshPlaylistViews() {
        val userPlaylists = PlaylistManager.getUserPlaylists()
        yourPlaylistsAdapter = PlaylistAdapter(userPlaylists) { playlist ->
            openPlaylistDetail(playlist)
        }

        binding.rvYourPlaylists.apply {
            layoutManager = LinearLayoutManager(
                this@PlaylistAlbumActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = yourPlaylistsAdapter
        }

        // Update empty state visibility
        val hasUserPlaylists = userPlaylists.isNotEmpty()
        binding.tvYourPlaylistsTitle.visibility = if (hasUserPlaylists) View.VISIBLE else View.GONE
        binding.rvYourPlaylists.visibility = if (hasUserPlaylists) View.VISIBLE else View.GONE
    }

    private fun openPlaylistDetail(playlist: Playlist) {
        Intent(this, PlaylistDetailActivity::class.java).apply {
            putExtra("PLAYLIST", playlist)
            startActivity(this)
        }
    }

    private fun navigateBackToMain() {
        val intent = Intent(this, MainActivity::class.java)
        val options = ActivityOptions.makeCustomAnimation(
            this,
            R.anim.slide_from_right,
            R.anim.slide_to_left
        )
        startActivity(intent, options.toBundle())
        finish()
    }

    override fun onResume() {
        super.onResume()
        refreshPlaylistViews()
    }
}