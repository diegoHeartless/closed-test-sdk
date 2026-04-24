package io.closedtest.sdk.internal.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queued_events")
data class QueuedEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val json: String,
    val createdAtEpochMs: Long,
)
