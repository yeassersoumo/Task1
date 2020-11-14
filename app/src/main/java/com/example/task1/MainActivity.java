package com.example.task1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends AppCompatActivity {
    private static String DOWNLOAD_URL = "http://dropbox.sandbox2000.com/intrvw/SampleVideo_1280x720_30mb.mp4";

    NotificationManagerCompat notificationManager;
    NotificationCompat.Builder builder;
    int notificationId = 1;
    int PROGRESS_MAX = 100;

    private boolean isPaused;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static void verifyStoragePermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        verifyStoragePermissions(this);

        notificationManager = NotificationManagerCompat.from(this);
        builder = new NotificationCompat.Builder(this, NotificationChannel.DEFAULT_CHANNEL_ID);
        builder.setContentTitle("Download")
                .setContentText("Download in progress")
                .setSmallIcon(R.drawable.ic_baseline_arrow_downward_24)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        int PROGRESS_MAX = 100;
        int PROGRESS_CURRENT = 0;
        builder.setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);

        findViewById(R.id.download_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DownloadFileAsyncTask().execute(DOWNLOAD_URL);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        isPaused = true;
        notificationManager.notify(notificationId, builder.build());
    }

    @Override
    protected void onResume() {
        super.onResume();
        isPaused = false;
        notificationManager.cancel(notificationId);
    }

    public class DownloadFileAsyncTask extends AsyncTask<String, Integer, String> {

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_horizontal);

        @Override
        protected String doInBackground(String... strings) {
            int count;
            try {
                URL url = new URL(strings[0]);
                URLConnection connection = url.openConnection();
                connection.connect();
                int lenghtOfFile = connection.getContentLength();

                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                OutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory() + "/Download/file.mp4");

                byte[] data = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress((int) ((total * 100) / lenghtOfFile));
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return "Task Completed.";
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setProgress(0);
            Toast.makeText(MainActivity.this, "Download will start shortly!", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (isPaused) {
                builder.setContentText("Download complete")
                        .setProgress(0, 0, false);
                notificationManager.notify(notificationId, builder.build());
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressBar.setProgress(values[0]);
            TextView textView = findViewById(R.id.length);
            textView.setText(values[0]+"%");

            if (isPaused) {
                builder.setContentText("Download in progress  " + values[0]+"%")
                        .setProgress(PROGRESS_MAX, values[0], false);
                notificationManager.notify(notificationId, builder.build());


            }
        }

    }
}

