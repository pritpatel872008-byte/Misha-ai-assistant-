package com.example.memory

import com.example.data.repository.MishaRepository

class MemoryManager(private val repository: MishaRepository) {

    suspend fun recordUserPreference(key: String, value: String) {
        repository.addMemory("PREFERENCE", key, value)
    }

    suspend fun recordFrequentContact(contactName: String, phone: String) {
        repository.addMemory("CONTACT", contactName, phone)
    }

    suspend fun recordAssistantLanguage(language: String) {
        repository.addMemory("LANGUAGE", "preferred_language", language)
    }

    suspend fun clearAllMemory() {
        repository.clearMemories()
    }
}
