package me.penguinpistol.facedetectioncamera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

public class FaceDetectionAnalyzer implements ImageAnalysis.Analyzer {

    private static final String TAG = "FaceDetectionAnalyzer";

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    private final FaceDetector mDetector;
    private final FaceDetectionListener mListener;

    private final Point displaySize;
    private final RectF targetRect;

    public FaceDetectionAnalyzer(Activity activity, FaceDetectionListener l) {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build();

        mDetector = FaceDetection.getClient(options);
        mListener = l;

        displaySize = new Point();
        activity.getWindowManager().getDefaultDisplay().getSize(displaySize);

        float marginHorizontal = displaySize.x * 0.24f;
        float heightRatio = 1.4f;

        targetRect = new RectF(
                marginHorizontal
                , 0
                , displaySize.x - marginHorizontal
                , 0
        );
        float height = targetRect.width() * heightRatio;
        targetRect.top = (displaySize.y >> 1) - height * 0.5f;
        targetRect.bottom = targetRect.top + height;
    }

    @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if(mediaImage != null) {
            int rotate = imageProxy.getImageInfo().getRotationDegrees();
            InputImage inputImage = InputImage.fromMediaImage(mediaImage, rotate);

            mDetector.process(inputImage)
                    .addOnSuccessListener(faces -> {
                        if(mListener != null) {
                            if (faces.size() > 0) {
                                Face face = faces.get(0);
                                if(checkFace(face)) {
                                    Log.d(TAG, "OK!!!!!!");
                                }
                                mListener.onDetected(face, targetRect, rotate);
                            }
                        }
                    })
                    .addOnFailureListener(Throwable::printStackTrace)
                    .addOnCompleteListener(task -> imageProxy.close());
        }
    }

    private boolean checkFace(Face face) {
        Rect bound = face.getBoundingBox();



        return false;
    }
}
