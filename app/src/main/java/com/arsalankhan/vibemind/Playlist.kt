package com.arsalankhan.vibemind

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class Playlist(
    var id: Long,
    var name: String,
    var songs: MutableList<Song>,
    var artist: String = "Various Artists",
    var isUserCreated: Boolean = false,
    var isArtistPlaylist: Boolean = false, // NEW: Add this flag
    var coverArtUri: Uri? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString() ?: "",
        parcel.createTypedArrayList(Song.CREATOR) ?: mutableListOf(),
        parcel.readString() ?: "Various Artists",
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(), // Read isArtistPlaylist
        parcel.readParcelable(Uri::class.java.classLoader)
    )

    fun getCoverArt(): Uri {
        if (isArtistPlaylist && songs.isNotEmpty()) {
            // Use the first song's album art for artist playlists
            return songs.first().getAlbumArtUri()
        }
        return coverArtUri ?: songs.firstOrNull()?.getAlbumArtUri() ?:
        Uri.parse("android.resource://com.arsalankhan.vibemind/drawable/album_art")
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(name)
        parcel.writeTypedList(songs)
        parcel.writeString(artist)
        parcel.writeByte(if (isUserCreated) 1 else 0)
        parcel.writeByte(if (isArtistPlaylist) 1 else 0) // Write isArtistPlaylist
        parcel.writeParcelable(coverArtUri, flags)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Playlist> {
        override fun createFromParcel(parcel: Parcel): Playlist {
            return Playlist(parcel)
        }

        override fun newArray(size: Int): Array<Playlist?> {
            return arrayOfNulls(size)
        }
    }
}