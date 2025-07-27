package com.arsalankhan.vibemind

import android.app.ActivityOptions
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.arsalankhan.vibemind.databinding.PlaylistAlbumBinding

class PlaylistAlbumActivity : AppCompatActivity() {

    private lateinit var binding: PlaylistAlbumBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = PlaylistAlbumBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener {
            val intent= Intent(this, MainActivity::class.java)
            val options = ActivityOptions.makeCustomAnimation(this, R.anim.fade_in, R.anim.fade_out)
            startActivity(intent, options.toBundle())
        }

    }
}