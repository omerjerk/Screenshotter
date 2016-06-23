package in.omerjerk.libscreenshotter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;

/**
 * Created by omerjerk on 17/2/16.
 */
public class Screenshotter implements ImageReader.OnImageAvailableListener {

    private static final String TAG = "LibScreenshotter";

    private VirtualDisplay virtualDisplay;

    private int width;
    private int height;

    private Context context;

    private int resultCode;
    private Intent data;
    private ScreenshotCallback cb;

    private static Screenshotter mInstance;

    private ImageReader mImageReader;
    private MediaProjection mMediaProjection;
    private volatile int imageAvailable = 0;

    /**
     * Get the single instance of the Screenshotter class.
     * @return the instance
     */
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
    public Screenshotter takeScreenshot(Context context, int resultCode, Intent data, final ScreenshotCallback cb) {
        this.context = context;
        this.cb = cb;
        this.resultCode = resultCode;
        this.data = data;

        imageAvailable = 0;
        mImageReader = ImageReader.newInstance(width, height, ImageFormat.RGB_565, 2);
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) context
                .getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        if (mMediaProjection == null) {
            mMediaProjection = mediaProjectionManager.getMediaProjection(this.resultCode, this.data);
            if (mMediaProjection == null) {
                Log.e(TAG, "MediaProjection null. Cannot take the screenshot.");
            }
        }
        try {
            virtualDisplay = mMediaProjection.createVirtualDisplay("Screenshotter",
                    width, height, 50,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mImageReader.getSurface(), null, null);
            mImageReader.setOnImageAvailableListener(Screenshotter.this, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return this;
    }

    /**
     * Set the size of the screenshot to be taken
     * @param width width of the requested bitmap
     * @param height height of the request bitmap
     * @return the singleton instance
     */
    public Screenshotter setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        synchronized (this) {
            ++imageAvailable;
            if (imageAvailable != 2) {
                reader.acquireLatestImage().close();
                return;
            }
        }
        Image image = reader.acquireLatestImage();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        // create bitmap
        Bitmap bitmap = Bitmap.createBitmap(width+rowPadding/pixelStride, height, Bitmap.Config.RGB_565);
        bitmap.copyPixelsFromBuffer(buffer);
        tearDown();
        image.close();

        cb.onScreenshot(bitmap);
    }

    private void tearDown() {
        virtualDisplay.release();
        mMediaProjection.stop();
        mMediaProjection = null;
        mImageReader = null;
    }
}
