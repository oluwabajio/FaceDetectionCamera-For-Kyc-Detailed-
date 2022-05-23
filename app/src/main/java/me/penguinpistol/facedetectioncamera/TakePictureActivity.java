package me.penguinpistol.facedetectioncamera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import me.penguinpistol.facedetectioncamera.databinding.ActivityTakePictureBinding;

public class TakePictureActivity extends AppCompatActivity {
    private static final String TAG = TakePictureActivity.class.getSimpleName();
    private static final int REQUEST_CODE_PERMISSION = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[] {
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        me.penguinpistol.facedetectioncamera.databinding.ActivityTakePictureBinding mBinding = ActivityTakePictureBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        CameraViewModel mViewModel = new ViewModelProvider(this).get(CameraViewModel.class);
        mViewModel.getIsFinished().observe(this, isFinished -> {
            if(isFinished) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, new TakePictureResultFragment())
                        .commit();
            }
        });

        if(allPermissionGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_PERMISSION) {
            if(allPermissionGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "카메라 권한을 허용 해주셔야 서비스 이용이 가능합니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new TakePictureFragment())
                .commit();
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