package com.thomasjbarrerasconsulting.faces.kotlin.facedetector

import com.google.mlkit.vision.face.Face


class FaceWithClassifications(val face: Face, val classifications: List<String>){
}