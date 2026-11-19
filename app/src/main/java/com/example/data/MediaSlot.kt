package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_slots")
data class MediaSlot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val uriString: String,
    val mediaType: String,
    val displayName: String,
    val orderIndex: Int,
    val addedAt: Long = System.currentTimeMillis()
) {
    val type: MediaType
        get() = MediaType.fromStored(mediaType)
}
