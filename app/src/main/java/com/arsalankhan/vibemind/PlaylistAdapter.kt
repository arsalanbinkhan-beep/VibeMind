package com.arsalankhan.vibemind

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arsalankhan.vibemind.databinding.ItemPlaylistBinding
import com.bumptech.glide.Glide

// PlaylistAdapter.kt
// PlaylistAdapter.kt
class PlaylistAdapter(
    private val playlists: List<Playlist>,
    private val onPlaylistClick: (Playlist) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    inner class PlaylistViewHolder(val binding: ItemPlaylistBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = ItemPlaylistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = playlists[position]

        holder.binding.tvPlaylistTitle.text = playlist.name
        holder.binding.tvPlaylistArtist.text = playlist.artist

        Glide.with(holder.binding.root.context)
            .load(playlist.getCoverArt())
            .placeholder(R.drawable.album_art)
            .error(R.drawable.album_art)
            .into(holder.binding.ivPlaylistCover)

        holder.binding.root.setOnClickListener {
            onPlaylistClick(playlist)
        }
    }

    override fun getItemCount(): Int = playlists.size
}