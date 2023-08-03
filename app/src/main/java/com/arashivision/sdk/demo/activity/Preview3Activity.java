package com.arashivision.sdk.demo.activity;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.ToggleButton;

import com.arashivision.sdk.demo.R;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.arashivision.sdkcamera.camera.callback.IPreviewStatusListener;
import com.arashivision.sdkmedia.player.capture.CaptureParamsBuilder;
import com.arashivision.sdkmedia.player.capture.InstaCapturePlayerView;
import com.arashivision.sdkmedia.player.listener.PlayerViewListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.annotation.Nullable;

/**
 * 预览时，使用ImageReader，传参Surface，可获取当前预览画面的Bitmap
 *
 * When previewing, use ImageReader and pass in Surface to get
 * the Bitmap of the current preview stream
 */
public class Preview3Activity extends BaseObserveCameraActivity implements IPreviewStatusListener {

    private final static String TAG = Preview3Activity.class.getName();

    private InstaCapturePlayerView mCapturePlayerView;
    private ToggleButton mBtnSwitch;

    private ImageReader mImageReader;
    private HandlerThread mImageReaderHandlerThread;
    private Handler mImageReaderHandler;

    private byte renderBuf[];

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview3);
        setTitle(R.string.preview3_toolbar_title);
        bindViews();

        // Auto open preview after page gets focus
        mCapturePlayerView.post(() -> {
            InstaCameraManager.getInstance().setPreviewStatusChangedListener(this);
            InstaCameraManager.getInstance().startPreviewStream();
        });
    }

    private void bindViews() {
        mCapturePlayerView = findViewById(R.id.player_capture);
        mCapturePlayerView.setLifecycle(getLifecycle());

        mBtnSwitch = findViewById(R.id.btn_switch);
        mBtnSwitch.setOnClickListener(v -> {
            if (mBtnSwitch.isChecked()) {
                InstaCameraManager.getInstance().startPreviewStream();
            } else {
                InstaCameraManager.getInstance().closePreviewStream();
            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            // Auto close preview after page loses focus
            InstaCameraManager.getInstance().setPreviewStatusChangedListener(null);
            InstaCameraManager.getInstance().closePreviewStream();
            mCapturePlayerView.destroy();
        }
    }

    @Override
    public void onOpening() {
        // Preview Opening
        mBtnSwitch.setChecked(true);
        // If you want to set your custom surface, do like this.
        createSurfaceView();
    }

    @Override
    public void onOpened() {
        // Preview stream is on and can be played
        InstaCameraManager.getInstance().setStreamEncode();
        mCapturePlayerView.setPlayerViewListener(new PlayerViewListener() {
            @Override
            public void onLoadingFinish() {
                InstaCameraManager.getInstance().setPipeline(mCapturePlayerView.getPipeline());
            }

            @Override
            public void onReleaseCameraPipeline() {
                InstaCameraManager.getInstance().setPipeline(null);
            }
        });
        mCapturePlayerView.prepare(createParams());
        mCapturePlayerView.play();
        mCapturePlayerView.setKeepScreenOn(true);
    }

    private CaptureParamsBuilder createParams() {
        CaptureParamsBuilder builder = new CaptureParamsBuilder()
                .setCameraType(InstaCameraManager.getInstance().getCameraType())
                .setMediaOffset(InstaCameraManager.getInstance().getMediaOffset())
                .setMediaOffsetV2(InstaCameraManager.getInstance().getMediaOffsetV2())
                .setMediaOffsetV3(InstaCameraManager.getInstance().getMediaOffsetV3())
                .setCameraSelfie(InstaCameraManager.getInstance().isCameraSelfie())
                .setGyroTimeStamp(InstaCameraManager.getInstance().getGyroTimeStamp())
                .setBatteryType(InstaCameraManager.getInstance().getBatteryType())
                .setCameraRenderSurfaceInfo(mImageReader.getSurface(), mImageReader.getWidth(), mImageReader.getHeight());
        return builder;
    }

    private void createSurfaceView() {
        if (mImageReader != null) {
            return;
        }

        File dir = new File(getExternalCacheDir(), "preview_jpg");
        dir.mkdirs();
        mImageReaderHandlerThread = new HandlerThread("camera render surface");
        mImageReaderHandlerThread.start();

        mImageReaderHandler = new Handler(mImageReaderHandlerThread.getLooper());
        mImageReader = ImageReader.newInstance(mCapturePlayerView.getWidth(), mCapturePlayerView.getHeight(), PixelFormat.RGBA_8888, 1);

        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                Log.i(TAG, "image format " + image.getFormat()
                        + " getWidth " + image.getWidth()
                        + " get height " + image.getHeight()
                        + " timestamp " + image.getTimestamp());
                int planeCount = image.getPlanes().length;


                Log.i(TAG, "plane count " + planeCount);
                Image.Plane plane = image.getPlanes()[0];
                int pixelStride = plane.getPixelStride();
                int rowStride = plane.getRowStride();
                int rowPadding = rowStride - pixelStride * image.getWidth();
                Log.i(TAG, " plane getPixelStride " + pixelStride + " getRowStride " + rowStride);

                Bitmap bitmap = Bitmap.createBitmap(image.getWidth() + rowPadding / pixelStride, image.getHeight(), Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(plane.getBuffer());

                String filePath = dir.getAbsolutePath() + "/" + image.getTimestamp() + ".png";
                File imageFile = new File(filePath);

                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(imageFile);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                    Log.i(TAG, "path " + filePath);
                    try {
                        os.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                image.close();
            }
        }, mImageReaderHandler);
    }

    @Override
    public void onIdle() {
        // Preview Stopped
        mCapturePlayerView.destroy();
        mCapturePlayerView.setKeepScreenOn(false);

        // Stop the thread and destroy the ImageReader
        if (mImageReaderHandlerThread != null) {
            mImageReaderHandlerThread.quit();
            mImageReaderHandlerThread = null;
            mImageReaderHandler = null;
            mImageReader = null;
        }
    }

    @Override
    public void onError() {
        // Preview Failed
        mBtnSwitch.setChecked(false);
    }

}
