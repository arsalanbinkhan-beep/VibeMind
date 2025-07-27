package com.arsalankhan.vibemind

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.arsalankhan.vibemind.databinding.PlaylistAlbumBinding

class PlaylistAlbumActivity : AppCompatActivity() {

    private lateinit var binding: PlaylistAlbumBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PlaylistAlbumBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }
}