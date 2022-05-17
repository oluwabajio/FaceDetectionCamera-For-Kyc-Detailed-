package me.penguinpistol.facedetectioncamera;

import com.google.mlkit.vision.face.Face;

public interface FaceDetectionListener {
    void onDetected(Face face, int rotate);
}
