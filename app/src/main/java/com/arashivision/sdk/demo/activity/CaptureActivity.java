package com.arashivision.sdk.demo.activity;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.arashivision.insta360.basecamera.camera.CameraType;
import com.arashivision.sdk.demo.R;
import com.arashivision.sdk.demo.util.TimeFormat;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.arashivision.sdkcamera.camera.callback.ICameraOperateCallback;
import com.arashivision.sdkcamera.camera.callback.ICaptureStatusListener;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.model.Response;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.Nullable;

public class CaptureActivity extends BaseObserveCameraActivity implements ICaptureStatusListener {

    private final String TAG = "CaptureActivity";

    private TextView mTvCaptureStatus;
    private TextView mTvCaptureTime;
    private TextView mTvCaptureCount;
    private Button mBtnPlayCameraFile;
    private Button mBtnPlayLocalFile;
    private EditText etCaptureDelayTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
        setTitle(R.string.capture_toolbar_title);
        bindViews();

        if (InstaCameraManager.getInstance().getCameraConnectedType() == InstaCameraManager.CONNECT_TYPE_NONE) {
            finish();
            return;
        }

        SwitchSensorCallback switchSensorCallback = new SwitchSensorCallback(this);
        findViewById(R.id.layout_switch_sensor).setVisibility((isOneX2() || isOneX3()) ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_switch_dual_sensor).setOnClickListener(v -> {
            switchSensorCallback.onStart();
            InstaCameraManager.getInstance().switchCameraMode(InstaCameraManager.CAMERA_MODE_PANORAMA, InstaCameraManager.FOCUS_SENSOR_ALL, switchSensorCallback);
        });
        findViewById(R.id.btn_switch_front_sensor).setOnClickListener(v -> {
            switchSensorCallback.onStart();
            InstaCameraManager.getInstance().switchCameraMode(InstaCameraManager.CAMERA_MODE_SINGLE_FRONT, InstaCameraManager.FOCUS_SENSOR_FRONT, switchSensorCallback);
        });
        findViewById(R.id.btn_switch_rear_sensor).setOnClickListener(v -> {
            switchSensorCallback.onStart();
            InstaCameraManager.getInstance().switchCameraMode(InstaCameraManager.CAMERA_MODE_SINGLE_REAR, InstaCameraManager.FOCUS_SENSOR_REAR, switchSensorCallback);
        });

        findViewById(R.id.btn_normal_capture).setOnClickListener(v -> {
            if (checkSdCardEnabled()) {
                int funcMode = InstaCameraManager.FUNCTION_MODE_CAPTURE_NORMAL;
                InstaCameraManager.getInstance().setRawToCamera(funcMode, false);
                InstaCameraManager.getInstance().startNormalCapture(false, getCaptureDelayTime());
            }
        });
        findViewById(R.id.btn_normal_capture_pure_shot).setOnClickListener(v -> {
            if (checkSdCardEnabled()) {
                int funcMode = InstaCameraManager.FUNCTION_MODE_CAPTURE_NORMAL;
                InstaCameraManager.getInstance().setRawToCamera(funcMode, true);
                InstaCameraManager.getInstance().startNormalCapture(true, getCaptureDelayTime());
            }
        });

        findViewById(R.id.btn_normal_pano_capture).setVisibility(isOneX2() ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_normal_pano_capture).setEnabled(supportInstaPanoCapture());
        findViewById(R.id.btn_normal_pano_capture).setOnClickListener(v -> {
            if (checkSdCardEnabled()) {
                InstaCameraManager.getInstance().startNormalPanoCapture(InstaCameraManager.FOCUS_SENSOR_REAR, false, getCaptureDelayTime());
            }
        });

        findViewById(R.id.btn_hdr_capture).setOnClickListener(v -> {
            if (checkSdCardEnabled()) {
                if (!isNanoS()) {
                    int funcMode = InstaCameraManager.FUNCTION_MODE_HDR_CAPTURE;
                    InstaCameraManager.getInstance().setAEBCaptureNum(funcMode, 3);
                    InstaCameraManager.getInstance().setExposureEV(funcMode, 2f);
                }
                InstaCameraManager.getInstance().startHDRCapture(false, getCaptureDelayTime());
            }
        });

        findViewById(R.id.btn_hdr_pano_capture).setVisibility(isOneX2() ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_hdr_pano_capture).setEnabled(supportInstaPanoCapture());
        findViewById(R.id.btn_hdr_pano_capture).setOnClickListener(v -> {
            if (checkSdCardEnabled()) {
                int funcMode = InstaCameraManager.FUNCTION_MODE_HDR_PANO_CAPTURE;
                InstaCameraManager.getInstance().setAEBCaptureNum(funcMode, 3);
                InstaCameraManager.getInstance().setExposureEV(funcMode, 2f);
                InstaCameraManager.getInstance().startHDRPanoCapture(InstaCameraManager.FOCUS_SENSOR_FRONT, false, getCaptureDelayTime());
            }
        });

        findViewById(R.id.btn_interval_shooting_start).setOnClickListener(v -> {
            if (checkSdCardEnabled()) {
                InstaCameraManager.getInstance().setIntervalShootingTime(3000);
                InstaCameraManager.getInstance().startIntervalShooting();
            }
        });

        findViewById(R.id.btn_interval_shooting_stop).setOnClickListener(v -> {
            InstaCameraManager.getInstance().stopIntervalShooting();
        });

        findViewById(R.id.btn_normal_record_start).setOnClickListener(v -> {
            if (checkSdCardEnabled()) {
                InstaCameraManager.getInstance().startNormalRecord();
            }
        });

        findViewById(R.id.btn_normal_record_stop).setOnClickListener(v -> {
            InstaCameraManager.getInstance().stopNormalRecord();
        });

        findViewById(R.id.btn_hdr_record_start).setOnClickListener(v -> {
            if (checkSdCardEnabled()) {
                InstaCameraManager.getInstance().startHDRRecord();
            }
        });

        findViewById(R.id.btn_hdr_record_stop).setOnClickListener(v -> {
            InstaCameraManager.getInstance().stopHDRRecord();
        });

        findViewById(R.id.btn_timelapse_start).setOnClickListener(v -> {
            if (checkSdCardEnabled()) {
                InstaCameraManager.getInstance().setTimeLapseInterval(500);
                InstaCameraManager.getInstance().startTimeLapse();
            }
        });

        findViewById(R.id.btn_timelapse_stop).setOnClickListener(v -> {
            InstaCameraManager.getInstance().stopTimeLapse();
        });

        // Capture Status Callback
        InstaCameraManager.getInstance().setCaptureStatusListener(this);
    }

    private void bindViews() {
        mTvCaptureStatus = findViewById(R.id.tv_capture_status);
        mTvCaptureTime = findViewById(R.id.tv_capture_time);
        mTvCaptureCount = findViewById(R.id.tv_capture_count);
        mBtnPlayCameraFile = findViewById(R.id.btn_play_camera_file);
        mBtnPlayLocalFile = findViewById(R.id.btn_play_local_file);
        etCaptureDelayTime = findViewById(R.id.et_capture_delay_timer);
    }

    private int getCaptureDelayTime() {
        String delayTime = etCaptureDelayTime.getText().toString();
        if (TextUtils.isEmpty(delayTime)) return 0;
        try {
            return Integer.parseInt(delayTime);
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean isNanoS() {
        String cameraType = InstaCameraManager.getInstance().getCameraType();
        return TextUtils.isEmpty(cameraType) || CameraType.getForType(cameraType) == CameraType.NANOS;
    }

    private boolean isOneX2() {
        return CameraType.getForType(InstaCameraManager.getInstance().getCameraType()) == CameraType.ONEX2;
    }

    private boolean isOneX3() {
        return CameraType.getForType(InstaCameraManager.getInstance().getCameraType()) == CameraType.X3;
    }

    private boolean supportInstaPanoCapture() {
        return isOneX2() && InstaCameraManager.getInstance().getCurrentCameraMode() == InstaCameraManager.CAMERA_MODE_PANORAMA;
    }

    private boolean checkSdCardEnabled() {
        if (!InstaCameraManager.getInstance().isSdCardEnabled()) {
            Toast.makeText(this, R.string.capture_toast_sd_card_error, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    @Override
    public void onCameraStatusChanged(boolean enabled) {
        super.onCameraStatusChanged(enabled);
        if (!enabled) {
            finish();
        }
    }

    @Override
    public void onCameraSensorModeChanged(int cameraSensorMode) {
        super.onCameraSensorModeChanged(cameraSensorMode);
        findViewById(R.id.btn_normal_pano_capture).setEnabled(supportInstaPanoCapture());
        findViewById(R.id.btn_hdr_pano_capture).setEnabled(supportInstaPanoCapture());
    }

    @Override
    public void onCaptureStarting() {
        mTvCaptureStatus.setText(R.string.capture_capture_starting);
        mBtnPlayCameraFile.setVisibility(View.GONE);
        mBtnPlayLocalFile.setVisibility(View.GONE);
    }

    @Override
    public void onCaptureWorking() {
        mTvCaptureStatus.setText(R.string.capture_capture_working);
    }

    @Override
    public void onCaptureStopping() {
        mTvCaptureStatus.setText(R.string.capture_capture_stopping);
    }

    @Override
    public void onCaptureFinish(String[] filePaths) {
        Log.i(TAG, "onCaptureFinish, filePaths = " + ((filePaths == null) ? "null" : Arrays.toString(filePaths)));
        mTvCaptureStatus.setText(R.string.capture_capture_finished);
        mTvCaptureTime.setVisibility(View.GONE);
        mTvCaptureCount.setVisibility(View.GONE);
        if (filePaths != null && filePaths.length > 0) {
            mBtnPlayCameraFile.setVisibility(View.VISIBLE);
            mBtnPlayCameraFile.setOnClickListener(v -> {
                PlayAndExportActivity.launchActivity(this, filePaths);
            });
            mBtnPlayLocalFile.setVisibility(View.VISIBLE);
            mBtnPlayLocalFile.setOnClickListener(v -> {
                downloadFilesAndPlay(filePaths);
            });
        } else {
            mBtnPlayCameraFile.setVisibility(View.GONE);
            mBtnPlayCameraFile.setOnClickListener(null);
            mBtnPlayLocalFile.setVisibility(View.GONE);
            mBtnPlayLocalFile.setOnClickListener(null);
        }
    }

    @Override
    public void onCaptureTimeChanged(long captureTime) {
        mTvCaptureTime.setVisibility(View.VISIBLE);
        mTvCaptureTime.setText(getString(R.string.capture_capture_time, TimeFormat.durationFormat(captureTime)));
    }

    @Override
    public void onCaptureCountChanged(int captureCount) {
        mTvCaptureCount.setVisibility(View.VISIBLE);
        mTvCaptureCount.setText(getString(R.string.capture_capture_count, captureCount));
    }

    private void downloadFilesAndPlay(String[] urls) {
        if (urls == null || urls.length == 0) {
            return;
        }

        String localFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/SDK_DEMO_CAPTURE";
        String[] fileNames = new String[urls.length];
        String[] localPaths = new String[urls.length];
        boolean needDownload = false;
        for (int i = 0; i < localPaths.length; i++) {
            fileNames[i] = urls[i].substring(urls[i].lastIndexOf("/") + 1);
            localPaths[i] = localFolder + "/" + fileNames[i];
            if (!new File(localPaths[i]).exists()) {
                needDownload = true;
            }
        }

        if (!needDownload) {
            PlayAndExportActivity.launchActivity(CaptureActivity.this, localPaths);
            return;
        }

        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .title(R.string.osc_dialog_title_downloading)
                .content(getString(R.string.osc_dialog_msg_downloading, urls.length, 0, 0))
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .show();

        AtomicInteger successfulCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        for (int i = 0; i < localPaths.length; i++) {
            String url = urls[i];
            OkGo.<File>get(url)
                    .execute(new FileCallback(localFolder, fileNames[i]) {

                        @Override
                        public void onError(Response<File> response) {
                            super.onError(response);
                            errorCount.incrementAndGet();
                            checkDownloadCount();
                        }

                        @Override
                        public void onSuccess(Response<File> response) {
                            successfulCount.incrementAndGet();
                            checkDownloadCount();
                        }

                        private void checkDownloadCount() {
                            dialog.setContent(getString(R.string.osc_dialog_msg_downloading, urls.length, successfulCount.intValue(), errorCount.intValue()));
                            if (successfulCount.intValue() + errorCount.intValue() >= urls.length) {
                                PlayAndExportActivity.launchActivity(CaptureActivity.this, localPaths);
                                dialog.dismiss();
                            }
                        }
                    });
        }
    }

    private static class SwitchSensorCallback implements ICameraOperateCallback {

        private final Context context;
        private MaterialDialog dialog;

        private SwitchSensorCallback(Context context) {
            this.context = context;
        }

        public void onStart() {
            dialog = new MaterialDialog.Builder(context)
                    .content(R.string.capture_switch_sensor_ing)
                    .progress(true, 100)
                    .cancelable(false)
                    .canceledOnTouchOutside(false)
                    .show();
        }

        @Override
        public void onSuccessful() {
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            Toast.makeText(context, R.string.capture_switch_sensor_success, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed() {
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            Toast.makeText(context, R.string.capture_switch_sensor_failed, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCameraConnectError() {
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            Toast.makeText(context, R.string.capture_switch_sensor_camera_connect_error, Toast.LENGTH_SHORT).show();
        }
    }

    ;
}
