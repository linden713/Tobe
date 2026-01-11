package com.example.tobe.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tobe.data.Quote
import com.example.tobe.data.QuoteRepository
import com.example.tobe.data.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: UserPreferencesRepository
) : ViewModel() {

    val userPreferences = repository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val currentQuote: Quote = QuoteRepository.getRandomQuote()

    fun checkIn() {
        viewModelScope.launch {
            repository.updateLastActiveTime()
        }
    }

    fun updateTimeout(hours: Int) {
        viewModelScope.launch {
            repository.updateTimeout(hours)
        }
    }

    fun updateContact(name: String, phone: String) {
        viewModelScope.launch {
            repository.updateContact(name, phone)
        }
    }

    fun updateSmsMessage(message: String) {
        viewModelScope.launch {
            repository.updateSmsMessage(message)
        }
    }

    fun setSmsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSmsEnabled(enabled)
        }
    }

    fun setMonitoringEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMonitoringEnabled(enabled)
        }
    }

    // Factory for manual DI
    class Factory(private val repository: UserPreferencesRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
