package com.arashivision.sdk.demo.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.ToggleButton;

import com.arashivision.insta360.basecamera.camera.CameraType;
import com.arashivision.sdk.demo.R;
import com.arashivision.sdkcamera.InstaCameraSDK;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.arashivision.sdkcamera.camera.callback.IPreviewStatusListener;
import com.arashivision.sdkcamera.camera.resolution.PreviewStreamResolution;
import com.arashivision.sdkmedia.player.capture.CaptureParamsBuilder;
import com.arashivision.sdkmedia.player.capture.InstaCapturePlayerView;
import com.arashivision.sdkmedia.player.config.InstaStabType;
import com.arashivision.sdkmedia.player.listener.PlayerViewListener;

import androidx.annotation.Nullable;

public class PreviewActivity extends BaseObserveCameraActivity implements IPreviewStatusListener {

    private ViewGroup mLayoutContent;
    private InstaCapturePlayerView mCapturePlayerView;
    private ToggleButton mBtnSwitch;
    private RadioButton mRbNormal;
    private RadioButton mRbFisheye;
    private RadioButton mRbPerspective;
    private RadioButton mRbPlane;
    private Spinner mSpinnerResolution;
    private Spinner mSpinnerStabType;

    private PreviewStreamResolution mCurrentResolution;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        setTitle(R.string.preview_toolbar_title);
        bindViews();

        // 进入页面后可自动开启预览
        // Auto open preview after page gets focus
        InstaCameraManager.getInstance().setPreviewStatusChangedListener(this);
        InstaCameraManager.getInstance().startPreviewStream(PreviewStreamResolution.STREAM_1440_720_30FPS, InstaCameraManager.PREVIEW_TYPE_NORMAL);
        // mSpinnerResolution的onItemSelected会自动触发开启预览，故此处注释掉
        // mSpinnerResolution -> onItemSelected() Will automatically trigger to open the preview, so comment it out here
//        InstaCameraManager.getInstance().startPreviewStream();
    }

    private void bindViews() {
        mLayoutContent = findViewById(R.id.layout_content);
        mCapturePlayerView = findViewById(R.id.player_capture);
        mCapturePlayerView.setLifecycle(getLifecycle());

        mBtnSwitch = findViewById(R.id.btn_switch);
        mBtnSwitch.setOnClickListener(v -> {
            if (mBtnSwitch.isChecked()) {
                if (mCurrentResolution == null) {
                    InstaCameraManager.getInstance().startPreviewStream();
                } else {
                    InstaCameraManager.getInstance().startPreviewStream(mCurrentResolution);
                }
            } else {
                InstaCameraManager.getInstance().closePreviewStream();
            }
        });

        mRbNormal = findViewById(R.id.rb_normal);
        mRbFisheye = findViewById(R.id.rb_fisheye);
        mRbPerspective = findViewById(R.id.rb_perspective);
        mRbPlane = findViewById(R.id.rb_plane);
        RadioGroup radioGroup = findViewById(R.id.rg_preview_mode);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // 在平铺和其他模式之间切换需要重启预览流
            // Need to restart the preview stream when switching between plane and others
            if (checkedId == R.id.rb_plane) {
                InstaCameraManager.getInstance().closePreviewStream();
                if (mCurrentResolution == null) {
                    InstaCameraManager.getInstance().startPreviewStream();
                } else {
                    InstaCameraManager.getInstance().startPreviewStream(mCurrentResolution);
                }
                mRbFisheye.setEnabled(false);
                mRbPerspective.setEnabled(false);
            } else if (checkedId == R.id.rb_normal) {
                if (!mRbFisheye.isEnabled() || !mRbPerspective.isEnabled()) {
                    InstaCameraManager.getInstance().closePreviewStream();
                    if (mCurrentResolution == null) {
                        InstaCameraManager.getInstance().startPreviewStream();
                    } else {
                        InstaCameraManager.getInstance().startPreviewStream(mCurrentResolution);
                    }
                    mRbFisheye.setEnabled(true);
                    mRbPerspective.setEnabled(true);
                } else {
                    // 切换到普通模式
                    // Switch to Normal Mode
                    mCapturePlayerView.switchNormalMode();
                }
            } else if (checkedId == R.id.rb_fisheye) {
                // 切换到鱼眼模式
                // Switch to Fisheye Mode
                mCapturePlayerView.switchFisheyeMode();
            } else if (checkedId == R.id.rb_perspective) {
                // 切换到透视模式
                // Switch to Perspective Mode
                mCapturePlayerView.switchPerspectiveMode();
            }
        });

        mSpinnerResolution = findViewById(R.id.spinner_resolution);
        ArrayAdapter<PreviewStreamResolution> adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        adapter1.addAll(InstaCameraManager.getInstance().getSupportedPreviewStreamResolution(InstaCameraManager.PREVIEW_TYPE_NORMAL));
        mSpinnerResolution.setAdapter(adapter1);
        mSpinnerResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentResolution = adapter1.getItem(position);
                InstaCameraManager.getInstance().closePreviewStream();
                InstaCameraManager.getInstance().startPreviewStream(mCurrentResolution);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mSpinnerStabType = findViewById(R.id.spinner_stab_type);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        adapter2.add(getString(R.string.stab_type_auto));
        adapter2.add(getString(R.string.stab_type_panorama));
        adapter2.add(getString(R.string.stab_type_calibrate_horizon));
        adapter2.add(getString(R.string.stab_type_footage_motion_smooth));
        adapter2.add(getString(R.string.stab_type_off));
        mSpinnerStabType.setAdapter(adapter2);
        mSpinnerStabType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 4 && mCapturePlayerView.isStabEnabled()
                        || position != 4 && !mCapturePlayerView.isStabEnabled()) {
                    InstaCameraManager.getInstance().closePreviewStream();
                    if (mCurrentResolution == null) {
                        InstaCameraManager.getInstance().startPreviewStream();
                    } else {
                        InstaCameraManager.getInstance().startPreviewStream(mCurrentResolution);
                    }
                } else {
                    mCapturePlayerView.setStabType(getStabType());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        boolean isNanoS = TextUtils.equals(InstaCameraManager.getInstance().getCameraType(), CameraType.NANOS.type);
        mSpinnerStabType.setVisibility(isNanoS ? View.GONE : View.VISIBLE);
    }

    private int getStabType() {
        switch (mSpinnerStabType.getSelectedItemPosition()) {
            case 0:
            default:
                return InstaStabType.STAB_TYPE_AUTO;
            case 1:
                return InstaStabType.STAB_TYPE_PANORAMA;
            case 2:
                return InstaStabType.STAB_TYPE_CALIBRATE_HORIZON;
            case 3:
                return InstaStabType.STAB_TYPE_FOOTAGE_MOTION_SMOOTH;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            // 退出页面时需要关闭预览
            // Auto close preview after page loses focus
            InstaCameraManager.getInstance().setPreviewStatusChangedListener(null);
            InstaCameraManager.getInstance().closePreviewStream();
            mCapturePlayerView.destroy();
        }
    }

    @Override
    public void onOpening() {
        // 预览开启中
        // Preview Opening
        mBtnSwitch.setChecked(true);
    }

    @Override
    public void onOpened() {
        // 预览开启成功，可以播放预览流
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

            @Override
            public void onLoadingStatusChanged(boolean isLoading) {
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
                .setStabType(getStabType())
                .setResolutionParams(3840, 1920, 10)
                .setStabEnabled(mSpinnerStabType.getSelectedItemPosition() != 4);
        if (mCurrentResolution != null) {
            builder.setResolutionParams(3840, 1920, 10);
        }
        if (mRbPlane.isChecked()) {
            // 平铺模式
            // Plane Mode
            builder.setRenderModelType(CaptureParamsBuilder.RENDER_MODE_PLANE_STITCH)
                    .setScreenRatio(2, 1)
                    .setResolutionParams(3840, 1920, 10);
        } else {
            // 普通模式
            // Normal Mode
            builder.setRenderModelType(CaptureParamsBuilder.RENDER_MODE_AUTO);
        }
        return builder;
    }

    @Override
    public void onIdle() {
        // 预览已停止
        // Preview Stopped
        mCapturePlayerView.destroy();
        mCapturePlayerView.setKeepScreenOn(false);
    }

    @Override
    public void onError() {
        // 预览开启失败
        // Preview Failed
        mBtnSwitch.setChecked(false);
    }

    public void onVideoData(byte[] videoData) {
        // Callback frequency 500Hz
        // videoData.timestamp: The time since the camera was turned on
        // videoData.data: Preview raw stream data every frame
        // videoData.size: videoData.data.length
        Log.d("Debug", "##### onVideoData Length: " + videoData.length);
    }
}
