package com.example.myapplication.ui;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.myapplication.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ImageViewerActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "extra_image_url";
    public static final String EXTRA_FILE_NAME = "extra_file_name";
    private static final String TAG = "ImageViewerActivity";

    private ImageView ivFullImage;
    private ProgressBar pbImage;
    private ProgressBar pbDownload;
    private View btnDownload;
    private View btnClose;

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final OkHttpClient downloadClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        ivFullImage = findViewById(R.id.ivFullImage);
        pbImage = findViewById(R.id.pbImageLoading);
        pbDownload = findViewById(R.id.pbDownload);
        btnDownload = findViewById(R.id.btnDownloadImage);
        btnClose = findViewById(R.id.btnClose);

        Intent intent = getIntent();
        String imageUrl = intent != null ? intent.getStringExtra(EXTRA_IMAGE_URL) : null;
        String fileName = intent != null ? intent.getStringExtra(EXTRA_FILE_NAME) : null;

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            Toast.makeText(this, getString(R.string.chat_file_url_missing), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnClose.setOnClickListener(v -> onBackPressed());
        btnDownload.setOnClickListener(v -> startDownload(imageUrl, fileName));

        loadImage(imageUrl);
    }

    private void loadImage(String imageUrl) {
        pbImage.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_broken_image)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        pbImage.setVisibility(View.GONE);
                        Log.e(TAG, "Load image failed url=" + model, e);
                        Toast.makeText(ImageViewerActivity.this, getString(R.string.chat_file_download_failed), Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        pbImage.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(ivFullImage);
    }

    private void startDownload(String imageUrl, String fileName) {
        btnDownload.setEnabled(false);
        pbDownload.setVisibility(View.VISIBLE);
        ioExecutor.execute(() -> {
            File saved = null;
            Exception error = null;
            try {
                saved = downloadToDownloads(imageUrl, fileName);
            } catch (Exception e) {
                error = e;
                Log.e(TAG, "Download failed url=" + imageUrl, e);
            }

            File finalSaved = saved;
            Exception finalError = error;
            runOnUiThread(() -> {
                pbDownload.setVisibility(View.GONE);
                btnDownload.setEnabled(true);
                if (finalError != null || finalSaved == null) {
                    Toast.makeText(ImageViewerActivity.this, getString(R.string.chat_image_download_failed), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ImageViewerActivity.this, getString(R.string.chat_image_download_success, finalSaved.getName()), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private File downloadToDownloads(String imageUrl, String fileName) throws Exception {
        String safeName = safeFileName(fileName != null ? fileName : deriveNameFromUrl(imageUrl));
        String mime = safeName.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";

        Request request = new Request.Builder().url(imageUrl).get().build();
        ResponseBody body;
        Response response = downloadClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IllegalStateException("HTTP " + response.code());
        }
        body = response.body();
        if (body == null) throw new IllegalStateException("Empty body");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, safeName);
            values.put(MediaStore.Downloads.MIME_TYPE, mime);
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/HRM");
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IllegalStateException("Cannot create download entry");
            try (OutputStream out = getContentResolver().openOutputStream(uri);
                 InputStream in = body.byteStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
            return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), safeName);
        } else {
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (downloads != null && !downloads.exists()) {
                //noinspection ResultOfMethodCallIgnored
                downloads.mkdirs();
            }
            if (downloads == null) throw new IllegalStateException("Downloads directory not available");
            File outFile = new File(downloads, safeName);
            File tempFile = new File(outFile.getAbsolutePath() + ".download");
            try (InputStream in = body.byteStream(); FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
            if (outFile.exists()) outFile.delete();
            if (!tempFile.renameTo(outFile)) throw new IllegalStateException("Cannot move temp file");
            scanFile(outFile);
            return outFile;
        }
    }

    private String deriveNameFromUrl(String url) {
        try {
            Uri uri = Uri.parse(url);
            String last = uri.getLastPathSegment();
            if (last != null && !last.trim().isEmpty()) {
                return last;
            }
        } catch (Exception ignored) {
        }
        return "image.jpg";
    }

    private String safeFileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "image.jpg";
        }
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void scanFile(File file) {
        try {
            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scanIntent.setData(Uri.fromFile(file));
            sendBroadcast(scanIntent);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdownNow();
    }
}

