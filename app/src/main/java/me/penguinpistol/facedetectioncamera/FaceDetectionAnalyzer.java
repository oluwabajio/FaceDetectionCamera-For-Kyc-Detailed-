package me.penguinpistol.facedetectioncamera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

public class FaceDetectionAnalyzer implements ImageAnalysis.Analyzer {

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private final Activity mActivity;
    private final FaceDetector mDetector;
    private final FaceDetectionListener mListener;

    public FaceDetectionAnalyzer(Activity activity, FaceDetectionListener l) {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build();

        mDetector = FaceDetection.getClient(options);
        mActivity = activity;
        mListener = l;
    }

    @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if(mediaImage != null) {
//            int rotate;
//            try {
//                rotate = getRotationCompensation("0", mActivity, true);
//            } catch (CameraAccessException e) {
//                rotate = imageProxy.getImageInfo().getRotationDegrees();
//            }
            int rotate = imageProxy.getImageInfo().getRotationDegrees();
            InputImage inputImage = InputImage.fromMediaImage(mediaImage, rotate);

            mDetector.process(inputImage)
                    .addOnSuccessListener(faces -> {
                        if(mListener != null) {
                            if (faces.size() > 0) {
                                Face face = faces.get(0);
                                mListener.onDetected(face, rotate);
                            }
                        }
                    })
                    .addOnFailureListener(Throwable::printStackTrace)
                    .addOnCompleteListener(task -> imageProxy.close());
        }
    }

    @OptIn(markerClass = androidx.camera.core.ExperimentalAnalyzer.class)
    @Nullable
    @Override
    public Size getTargetResolutionOverride() {
        return null;
    }

    @OptIn(markerClass = androidx.camera.core.ExperimentalAnalyzer.class)
    @Override
    public int getTargetCoordinateSystem() {
        return 0;
    }

    @Override
    public void updateTransform(@Nullable Matrix matrix) {

    }

    private int getRotationCompensation(String cameraId, Activity activity, boolean isFrontFacing) throws CameraAccessException {
        int deviceRotate = activity.getWindowManager().getDefaultDisplay().getRotation();
        int rotationCompensation = ORIENTATIONS.get(deviceRotate);

        CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        int sensorOrientation = cameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.SENSOR_ORIENTATION);

        if(isFrontFacing) {
            rotationCompensation = (sensorOrientation + rotationCompensation) % 360;
        } else {
            rotationCompensation = (sensorOrientation - rotationCompensation + 360) % 360;
        }

        return rotationCompensation;
    }
}
