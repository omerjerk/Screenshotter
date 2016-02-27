package in.omerjerk.screenshotter;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import in.omerjerk.libscreenshotter.ScreenshotCallback;
import in.omerjerk.libscreenshotter.Screenshotter;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_MEDIA_PROJECTION = 1;

    private static final int REQUEST_EXTERNAL_STORAGE = 2;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final String TAG = "ScreenshotterExample";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void takeScreenshot(View v) {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Screenshotter.getInstance()
                    .setSize(720, 1280)
                    .takeScreenshot(this, resultCode, data, new ScreenshotCallback() {
                        @Override
                        public void onScreenshot(Bitmap bitmap) {
                            try {
                                Log.d(TAG, "onScreenshot called");
                                verifyStoragePermissions(MainActivity.this);
                                File file = new File(Environment.getExternalStorageDirectory(), "test.jpeg");
                                FileOutputStream out = new FileOutputStream(file);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                Toast.makeText(MainActivity.this, "Screenshot Captured!", Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
        } else {
            Toast.makeText(this, "You denied the permission.", Toast.LENGTH_SHORT).show();
        }
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}
