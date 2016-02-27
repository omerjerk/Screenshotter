package in.omerjerk.libscreenshotter;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by omerjerk on 19/2/16.
 */
public class EncoderDecoder implements Runnable {

    private int width;
    private int height;
    private String MIME_TYPE = "video/avc";

    private static final String TAG = "EncoderDecoder";

    private MediaCodec encoder;
    private MediaCodec decoder;
    private CodecCallback codecCb;

    private CodecOutputSurface outputSurface;

    boolean VERBOSE = true;

    public EncoderDecoder(int width, int height, CodecCallback cb) {
        this.width = width;
        this.height = height;
        this.codecCb = cb;

        outputSurface = new CodecOutputSurface(width, height);
    }

    public Surface createDisplaySurface() throws IOException {
        MediaFormat encoderFormat = MediaFormat.createVideoFormat(MIME_TYPE,
                width, height);
        encoderFormat.setInteger(MediaFormat.KEY_BIT_RATE, (int) (1024 * 1024 * 0.5));
        encoderFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        encoderFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        encoderFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        if(VERBOSE) Log.i(TAG, "Starting encoder");
        encoder = MediaCodec.createEncoderByType(MIME_TYPE);
        encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        Surface surface = encoder.createInputSurface();
        encoder.start();

        initDecoder();
        return surface;
    }

    private void initDecoder() throws IOException {
        decoder = MediaCodec.createDecoderByType(MIME_TYPE);
    }

    @Override
    public void run() {
        boolean encoderDone = false;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        final int TIMEOUT_USEC = 10000;
        ByteBuffer encodedData;

        boolean decoderConfigured = false;
        boolean outputDone = false;

        while (!outputDone) {
            if (!encoderDone) {
                int encoderStatus = encoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE) Log.d(TAG, "no output from encoder available");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // not expected for an encoder
                    MediaFormat newFormat = encoder.getOutputFormat();
                    if (VERBOSE) Log.d(TAG, "encoder output format changed: " + newFormat);
                } else if (encoderStatus < 0) {
                    //fail("unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
                } else { // encoderStatus >= 0
                    encodedData = encoder.getOutputBuffer(encoderStatus);
                    if (encodedData == null) {
                        //fail("encoderOutputBuffer " + encoderStatus + " was null");
                    }
                    // It's usually necessary to adjust the ByteBuffer values to match BufferInfo.
                    encodedData.position(info.offset);
                    encodedData.limit(info.offset + info.size);
//                encodedSize += info.size;
                    if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        // Codec config info.  Only expected on first packet.  One way to
                        // handle this is to manually stuff the data into the MediaFormat
                        // and pass that to configure().  We do that here to exercise the API.
                        MediaFormat format =
                                MediaFormat.createVideoFormat(MIME_TYPE, width, height);
                        format.setByteBuffer("csd-0", encodedData);
                        decoder.configure(format, outputSurface.getSurface(), null, 0);
                        decoder.start();
                        decoderConfigured = true;
                        if (VERBOSE) Log.d(TAG, "decoder configured (" + info.size + " bytes)");
                    } else {
                        // Get a decoder input buffer, blocking until it's available.
                        //assertTrue(decoderConfigured);
                        int inputBufIndex = decoder.dequeueInputBuffer(-1);
                        ByteBuffer inputBuf = decoder.getInputBuffer(inputBufIndex);
                        inputBuf.clear();
                        inputBuf.put(encodedData);
                        decoder.queueInputBuffer(inputBufIndex, 0, info.size,
                                info.presentationTimeUs, info.flags);
                        encoderDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                        if (VERBOSE) Log.d(TAG, "passed " + info.size + " bytes to decoder"
                                + (encoderDone ? " (EOS)" : ""));
                    }
                    encoder.releaseOutputBuffer(encoderStatus, false);
                }
            }
            // Check for output from the decoder.  We want to do this on every loop to avoid
            // the possibility of stalling the pipeline.  We use a short timeout to avoid
            // burning CPU if the decoder is hard at work but the next frame isn't quite ready.
            //
            // If we're decoding to a Surface, we'll get notified here as usual but the
            // ByteBuffer references will be null.  The data is sent to Surface instead.
            if (decoderConfigured) {
                int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    if (VERBOSE) Log.d(TAG, "no output from decoder available");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // this happens before the first frame is returned
//                decoderOutputFormat = decoder.getOutputFormat();
//                if (VERBOSE) Log.d(TAG, "decoder output format changed: " +
//                        decoderOutputFormat);
                } else if (decoderStatus < 0) {
                    //TODO: fail
                } else {  // decoderStatus >= 0
                    boolean doRender = info.size != 0;
                    if (!doRender) {
                        if (VERBOSE) Log.d(TAG, "got empty frame");
                    }
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (VERBOSE) Log.d(TAG, "output EOS");
                        outputDone = true;
                    }
                    decoder.releaseOutputBuffer(decoderStatus, doRender /*render*/);
                }
            }
        }
    }

    public void stop() {
        encoder.signalEndOfInputStream();
    }
}
