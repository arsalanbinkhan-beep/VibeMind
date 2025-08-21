package com.arsalankhan.vibemind

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arsalankhan.vibemind.databinding.ItemSongHorizontalBinding
import com.bumptech.glide.Glide

class LastPlayedAdapter(
    private val songs: ArrayList<Song>,
    private val onClick: (ArrayList<Song>, Int) -> Unit
) : RecyclerView.Adapter<LastPlayedAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemSongHorizontalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(song: Song) {
            binding.tvSongTitle.text = song.title
            binding.tvArtistName.text = song.artist

            Glide.with(binding.root.context)
                .load(song.getAlbumArtUri())
                .placeholder(R.drawable.ic_album_art)
                .into(binding.ivAlbumArt)

            binding.root.setOnClickListener {
                onClick(songs, adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSongHorizontalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = songs.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(songs[position])
    }
}
