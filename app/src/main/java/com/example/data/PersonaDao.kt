package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonaDao {
    @Query("SELECT * FROM personas ORDER BY builtIn DESC, createdAt ASC")
    fun getAll(): Flow<List<Persona>>

    @Query("SELECT * FROM personas ORDER BY builtIn DESC, createdAt ASC")
    suspend fun getAllOnce(): List<Persona>

    @Query("SELECT * FROM personas WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Persona?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(persona: Persona): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(personas: List<Persona>)

    @Update suspend fun update(persona: Persona)
    @Delete suspend fun delete(persona: Persona)

    @Query("SELECT COUNT(*) FROM personas")
    suspend fun count(): Int
}
