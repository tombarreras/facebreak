# FaceBreak

An Android app for facial analysis using TensorFlow Lite models.

## Setup

### ML Models

The TensorFlow Lite model files (`.tflite`) are stored in a separate private repository and are **not** included in this repo. You must obtain access before building the project.

To request access, contact the repository owner.

Once you have the model files, place them in:

```
app/src/main/ml/
```

The following models are required:

- `AgeModel5.tflite`
- `AncestryModel9.tflite`
- `CharacterFlawsModel3.tflite`
- `CharacterModel4.tflite`
- `EmotionsModel1600.tflite`
- `EyeColorModel3.tflite`
- `EyebrowsModel.tflite`
- `FaceShapeModel1000d.tflite`
- `FeaturesFaceModel5.tflite`
- `GenderModel2.tflite`
- `HairColorModel8.tflite`
- `HairStyleModel4.tflite`
- `HeadwearModel3.tflite`
- `JawModel5.tflite`
