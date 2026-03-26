package com.example.myapplication.utils;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class ImagePickerHelper {

    public interface Listener {
        void onImagePicked(@NonNull Uri uri);
        void onFilePicked(@NonNull Uri uri);
        void onPermissionDenied();
    }

    private enum PendingAction {
        NONE,
        PICK_IMAGE,
        PICK_FILE
    }

    private final AppCompatActivity activity;
    private final Listener listener;

    private final ActivityResultLauncher<String> permissionLauncher;
    private final ActivityResultLauncher<PickVisualMediaRequest> pickVisualMediaLauncher;
    private final ActivityResultLauncher<Intent> legacyImagePickerLauncher;
    private final ActivityResultLauncher<String> filePickerLauncher;

    private PendingAction pendingAction = PendingAction.NONE;

    public ImagePickerHelper(@NonNull AppCompatActivity activity, @NonNull Listener listener) {
        this.activity = activity;
        this.listener = listener;

        permissionLauncher = activity.registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            PendingAction action = pendingAction;
            pendingAction = PendingAction.NONE;
            if (!granted) {
                listener.onPermissionDenied();
                return;
            }
            if (action == PendingAction.PICK_IMAGE) {
                launchImagePickerInternal();
            } else if (action == PendingAction.PICK_FILE) {
                launchFilePickerInternal();
            }
        });

        pickVisualMediaLauncher = activity.registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                listener.onImagePicked(uri);
            }
        });

        legacyImagePickerLauncher = activity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                listener.onImagePicked(result.getData().getData());
            }
        });

        filePickerLauncher = activity.registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                listener.onFilePicked(uri);
            }
        });
    }

    public void pickImage() {
        if (needsReadPermission() && !hasReadPermission()) {
            pendingAction = PendingAction.PICK_IMAGE;
            permissionLauncher.launch(getReadPermission());
            return;
        }
        launchImagePickerInternal();
    }

    public void pickFile() {
        if (needsReadPermission() && !hasReadPermission()) {
            pendingAction = PendingAction.PICK_FILE;
            permissionLauncher.launch(getReadPermission());
            return;
        }
        launchFilePickerInternal();
    }

    private void launchImagePickerInternal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pickVisualMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            legacyImagePickerLauncher.launch(intent);
        }
    }

    private void launchFilePickerInternal() {
        filePickerLauncher.launch("*/*");
    }

    private boolean hasReadPermission() {
        return ContextCompat.checkSelfPermission(activity, getReadPermission()) == PackageManager.PERMISSION_GRANTED;
    }

    private String getReadPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private boolean needsReadPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }
}

