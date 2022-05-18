package me.penguinpistol.facedetectioncamera;

import android.graphics.RectF;

import com.google.mlkit.vision.face.Face;

public interface FaceDetectionListener {
    void onDetected(Face face, RectF targetRect, int rotate);
}
