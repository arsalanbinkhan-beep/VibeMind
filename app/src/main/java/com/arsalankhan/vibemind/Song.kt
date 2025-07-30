package com.arsalankhan.vibemind

import java.io.Serializable


data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val path: String,
    val duration: Long,
    val albumId: Long,
    val category: String
) : Serializable
