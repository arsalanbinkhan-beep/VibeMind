package com.arsalankhan.vibemind

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arsalankhan.vibemind.databinding.ItemRecommendedSongBinding
import com.bumptech.glide.Glide

class RecommendedSongAdapter(
    private val songs: ArrayList<Song>,
    private val onClick: (ArrayList<Song>, Int) -> Unit
) : RecyclerView.Adapter<RecommendedSongAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemRecommendedSongBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song) {
            binding.tvRecommendedTitle.text = song.title
            binding.tvRecommendedArtist.text = song.artist
            Glide.with(binding.root.context)
                .load(song.albumArtUri)
                .placeholder(R.drawable.ic_album_art)
                .into(binding.ivRecommendedArt)

            binding.ivPlayRecommended.setOnClickListener {
                onClick(songs, adapterPosition)
            }

            binding.root.setOnClickListener {
                onClick(songs, adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ItemRecommendedSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = songs.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(songs[position])
    }
}
