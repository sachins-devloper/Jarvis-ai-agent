package com.personalai.jarvis.memory

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class FactItem(
    val id: String = UUID.randomUUID().toString(),
    val key: String,
    val value: String,
    val category: String = "general",
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "user", "jarvis", "system"
    val text: String,
    val imageUri: String? = null,
    val toolCall: String? = null,
    val toolResult: String? = null,
    val suggestions: List<String>? = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

class MemoryRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jarvis_memory", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_FACTS = "memory_facts"
        private const val KEY_MESSAGES = "conversation_messages"
    }

    init {
        // Pre-seed initial user facts if empty
        if (getFactsSync().isEmpty()) {
            saveFactSync(FactItem(key = "user_name", value = "User", category = "profile"))
            saveFactSync(FactItem(key = "assistant_role", value = "Jarvis - Android Local AI Agent", category = "system"))
            saveFactSync(FactItem(key = "preferred_programming_language", value = "Python", category = "preferences"))
        }
    }

    private fun getFactsSync(): List<FactItem> {
        val json = prefs.getString(KEY_FACTS, null) ?: return emptyList()
        val type = object : TypeToken<List<FactItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveFactSync(fact: FactItem) {
        val list = getFactsSync().toMutableList()
        // Replace if key exists, else append
        val index = list.indexOfFirst { it.key.equals(fact.key, ignoreCase = true) }
        if (index != -1) {
            list[index] = fact
        } else {
            list.add(fact)
        }
        prefs.edit().putString(KEY_FACTS, gson.toJson(list)).apply()
    }

    suspend fun getAllFacts(): List<FactItem> = withContext(Dispatchers.IO) {
        getFactsSync()
    }

    suspend fun saveFact(key: String, value: String, category: String = "general"): FactItem = withContext(Dispatchers.IO) {
        val fact = FactItem(key = key, value = value, category = category)
        saveFactSync(fact)
        fact
    }

    suspend fun getRecentMessages(limit: Int = 20): List<ChatMessage> = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_MESSAGES, null) ?: return@withContext emptyList()
        val type = object : TypeToken<List<ChatMessage>>() {}.type
        try {
            val list: List<ChatMessage> = gson.fromJson(json, type) ?: emptyList()
            list.takeLast(limit).map {
                it.copy(suggestions = it.suggestions ?: emptyList())
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_MESSAGES, null)
        val type = object : TypeToken<List<ChatMessage>>() {}.type
        val list: MutableList<ChatMessage> = try {
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
        list.add(message)
        // Keep last 100 messages
        if (list.size > 100) {
            list.removeAt(0)
        }
        prefs.edit().putString(KEY_MESSAGES, gson.toJson(list)).apply()
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        prefs.edit().remove(KEY_MESSAGES).apply()
    }
}
