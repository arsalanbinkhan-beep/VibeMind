package com.arsalankhan.vibemind

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arsalankhan.vibemind.databinding.ItemSongBinding
import com.bumptech.glide.Glide

class SongAdapter(
    private val songList: ArrayList<Song>,
    private val onSongClick: (ArrayList<Song>, Int) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    inner class SongViewHolder(val binding: ItemSongBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songList[position]
        val binding = holder.binding

        binding.tvSongTitle.text = song.title
        binding.tvSongCategory.text = song.artist

        Glide.with(binding.root.context)
            .load(song.albumArtUri)
            .placeholder(R.drawable.ic_album_art) // fallback image
            .into(binding.ivSongIcon)

        // Click listeners
        binding.root.setOnClickListener {
            onSongClick(songList, position)
        }

        binding.ivPlayRecommended.setOnClickListener {
            onSongClick(songList, position)
        }
    }

    override fun getItemCount(): Int = songList.size
}
