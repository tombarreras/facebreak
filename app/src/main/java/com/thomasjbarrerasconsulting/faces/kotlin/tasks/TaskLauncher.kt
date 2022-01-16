package com.thomasjbarrerasconsulting.faces.kotlin.tasks

import com.thomasjbarrerasconsulting.faces.kotlin.ExceptionHandler
import com.thomasjbarrerasconsulting.faces.kotlin.FaceBreakApplication
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TaskLauncher {

    private val tasks = ThreadSafeTaskMap()

    fun update(taskType:String, task: () -> Unit){
        tasks.update(taskType, task)
    }

    fun launchAll(){
        try {
            runBlocking {
                do {
                    val task = tasks.pop()
                    if (task != null){
                        launch {
                            task()
                        }
                    }

                } while (task != null)
            }

        } catch (e: Exception){
            ExceptionHandler.alert(FaceBreakApplication.instance, "Failed to execute tasks", TAG, e)
        }
    }

    companion object {
        private const val TAG = "AsyncTaskRunner"
    }
}