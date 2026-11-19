package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaSlotDao {

    @Query("SELECT * FROM media_slots ORDER BY orderIndex ASC, id ASC")
    fun observeAllSlots(): Flow<List<MediaSlot>>

    @Query("SELECT * FROM media_slots ORDER BY orderIndex ASC, id ASC")
    suspend fun getAllSlotsOnce(): List<MediaSlot>

    @Query("SELECT * FROM media_slots WHERE id = :slotId LIMIT 1")
    suspend fun getSlotById(slotId: Long): MediaSlot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: MediaSlot): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<MediaSlot>)

    @Update
    suspend fun updateSlot(slot: MediaSlot)

    @Update
    suspend fun updateSlots(slots: List<MediaSlot>)

    @Query("DELETE FROM media_slots WHERE id = :slotId")
    suspend fun deleteSlotById(slotId: Long)

    @Query("SELECT COUNT(*) FROM media_slots WHERE uriString = :uriString")
    suspend fun countByUri(uriString: String): Int

    @Transaction
    suspend fun appendSlots(items: List<MediaSlot>) {
        if (items.isEmpty()) return
        val existingCount = getAllSlotsOnce().size
        val indexed = items.mapIndexed { offset, slot ->
            slot.copy(orderIndex = existingCount + offset)
        }
        insertSlots(indexed)
    }

    @Transaction
    suspend fun moveSlot(slotId: Long, delta: Int) {
        val current = getAllSlotsOnce().toMutableList()
        val fromIndex = current.indexOfFirst { it.id == slotId }
        if (fromIndex == -1) return
        val toIndex = (fromIndex + delta).coerceIn(0, current.lastIndex)
        if (fromIndex == toIndex) return

        val moved = current.removeAt(fromIndex)
        current.add(toIndex, moved)

        val reindexed = current.mapIndexed { index, slot ->
            slot.copy(orderIndex = index)
        }
        updateSlots(reindexed)
    }

    @Transaction
    suspend fun deleteAndReindex(slotId: Long): String? {
        val target = getSlotById(slotId) ?: return null
        deleteSlotById(slotId)
        val remaining = getAllSlotsOnce().mapIndexed { index, slot ->
            slot.copy(orderIndex = index)
        }
        if (remaining.isNotEmpty()) {
            updateSlots(remaining)
        }
        return target.uriString
    }
}
