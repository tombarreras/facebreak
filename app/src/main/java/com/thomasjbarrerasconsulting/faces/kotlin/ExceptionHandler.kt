/*
 * Copyright 2021 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AlertDialog
import java.lang.Exception

class ExceptionHandler {
    companion object{
        @JvmStatic
        fun Alert(context: Context, message:String, tag:String, e:Exception){
            val fullMessage = "$message: ${e.message}"
            Log.e(tag, fullMessage)

            AlertDialog.Builder(context)
                .setTitle("Exception")
                .setMessage(fullMessage)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        }
    }
}