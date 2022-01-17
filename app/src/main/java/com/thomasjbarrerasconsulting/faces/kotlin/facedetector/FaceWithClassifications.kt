/*
 * Copyright 2022 Thomas J. Barreras. All rights reserved.
 * https://www.linkedin.com/in/tombarreras/
*/
package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import com.google.mlkit.vision.face.Face


class FaceWithClassifications(val face: Face, val classifications: List<String>, val classificationType: FaceClassifierProcessor.Classifier){
}