package com.example.medicare.data

import kotlinx.coroutines.flow.Flow

class MedicineRepository(private val dao: MedicineDao) {
    val allMedicines: Flow<List<Medicine>> = dao.getAllMedicines()

    suspend fun insert(medicine: Medicine) {
        dao.insert(medicine)
    }

    suspend fun delete(medicine: Medicine) {
        dao.delete(medicine)
    }
}
