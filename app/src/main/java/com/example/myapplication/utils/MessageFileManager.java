package com.example.myapplication.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility to manage local storage of chat attachments (images, files).
 * This ensures that sent/received media can be cached locally for immediate display and offline access.
 */
public class MessageFileManager {

    private static final String TAG = "MessageFileManager";
    private static final String MEDIA_DIR = "chat_media";

    /**
     * Saves a file from a Uri to the app's internal persistent storage.
     * @param context Application context
     * @param uri Source Uri of the file
     * @param fileName Target filename (should be unique, e.g., using messageId or original name)
     * @return The local File object if successful, null otherwise.
     */
    public static File saveToLocal(Context context, Uri uri, String fileName) {
        if (uri == null || fileName == null) return null;

        File directory = new File(context.getFilesDir(), MEDIA_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File localFile = new File(directory, fileName);
        
        try (InputStream is = context.getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(localFile)) {
            
            if (is == null) return null;

            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.flush();
            Log.d(TAG, "Saved file to: " + localFile.getAbsolutePath());
            return localFile;

        } catch (IOException e) {
            Log.e(TAG, "Error saving file locally", e);
            return null;
        }
    }

    /**
     * Resolves a local file in the chat media directory.
     * @param context Application context
     * @param fileName The filename to look for
     * @return The File object if it exists, null otherwise.
     */
    public static File getLocalFile(Context context, String fileName) {
        if (fileName == null || fileName.isEmpty()) return null;
        File file = new File(new File(context.getFilesDir(), MEDIA_DIR), fileName);
        return file.exists() ? file : null;
    }

    /**
     * Tries to find a local copy of a remote file based on its URL or metadata.
     * @param context Application context
     * @param remoteUrl The remote URL of the file
     * @return The File object if found locally, null otherwise.
     */
    public static File findLocalCopy(Context context, String remoteUrl) {
        if (remoteUrl == null || remoteUrl.isEmpty()) return null;
        
        // Extract filename from URL
        String fileName = remoteUrl.substring(remoteUrl.lastIndexOf('/') + 1);
        if (fileName.isEmpty()) return null;

        return getLocalFile(context, fileName);
    }
}
