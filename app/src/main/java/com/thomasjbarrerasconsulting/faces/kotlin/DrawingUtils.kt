/*
 * Copyright 2021 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin

import android.graphics.Paint

class DrawingUtils {
    companion object {
        fun splitText(text: String, measurer: Paint, canvasWidth: Int): List<String>{
            val words = text.split(' ').toMutableList()
            val sentences = mutableListOf<String>()
            var sentence = words.first()
            words.removeAt(0)

            while (words.any()){
                if (measurer.measureText(sentence + " " + words.first()) > canvasWidth){
                    sentences.add(sentence)
                    sentence = "    " + words.first()
                } else {
                    sentence = sentence + " " + words.first()
                }
                words.removeAt(0)
            }

            sentences.add(sentence)
            return sentences.toList()
        }
    }
}