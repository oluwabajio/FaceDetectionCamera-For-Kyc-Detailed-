package me.penguinpistol.facedetectioncamera;

import android.graphics.PointF;
import android.graphics.RectF;
import android.media.Image;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.material.math.MathUtils;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;
import java.util.Locale;

public class FaceDetectionAnalyzer implements ImageAnalysis.Analyzer {
    private static final String TAG = "FaceDetectionAnalyzer";

    private final FaceDetector mDetector;
    private final GraphicOverlay mGraphic;
    private final FaceDetectionListener mListener;

    private final Size imageSize;
    private final RectF targetRect;

    private boolean isDetected = false;

    public void setDetected(boolean isDetected) {
        this.isDetected = isDetected;
    }

    public FaceDetectionAnalyzer(Size imageSize, float targetSize, GraphicOverlay graphic, FaceDetectionListener l) {
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build();

        mDetector = FaceDetection.getClient(options);
        mGraphic = graphic;
        mListener = l;

        this.imageSize = imageSize;
        this.targetRect = new RectF(
                0,
                0,
                imageSize.getWidth() * targetSize,
                imageSize.getWidth() * targetSize * 1.4f
        );
        targetRect.offset(
                (imageSize.getWidth()  - targetRect.width()) * 0.5f,
                (imageSize.getHeight()  - targetRect.height()) * 0.5f
        );

        mGraphic.init(imageSize, targetRect);
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

                            if (!isDetected && checkFace(face)) {
                                Log.d(TAG, "analyze: OK!!");
                                isDetected = true;
                                mListener.onDetected(inputImage);
//                                mListener.onDetected(inputImage);
                            }

                            FaceContour contour = face.getContour(FaceContour.FACE);
                            if(contour != null) {
                                mGraphic.setFaceContour(contour);
                            }
                        }
                    })
                    .addOnFailureListener(Throwable::printStackTrace)
                    .addOnCompleteListener(task -> imageProxy.close());
        }
    }

    private boolean checkFace(Face face) {
        FaceContour contour = face.getContour(FaceContour.FACE);

        if(contour == null) {
            return false;
        }

        RectF bound = getFaceRect(contour.getPoints());

        float widthRatio = bound.width() / targetRect.width();
        float centerDistance = MathUtils.dist(bound.centerX(), bound.centerY(), targetRect.centerX(), targetRect.centerY());

        float angleX = Math.abs(face.getHeadEulerAngleX());
        float angleY = Math.abs(face.getHeadEulerAngleY());
        float angleZ = Math.abs(face.getHeadEulerAngleZ());

        String debugText = String.format(Locale.getDefault(),
                "bound : %f\ntarget : %f\nratio : %f\nbound center: [%f, %f]\ntarget center[%f, %f]\ndistance: %f\nangle[%f, %f, %f]",
                bound.width(), targetRect.width(), widthRatio,
                bound.centerX(), bound.centerY(), targetRect.centerX(), targetRect.centerY(), centerDistance,
                angleX, angleY, angleZ
                );
        mGraphic.setDebugText(debugText);

        boolean checkRatio = (0.95f < widthRatio && widthRatio < 1.05f);
        boolean checkDistance = (centerDistance < 30);
        boolean checkAngle = (angleX < 7 && angleY < 7 && angleZ < 7);

        return checkRatio && checkDistance && checkAngle;
    }

    private RectF getFaceRect(List<PointF> landmarks) {
        return new RectF(
                (float)landmarks.stream().mapToDouble(x -> x.x).min().orElse(0),
                (float)landmarks.stream().mapToDouble(x -> x.y).min().orElse(0),
                (float)landmarks.stream().mapToDouble(x -> x.x).max().orElse(0),
                (float)landmarks.stream().mapToDouble(x -> x.y).max().orElse(0)
        );
    }
}
