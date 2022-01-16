package com.thomasjbarrerasconsulting.faces.kotlin.tasks

class ThreadSafeTaskMap{
    private val map = mutableMapOf<String, () -> Unit>()

    fun update(key: String, value: () -> Unit){
        synchronized(this){
            if (!map.containsKey(key)){
                map[key] = value
            }
        }
    }

    fun pop(): (() -> Unit)? {
        synchronized(this){
            if (map.isNotEmpty()){
                val entry = map.entries.first()
                map.remove(entry.key)
                return entry.value
            }
            return null
        }
    }
}