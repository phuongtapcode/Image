package com.example.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class AddMediaResult(
    val addedCount: Int,
    val newTotalCount: Int,
    val allPersisted: Boolean
)

class MediaSlotRepository(
    private val dao: MediaSlotDao,
    private val uriHelper: MediaUriHelper
) {
    val slots: Flow<List<MediaSlot>> = dao.observeAllSlots()

    suspend fun addMediaUris(uris: List<Uri>): AddMediaResult = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) {
            val currentCount = dao.getAllSlotsOnce().size
            return@withContext AddMediaResult(0, currentCount, true)
        }

        val resolved = uris.map { uriHelper.resolveAndPersist(it) }
        val newEntities = resolved.map { item ->
            MediaSlot(
                uriString = item.uriString,
                mediaType = item.mediaType.name,
                displayName = item.displayName,
                orderIndex = 0
            )
        }
        dao.appendSlots(newEntities)
        val totalAfter = dao.getAllSlotsOnce().size
        AddMediaResult(
            addedCount = newEntities.size,
            newTotalCount = totalAfter,
            allPersisted = resolved.all { it.persistedPermission }
        )
    }

    suspend fun replaceSlot(slotId: Long, newUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val existing = dao.getSlotById(slotId) ?: return@withContext false
        val resolved = uriHelper.resolveAndPersist(newUri)
        val updated = existing.copy(
            uriString = resolved.uriString,
            mediaType = resolved.mediaType.name,
            displayName = resolved.displayName
        )
        dao.updateSlot(updated)

        if (existing.uriString != resolved.uriString && dao.countByUri(existing.uriString) == 0) {
            uriHelper.releasePermissionIfHeld(existing.uriString)
        }
        resolved.persistedPermission
    }

    suspend fun moveSlot(slotId: Long, delta: Int) = withContext(Dispatchers.IO) {
        dao.moveSlot(slotId, delta)
    }

    suspend fun deleteSlot(slotId: Long) = withContext(Dispatchers.IO) {
        val removedUri = dao.deleteAndReindex(slotId)
        if (removedUri != null && dao.countByUri(removedUri) == 0) {
            uriHelper.releasePermissionIfHeld(removedUri)
        }
    }
}
