package in.omerjerk.libscreenshotter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.io.IOException;

/**
 * Created by omerjerk on 17/2/16.
 */
public class Screenshotter {

    private static final String TAG = "LibScreenshotter";

    private VirtualDisplay virtualDisplay;
    private Surface mSurface;

    private int width;
    private int height;

    private TextureView textureView;

    private static Screenshotter mInstance;

    public Screenshotter getInstance() {
        if (mInstance == null) {
            mInstance = new Screenshotter();
        }
        return mInstance;
    }

    /**
     * Takes the screenshot of whatever currently is on the default display.
     * @param resultCode The result code returned by the request for accessing MediaProjection permission
     * @param data The intent returned by the same request
     */
    private Bitmap takeScreenshot(Context context, int resultCode, Intent data, int width, int height)
            throws IOException{
        MediaProjectionManager mMediaProjectionManager = (MediaProjectionManager)
                context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        MediaProjection mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);

        this.width = width;
        this.height = height;

        textureView = new TextureView(context);
        mSurface = new Surface(textureView.getSurfaceTexture());

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
        mMediaProjection.stop();
        virtualDisplay.release();

        return screenshot;
    }
}
