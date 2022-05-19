package me.penguinpistol.facedetectioncamera;

import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.Locale;

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
    private final GraphicOverlay graphic;
    private final FaceDetectionListener mListener;

    public FaceDetectionAnalyzer(GraphicOverlay graphic, FaceDetectionListener l) {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build();

        mDetector = FaceDetection.getClient(options);
        mListener = l;

        this.graphic = graphic;
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
                        if (faces.size() > 0) {
                            Face face = faces.get(0);
                            FaceContour contour = face.getContour(FaceContour.FACE);

                            if(contour != null) {
                                graphic.setFace(contour);
                                if (checkFace(face)) {
                                    Log.d(TAG, "OK!!!!!!");
                                }
                            }
                        }
                    })
                    .addOnFailureListener(Throwable::printStackTrace)
                    .addOnCompleteListener(task -> imageProxy.close());
        }
    }

    private boolean checkFace(Face face) {
        Rect bound = face.getBoundingBox();

//        Log.d("CameraActivity", getRectInfo(bound));

        return false;
    }

    private String getRectInfo(RectF rect) {
        return String.format(Locale.getDefault(),
                "Rect[%f, %f, %f, %f]",
                rect.left, rect.top, rect.right, rect.bottom);
    }
}
