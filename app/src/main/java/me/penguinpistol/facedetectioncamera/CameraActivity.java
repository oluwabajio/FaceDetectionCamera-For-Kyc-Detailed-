package me.penguinpistol.facedetectioncamera;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.os.HandlerCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.face.Face;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import me.penguinpistol.facedetectioncamera.databinding.ActivityCameraBinding;

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = CameraActivity.class.getSimpleName();
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss";
    private static final int REQUEST_CODE_PERMISSION = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[] {
            Manifest.permission.CAMERA
    };

    private ActivityCameraBinding mBinding;
    private ImageCapture imageCapture = null;

    private ScheduledExecutorService captureExecutor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        if(allPermissionGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSION);
        }

        mBinding.imageCaptureButton.setOnClickListener(v -> {
//            takePhoto();

            // 파일로 저장
            String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault()).format(System.currentTimeMillis());
            takePhoto(name);
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        if(captureExecutor == null) {
            Log.i("captureExecutor", "run!!");
//            captureExecutor = Executors.newSingleThreadScheduledExecutor();
//            captureExecutor.scheduleAtFixedRate(() -> {
//                takePhoto();
//            }, 0, 1, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

//        if(captureExecutor != null) {
//            captureExecutor.shutdown();
//            captureExecutor = null;
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_PERMISSION) {
            if(allPermissionGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void takePhoto() {
        //
        if(imageCapture != null) {
            imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
                @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                    super.onCaptureSuccess(imageProxy);
                    Bitmap bitmap = convertBitmap(imageProxy);
                    mBinding.captureImage.setImageBitmap(bitmap);
//                    imageProxy.close();
                    imageProxy.close();
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    super.onError(exception);
                    Log.e(TAG, "takePicture " + exception);
                }
            });
        }
    }

    private void takePhoto(String name) {
        ImageCapture imageCapture = this.imageCapture;
        if(imageCapture == null) {
            return;
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CameraX-Image");
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues).build();


        // 파일로 저장하기
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        String msg = "Photo capture succeeded! >> " + output.getSavedUri();
                        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, msg);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "onError:" + exception);
                    }
                });
    }

    private Bitmap convertBitmap(ImageProxy image) {
        ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
        byteBuffer.rewind();
        byte[] bytes = new byte[byteBuffer.capacity()];
        byteBuffer.get(bytes);
        byte[] clonedBytes = bytes.clone();
        Bitmap result = BitmapFactory.decodeByteArray(clonedBytes, 0, clonedBytes.length);

        int degrees = image.getImageInfo().getRotationDegrees();
        if(degrees > 0) {
            Matrix matrix = new Matrix();
            matrix.setScale(-1, 1);
            matrix.postRotate(90);
            result = Bitmap.createBitmap(result, 0, 0, result.getWidth(), result.getHeight(), matrix, true);
        }

        return result;
    }

    private void startCamera() {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);

        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true);
        int actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());

        Size previewSize = new Size(displaySize.x, displaySize.y);
        Size outputSize = new Size(720, 1280);

        Log.d("CameraActivity", "------------------------------------------------");
        Log.d("CameraActivity", "action bar >> " + actionBarHeight);
        Log.d("CameraActivity", "preview Size >> " + previewSize.toString());
        Log.d("CameraActivity", "------------------------------------------------");

        FaceDetectionAnalyzer faceDetectionAnalyzer = new FaceDetectionAnalyzer(this, new FaceDetectionListener() {
            @Override
            public void onDetected(Face face, RectF targetRect, int rotate) {
                HandlerCompat.createAsync(Looper.getMainLooper()).post(() -> {
                    mBinding.detectionGraphic.setFace(face, targetRect, rotate);
                });
            }
        });

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                Preview preview = new Preview.Builder()
                        .setTargetResolution(previewSize)
                        .build();
                preview.setSurfaceProvider(mBinding.viewFinder.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(previewSize)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), faceDetectionAnalyzer);

                imageCapture = new ImageCapture.Builder()
                        .setTargetResolution(outputSize)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

        }, ContextCompat.getMainExecutor(this));
    }

    private boolean allPermissionGranted() {
        for(String permission : REQUIRED_PERMISSIONS) {
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}