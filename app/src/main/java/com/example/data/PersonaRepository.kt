package com.example.data

import android.content.Context
import com.example.util.LogRepository
import kotlinx.coroutines.flow.Flow

class PersonaRepository(context: Context) {
    private companion object { const val TAG = "PersonaRepo" }

    private val dao = ChatDatabase.getDatabase(context).personaDao()
    val personas: Flow<List<Persona>> = dao.getAll()

    /**
     * Đảm bảo DB có đủ persona built-in với prompt mới nhất.
     * Nếu prompt version cũ → xoá và chèn lại từ BuiltInPersonas.
     * Persona do user tạo (builtIn=false) KHÔNG bị ảnh hưởng.
     */
    suspend fun ensureDefaults() {
        val existing = dao.getAllOnce()
        val currentVersion = BuiltInPersonas.CURRENT_VERSION

        // Xoá built-in cũ nếu version thấp hơn
        val outdated = existing.filter { it.builtIn && it.builtInVersion < currentVersion }
        if (outdated.isNotEmpty()) {
            LogRepository.log(TAG, "Cập nhật ${outdated.size} persona built-in lên v${currentVersion}")
            outdated.forEach { dao.delete(it) }
        }

        // Đếm lại sau khi xoá
        val afterDelete = dao.getAllOnce()
        val existingNames = afterDelete.filter { it.builtIn }.map { it.name }.toSet()

        // Chèn những persona built-in còn thiếu
        val toInsert = BuiltInPersonas.defaults().filter { it.name !in existingNames }
        if (toInsert.isNotEmpty()) {
            LogRepository.log(TAG, "Chèn ${toInsert.size} persona built-in mới")
            dao.insertAll(toInsert)
        }
    }

    suspend fun getById(id: Long): Persona? = dao.getById(id)
    suspend fun insert(p: Persona): Long = dao.insert(p)
    suspend fun update(p: Persona) = dao.update(p)
    suspend fun delete(p: Persona) = dao.delete(p)
}
