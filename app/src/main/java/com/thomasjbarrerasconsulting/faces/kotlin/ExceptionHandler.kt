/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.lang.Exception

class ExceptionHandler {
    companion object{
        @JvmStatic
        fun alert(context: Context, message:String, tag:String, e:Exception){
            val fullMessage = "$message: ${e.message}"
            Log.e(tag, fullMessage)

            Toast.makeText(context, fullMessage, Toast.LENGTH_LONG).show()
        }
    }
}