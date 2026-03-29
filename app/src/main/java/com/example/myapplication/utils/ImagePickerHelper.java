package com.example.myapplication.utils;

import android.net.Uri;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

public class ImagePickerHelper {

    public interface Listener {
        void onImagePicked(@NonNull Uri uri);
        void onFilePicked(@NonNull Uri uri);
        void onPermissionDenied();
    }

    private final ActivityResultLauncher<String> pickImageLauncher;
    private final ActivityResultLauncher<String[]> pickFileLauncher;

    public ImagePickerHelper(@NonNull ComponentActivity activity, @NonNull Listener listener) {
        pickImageLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        listener.onImagePicked(uri);
                    }
                }
        );

        pickFileLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        listener.onFilePicked(uri);
                    }
                }
        );
    }

    public void pickImage() {
        pickImageLauncher.launch("image/*");
    }

    public void pickFile() {
        pickFileLauncher.launch(new String[]{"*/*"});
    }
}
