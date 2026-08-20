package com.lemonsquad.froggba.cheats;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads and extracts the official Libretro cheat database bundle in background.
 */
public class CheatDownloader {

    private static final String TAG = "FrogEmu_Downloader";
    private static final String CHEATS_BUNDLE_URL = "https://buildbot.libretro.com/assets/frontend/cheats.zip";

    public interface Callback {
        void onProgress(int percent, String message);
        void onComplete(int extractedCount);
        void onError(String error);
    }

    private static final ExecutorService sExecutor = Executors.newSingleThreadExecutor();
    private static volatile boolean sIsDownloading = false;

    public static boolean isDownloading() {
        return sIsDownloading;
    }

    public static int getDownloadedCount(Context context, EmulationSystem system) {
        File dir = new File(context.getFilesDir(), "cheats/" + (system != null ? system.getDirectoryKey() : "gba"));
        if (!dir.exists() || !dir.isDirectory()) return 0;
        File[] files = dir.listFiles((d, name) -> name.endsWith(".cht"));
        return files != null ? files.length : 0;
    }

    public static void startDownload(Context context, Callback callback) {
        if (sIsDownloading) {
            if (callback != null) callback.onError("Download already in progress");
            return;
        }

        sIsDownloading = true;
        Handler mainHandler = new Handler(Looper.getMainLooper());

        sExecutor.execute(() -> {
            HttpURLConnection conn = null;
            int totalExtracted = 0;

            try {
                notifyProgress(mainHandler, callback, 0, "Connecting to Libretro buildbot...");

                URL url = new URL(CHEATS_BUNDLE_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "FrogEmu/1.2.0 (Android Handheld)");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.connect();

                int code = conn.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
                    throw new Exception("HTTP error " + code + ": " + conn.getResponseMessage());
                }

                long totalBytes = conn.getContentLength();
                long bytesReadTotal = 0;

                File baseCheatDir = new File(context.getFilesDir(), "cheats");
                File gbaDir = new File(baseCheatDir, "gba");
                File gbDir = new File(baseCheatDir, "gb");
                File gbcDir = new File(baseCheatDir, "gbc");
                gbaDir.mkdirs();
                gbDir.mkdirs();
                gbcDir.mkdirs();

                InputStream rawIn = conn.getInputStream();
                // Wrap in progress tracking stream
                InputStream progressIn = new InputStream() {
                    long readCount = 0;
                    int lastPercent = -1;

                    @Override
                    public int read() throws java.io.IOException {
                        int b = rawIn.read();
                        if (b != -1) {
                            readCount++;
                            update();
                        }
                        return b;
                    }

                    @Override
                    public int read(byte[] b, int off, int len) throws java.io.IOException {
                        int n = rawIn.read(b, off, len);
                        if (n != -1) {
                            readCount += n;
                            update();
                        }
                        return n;
                    }

                    private void update() {
                        if (totalBytes > 0) {
                            int pct = (int) ((readCount * 100) / totalBytes);
                            if (pct != lastPercent && pct % 2 == 0) {
                                lastPercent = pct;
                                notifyProgress(mainHandler, callback, pct,
                                        String.format("Downloading & Extracting... %d%%", pct));
                            }
                        }
                    }

                    @Override
                    public void close() throws java.io.IOException {
                        rawIn.close();
                    }
                };

                ZipInputStream zis = new ZipInputStream(progressIn);
                ZipEntry entry;
                byte[] buffer = new byte[8192];

                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName();
                    File targetDir = null;

                    if (name.startsWith("Nintendo - Game Boy Advance/") && name.endsWith(".cht")) {
                        targetDir = gbaDir;
                    } else if (name.startsWith("Nintendo - Game Boy/") && name.endsWith(".cht")) {
                        targetDir = gbDir;
                    } else if (name.startsWith("Nintendo - Game Boy Color/") && name.endsWith(".cht")) {
                        targetDir = gbcDir;
                    }

                    if (targetDir != null && !entry.isDirectory()) {
                        String fileName = new File(name).getName();
                        File destFile = new File(targetDir, fileName);
                        try (FileOutputStream fos = new FileOutputStream(destFile)) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                        totalExtracted++;
                    }
                    zis.closeEntry();
                }

                zis.close();
                Log.i(TAG, "Cheats database installed: " + totalExtracted + " files.");

                final int finalCount = totalExtracted;
                sIsDownloading = false;
                mainHandler.post(() -> {
                    if (callback != null) callback.onComplete(finalCount);
                });

            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                sIsDownloading = false;
                final String err = e.getMessage() != null ? e.getMessage() : "Unknown error";
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(err);
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private static void notifyProgress(Handler handler, Callback callback, int percent, String msg) {
        if (callback != null) {
            handler.post(() -> callback.onProgress(percent, msg));
        }
    }
}
