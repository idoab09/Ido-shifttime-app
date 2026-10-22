package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WorkShiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkShiftDao {
    @Query("SELECT * FROM work_shifts ORDER BY dateString DESC, startHour DESC, startMinute DESC")
    fun getAllShifts(): Flow<List<WorkShiftEntity>>

    @Query("SELECT * FROM work_shifts WHERE isArchived = 0 ORDER BY dateString DESC, startHour DESC, startMinute DESC")
    fun getActiveShifts(): Flow<List<WorkShiftEntity>>

    @Query("SELECT * FROM work_shifts WHERE isArchived = 1 ORDER BY dateString DESC, startHour DESC, startMinute DESC")
    fun getArchivedShifts(): Flow<List<WorkShiftEntity>>

    @Query("SELECT * FROM work_shifts WHERE id = :id LIMIT 1")
    suspend fun getShiftById(id: Long): WorkShiftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: WorkShiftEntity): Long

    @Update
    suspend fun updateShift(shift: WorkShiftEntity)

    @Delete
    suspend fun deleteShift(shift: WorkShiftEntity)

    @Query("DELETE FROM work_shifts WHERE id = :id")
    suspend fun deleteShiftById(id: Long)

    @Query("UPDATE work_shifts SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)

    @Query("UPDATE work_shifts SET isArchived = 1 WHERE dateString < :cutoffDate AND isArchived = 0")
    suspend fun archiveOlderThan(cutoffDate: String): Int

    @Query("UPDATE work_shifts SET isArchived = 1 WHERE isArchived = 0")
    suspend fun archiveAllActive(): Int

    @Query("UPDATE work_shifts SET isArchived = 0 WHERE isArchived = 1")
    suspend fun restoreAllArchived(): Int

    @Query("DELETE FROM work_shifts WHERE isArchived = 1")
    suspend fun deleteAllArchived(): Int
}
