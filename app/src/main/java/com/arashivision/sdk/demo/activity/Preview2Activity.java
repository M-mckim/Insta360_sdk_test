package com.arashivision.sdk.demo.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ToggleButton;

import com.arashivision.sdk.demo.R;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.arashivision.sdkcamera.camera.callback.IPreviewStatusListener;
import com.arashivision.sdkmedia.player.capture.CaptureParamsBuilder;
import com.arashivision.sdkmedia.player.capture.InstaCapturePlayerView;
import com.arashivision.sdkmedia.player.listener.PlayerViewListener;

import androidx.annotation.Nullable;

/**
 * 预览时，传参Surface，可在自定义画布上显示预览画面
 * InstaCapturePlayerView可以切换显示隐藏画面，并保持接收手势操作
 *
 * When previewing, set Surface to display the preview stream on the custom canvas
 * InstaCapturePlayerView can switch to show or hidden and keep receiving gestures
 */
public class Preview2Activity extends BaseObserveCameraActivity implements IPreviewStatusListener {

    private InstaCapturePlayerView mCapturePlayerView;
    private ViewGroup mLayoutSurfaceContainer; // Just for custom surface
    private ToggleButton mBtnPreview;
    private ToggleButton mBtnPlayer;
    private SurfaceView mSurfaceView; // Just for custom surface

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview2);
        setTitle(R.string.preview2_toolbar_title);
        bindViews();

        InstaCameraManager.getInstance().setPreviewStatusChangedListener(this);
        InstaCameraManager.getInstance().startPreviewStream();
    }

    private void bindViews() {
        mLayoutSurfaceContainer = findViewById(R.id.layout_surface_container);
        mCapturePlayerView = findViewById(R.id.player_capture);
        mCapturePlayerView.setLifecycle(getLifecycle());

        mBtnPreview = findViewById(R.id.btn_preview);
        mBtnPreview.setOnClickListener(v -> {
            if (mBtnPreview.isChecked()) {
                InstaCameraManager.getInstance().startPreviewStream();
            } else {
                InstaCameraManager.getInstance().closePreviewStream();
            }
        });

        mBtnPlayer = findViewById(R.id.btn_player);
        mBtnPlayer.setOnClickListener(v -> {
            if (mBtnPlayer.isChecked()) {
                mCapturePlayerView.hideFrame();
            } else {
                mCapturePlayerView.showFrame();
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
        mBtnPreview.setChecked(true);
        mBtnPlayer.setChecked(false);
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
                mBtnPlayer.setEnabled(true);
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
                .setCameraRenderSurfaceInfo(mSurfaceView.getHolder().getSurface(), mSurfaceView.getWidth(), mSurfaceView.getHeight());
        Log.d("RESOLUTION TEST", "Captured RESOLUTION! Width: " + builder.getWidth());


        return builder;
    }

    // 每次开启新预览时，也要创建新的Surface传进参数，不能复用Surface
    // Every time you open a new preview, you must also create a new Surface to set the arguments, you cannot reuse the Surface
    private void createSurfaceView() {
        if (mSurfaceView == null) {
            mSurfaceView = new SurfaceView(this);
            mLayoutSurfaceContainer.addView(mSurfaceView, new FrameLayout.LayoutParams(-1, -1));
        }
    }

    @Override
    public void onIdle() {
        // Preview Stopped
        mCapturePlayerView.destroy();
        mCapturePlayerView.setKeepScreenOn(false);
        mBtnPlayer.setEnabled(false);
        mBtnPlayer.setChecked(false);
        mBtnPreview.setChecked(false);

        // Destroy SurfaceView
        if (mSurfaceView != null) {
            mSurfaceView.getHolder().getSurface().release();
            mLayoutSurfaceContainer.removeView(mSurfaceView);
            mSurfaceView = null;
        }
    }

    @Override
    public void onError() {
        // Preview Failed
        mBtnPlayer.setEnabled(false);
        mBtnPlayer.setChecked(false);
        mBtnPreview.setChecked(false);
    }

}
