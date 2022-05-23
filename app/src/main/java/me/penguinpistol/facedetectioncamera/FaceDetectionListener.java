package me.penguinpistol.facedetectioncamera;

public interface FaceDetectionListener {
    void onDetected(FaceChecker.Direction direction);
}
