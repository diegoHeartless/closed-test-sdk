package io.closedtest.sdk.internal.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
@Dao
abstract class QueuedEventDao {

    @Insert
    abstract suspend fun insert(entity: QueuedEventEntity): Long

    @Query("SELECT * FROM queued_events ORDER BY id ASC LIMIT :limit")
    abstract suspend fun peek(limit: Int): List<QueuedEventEntity>

    @Query("DELETE FROM queued_events WHERE id IN (:ids)")
    abstract suspend fun deleteIdsInternal(ids: List<Long>)

    suspend fun deleteIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        deleteIdsInternal(ids)
    }

    @Query("SELECT COUNT(*) FROM queued_events")
    abstract suspend fun count(): Long

    @Query("SELECT id FROM queued_events ORDER BY id ASC LIMIT :limit")
    abstract suspend fun oldestIds(limit: Int): List<Long>
}
