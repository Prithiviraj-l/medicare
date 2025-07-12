package com.example.medicare.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medicare.data.Medicine
import com.example.medicare.data.MedicineRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MedicineViewModel(private val repository: MedicineRepository) : ViewModel() {
    val allMedicines: StateFlow<List<Medicine>> = repository.allMedicines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun addMedicine(medicine: Medicine) {
        viewModelScope.launch { repository.insert(medicine) }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch { repository.delete(medicine) }
    }
}
