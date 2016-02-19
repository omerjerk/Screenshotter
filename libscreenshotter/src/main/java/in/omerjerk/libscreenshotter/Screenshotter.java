package in.omerjerk.libscreenshotter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.io.IOException;

/**
 * Created by omerjerk on 17/2/16.
 */
public class Screenshotter implements TextureView.SurfaceTextureListener {

    private static final String TAG = "LibScreenshotter";

    private VirtualDisplay virtualDisplay;
    private Surface mSurface;

    private int width;
    private int height;

    private Context context;
    private TextureView textureView;

    private int resultCode;
    private Intent data;
    private ScreenshotCallback cb;

    private static Screenshotter mInstance;

    public static Screenshotter getInstance() {
        if (mInstance == null) {
            mInstance = new Screenshotter();
        }
        return mInstance;
    }

    private Screenshotter() {}

    /**
     * Takes the screenshot of whatever currently is on the default display.
     * @param resultCode The result code returned by the request for accessing MediaProjection permission
     * @param data The intent returned by the same request
     */
    public Screenshotter takeScreenshot(Context context, int resultCode, Intent data, ScreenshotCallback cb) {
        this.context = context;
        this.cb = cb;
        this.resultCode = resultCode;
        this.data = data;

        if (textureView == null) {
            textureView = new TextureView(context);
            textureView.setSurfaceTextureListener(this);
        }
        return this;
    }

    public Screenshotter setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable width = " + width + " height = " + height);
        if (mSurface == null) {
            mSurface = new Surface(textureView.getSurfaceTexture());
        }

        MediaProjectionManager mMediaProjectionManager = (MediaProjectionManager)
                context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        MediaProjection mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);

        virtualDisplay = mMediaProjection.createVirtualDisplay("Screenshotter",
                width, height, 50,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mSurface, null, null);

        try {
            Thread.sleep(1000); //Let's wait for 1 sec for the virtual display to start properly
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Bitmap screenshot = textureView.getBitmap(width, height);
        cb.onScreenshot(screenshot);
        mMediaProjection.stop();
        virtualDisplay.release();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureDestroyed");
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureUpdated");
    }
}
