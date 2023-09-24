/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin

import android.util.Log
import com.thomasjbarrerasconsulting.faces.kotlin.Toaster.Companion.toast

class ObservableList<T> () {
    val list = mutableListOf<T>()
    private val listeners = mutableListOf<ListUpdatedListener<T>>()

    interface ListUpdatedListener<T>{
        fun listUpdated(list:List<T>)
    }

    fun items(): List<T>{
        return list.toList()
    }

    fun updateIfDifferent(updatedList: List<T>){
        var differencesDetected = false
        synchronized(this){
            differencesDetected = (list.count() != updatedList.count()) || list.map{ it.toString() } != updatedList.map { it.toString() }
            if (differencesDetected) {
                list.clear()
                list.addAll(updatedList)
            }
        }
        Log.d(TAG, "List: $list")

        if (differencesDetected){
            for (listener in listeners){
                try {
                    listener.listUpdated(items())
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                }
            }
        }
    }

    fun addListener(listener: ListUpdatedListener<T>){
        synchronized(this){
            if (! listeners.contains(listener)){
                listeners.add(listener)
            }
        }
    }

    fun removeListener(listener: ListUpdatedListener<T>){
        synchronized(this){
            if (listeners.contains(listener)){
                listeners.remove(listener)
            }
        }
    }

    companion object {
        private const val TAG = "ObservableList"
    }
}