package me.penguinpistol.facedetectioncamera;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import me.penguinpistol.facedetectioncamera.databinding.FragmentTakePictureResultBinding;

public class TakePictureResultFragment extends Fragment {

    private FragmentTakePictureResultBinding mBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = FragmentTakePictureResultBinding.inflate(inflater, container, false);
        CameraViewModel viewModel = new ViewModelProvider(requireActivity()).get(CameraViewModel.class);

        for(FaceChecker.Direction dir : viewModel.getTakePictures().keySet()) {
            setImage(dir, viewModel.getTakePictures().get(dir));
        }

        return mBinding.getRoot();
    }

    private void setImage(FaceChecker.Direction dir, Bitmap bitmap) {
        switch(dir) {
            case FRONT:
                mBinding.ivFront.setImageBitmap(bitmap);
                break;
            case LEFT_30:
                mBinding.ivL30.setImageBitmap(bitmap);
                break;
            case LEFT_45:
                mBinding.ivL45.setImageBitmap(bitmap);
                break;
            case RIGHT_30:
                mBinding.ivR30.setImageBitmap(bitmap);
                break;
            case RIGHT_45:
                mBinding.ivR45.setImageBitmap(bitmap);
                break;
        }
    }
}