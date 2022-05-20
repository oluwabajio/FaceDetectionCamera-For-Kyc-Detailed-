package me.penguinpistol.facedetectioncamera;

import com.google.mlkit.vision.common.InputImage;

public interface FaceDetectionListener {
    void onDetected(InputImage inputImage);
}
