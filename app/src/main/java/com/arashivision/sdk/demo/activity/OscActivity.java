package com.arashivision.sdk.demo.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.arashivision.insta360.basecamera.camera.CameraType;
import com.arashivision.sdk.demo.R;
import com.arashivision.sdk.demo.model.CaptureExposureData;
import com.arashivision.sdk.demo.osc.OscManager;
import com.arashivision.sdk.demo.osc.callback.IOscCallback;
import com.arashivision.sdk.demo.osc.delegate.OscRequestDelegate;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.arashivision.sdkmedia.stitch.StitchUtils;
import com.arashivision.sdkmedia.work.WorkWrapper;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.model.Response;

import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.Nullable;

public class OscActivity extends BaseObserveCameraActivity implements IOscCallback {

    private MaterialDialog mDialog;

    private TextView mTvExposureParams;
    private TextView mTvFrontPath;
    private TextView mTvRearPath;
    private TextView mTvStitchPath;

    private AtomicBoolean mBtnCaptureFrontSensorClicked = new AtomicBoolean(false);
    private AtomicBoolean mBtnCaptureRearSensorClicked = new AtomicBoolean(false);
    private String mFrontSensorCapturePath = null;
    private String mRearSensorCapturePath = null;
    private CaptureExposureData mCaptureExposureData = null;

    private StitchTask mStitchTask;
    private String mStitchOutputPath;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_osc);
        setTitle(R.string.osc_toolbar_title);

        mTvExposureParams = findViewById(R.id.tv_exposure_params);
        mTvFrontPath = findViewById(R.id.tv_fornt_path);
        mTvRearPath = findViewById(R.id.tv_rear_path);
        mTvStitchPath = findViewById(R.id.tv_stitch_path);

        mDialog = new MaterialDialog.Builder(this)
                .title("")
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .positiveText(android.R.string.ok)
                .build();

        // 配置网络请求代理
        // Setting up a network request proxy
        OscManager.getInstance().setOscRequestDelegate(new OscRequestDelegate());

        // 查看相机基本信息与支持的osc命令
        // Get the basic information about the camera and functionality it supports
        findViewById(R.id.btn_info).setOnClickListener(v -> {
            // 先检查相机是否已连接
            // First determine if the camera is connected
            if (isCameraConnected()) {
                OscManager.getInstance().customRequest("/osc/info", null, this);
            } else {
                promptToConnectCamera();
            }
        });

        // 获取相机属性状态
        // Get the attributes of the camera
        findViewById(R.id.btn_state).setOnClickListener(v -> {
            if (isCameraConnected()) {
                OscManager.getInstance().customRequest("/osc/state", "", this);
            } else {
                promptToConnectCamera();
            }
        });

        // 获取相机支持的Options参数
        // Get camera supported options
        findViewById(R.id.btn_get_support_options).setOnClickListener(v -> {
            if (isCameraConnected()) {
                String content = "{\"name\":\"camera.getOptions\"," +
                        "  \"parameters\": {\n" +
                        "      \"optionNames\": [\n" +
                        "          \"_batteryCapacity\",\n" +
                        "          \"remainingSpace\",\n" +
                        "          \"totalSpace\",\n" +
                        "          \"captureModeSupport\",\n" +
                        "          \"captureIntervalSupport\",\n" +
                        "          \"exposureProgramSupport\",\n" +
                        "          \"isoSupport\",\n" +
                        "          \"shutterSpeedSupport\",\n" +
                        "          \"whiteBalanceSupport\",\n" +
                        "          \"hdrSupport\",\n" +
                        "          \"exposureBracketSupport\"\n" +
                        "      ]\n" +
                        "  }\n" +
                        "}}";
                OscManager.getInstance().customRequest("/osc/commands/execute", content, this);
            } else {
                promptToConnectCamera();
            }
        });

        // 拍照，此处为HDR拍照
        // Take Picture. Here is HDR Capture.
        findViewById(R.id.btn_take_picture).setOnClickListener(v -> {
            if (isCameraConnected()) {
                String options = "\"captureMode\":\"image\"," +
                        "\"hdr\":\"hdr\"," +
                        "\"exposureBracket\":{" +
                        "   \"shots\":3," +
                        "   \"increment\":2" +
                        "}";
                OscManager.getInstance().takePicture(options, this);
            } else {
                promptToConnectCamera();
            }
        });

        // 拍照（机内拼接），此处为HDR拍照
        // Take Picture (Device Stitching). Here is HDR Capture.
        findViewById(R.id.btn_take_picture_device_stitching).setOnClickListener(v -> {
            if (isCameraConnected()) {
                String options = "\"captureMode\":\"image\"," +
                        "\"hdr\": \"hdr\"," +
                        "\"photoStitching\": \"ondevice\"";
                OscManager.getInstance().takePicture(options, this);
            } else {
                promptToConnectCamera();
            }
        });

        // 开始录像
        // Start Record
        findViewById(R.id.btn_start_record).setOnClickListener(v -> {
            if (isCameraConnected()) {
                String options = "\"captureMode\":\"video\"";
                OscManager.getInstance().startRecord(options, this);
            } else {
                promptToConnectCamera();
            }
        });

        // 停止录像
        // Stop Record
        findViewById(R.id.btn_stop_record).setOnClickListener(v -> {
            if (isCameraConnected()) {
                OscManager.getInstance().stopRecord(this);
            } else {
                promptToConnectCamera();
            }
        });

        if (Arrays.asList(CameraType.ONEX2, CameraType.X3).contains(CameraType.getForType(InstaCameraManager.getInstance().getCameraType()))) {
            findViewById(R.id.layout_separated_fisheye_stitch).setVisibility(View.VISIBLE);
            findViewById(R.id.btn_get_exposure_params).setOnClickListener(v -> {
                if (isCameraConnected()) {
                    OscManager.getInstance().getCaptureExposureParamsForX2(this);
                } else {
                    promptToConnectCamera();
                }
            });
            findViewById(R.id.btn_switch_front_sensor_capture).setOnClickListener(v -> {
                if (isCameraConnected()) {
                    if (mCaptureExposureData == null) {
                        Toast.makeText(this, R.string.osc_toast_get_exposure_params, Toast.LENGTH_SHORT).show();
                    } else {
                        mBtnCaptureFrontSensorClicked.set(true);
                        OscManager.getInstance().takeSingleSensorPictureForX2(1, mCaptureExposureData, this);
                    }
                } else {
                    promptToConnectCamera();
                }
            });
            findViewById(R.id.btn_switch_rear_sensor_capture).setOnClickListener(v -> {
                if (isCameraConnected()) {
                    if (mCaptureExposureData == null) {
                        Toast.makeText(this, R.string.osc_toast_get_exposure_params, Toast.LENGTH_SHORT).show();
                    } else {
                        mBtnCaptureRearSensorClicked.set(true);
                        OscManager.getInstance().takeSingleSensorPictureForX2(2, mCaptureExposureData, this);
                    }
                } else {
                    promptToConnectCamera();
                }
            });
            findViewById(R.id.btn_separated_fisheye_stitch).setOnClickListener(v -> {
                if (isCameraConnected()) {
                    if (mCaptureExposureData == null) {
                        Toast.makeText(this, R.string.osc_toast_get_exposure_params, Toast.LENGTH_SHORT).show();
                    } else if (TextUtils.isEmpty(mFrontSensorCapturePath) || !new File(mFrontSensorCapturePath).exists()) {
                        Toast.makeText(this, R.string.osc_toast_capture_front_sensor, Toast.LENGTH_SHORT).show();
                    } else if (TextUtils.isEmpty(mRearSensorCapturePath) || !new File(mRearSensorCapturePath).exists()) {
                        Toast.makeText(this, R.string.osc_toast_capture_rear_sensor, Toast.LENGTH_SHORT).show();
                    } else {
                        mStitchTask = new StitchTask(this);
                        mStitchTask.execute();
                    }
                } else {
                    promptToConnectCamera();
                }
            });
        }
    }

    private void updateUI() {
        if (mCaptureExposureData != null) {
            mTvExposureParams.setVisibility(View.VISIBLE);
            mTvExposureParams.setText(getString(R.string.osc_exposure_params, mCaptureExposureData.toString()));
        } else {
            mTvExposureParams.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(mFrontSensorCapturePath)) {
            mTvFrontPath.setVisibility(View.VISIBLE);
            mTvFrontPath.setText(getString(R.string.osc_front_path, mFrontSensorCapturePath));
        } else {
            mTvFrontPath.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(mRearSensorCapturePath)) {
            mTvRearPath.setVisibility(View.VISIBLE);
            mTvRearPath.setText(getString(R.string.osc_rear_path, mRearSensorCapturePath));
        } else {
            mTvRearPath.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(mStitchOutputPath)) {
            mTvStitchPath.setVisibility(View.VISIBLE);
            mTvStitchPath.setText(getString(R.string.osc_stitch_path, mStitchOutputPath));
        } else {
            mTvStitchPath.setVisibility(View.GONE);
        }
    }

    private boolean isCameraConnected() {
        return InstaCameraManager.getInstance().getCameraConnectedType() != InstaCameraManager.CONNECT_TYPE_NONE;
    }

    @Override
    public void onStartRequest() {
        mDialog.setTitle(R.string.osc_dialog_title_send_request);
        mDialog.setContent(R.string.osc_dialog_msg_send_request);
        mDialog.getActionButton(DialogAction.POSITIVE).setVisibility(View.GONE);
        mDialog.show();
    }

    @Override
    public void onSuccessful(Object object) {
        mDialog.setTitle(R.string.osc_dialog_title_success);
        if (object == null) {
            // setOptions()、startRecord() return null
            mDialog.setContent(R.string.osc_dialog_msg_no_data);
            mBtnCaptureFrontSensorClicked.set(false);
            mBtnCaptureRearSensorClicked.set(false);
        } else if (object instanceof CaptureExposureData) {
            mCaptureExposureData = (CaptureExposureData) object;
            mCaptureExposureData.exposureProgram = 1;
            mDialog.setContent(object.toString());
            updateUI();
        } else {
            try {
                // customRequest() will callback the original content returned by OSC
                JSONObject jsonObject = new JSONObject((String) object);
                mDialog.setContent(jsonObject.toString(2));
                mBtnCaptureFrontSensorClicked.set(false);
                mBtnCaptureRearSensorClicked.set(false);
            } catch (Exception e) {
                // takePicture()、stopRecord() will return file address (String[] urls), could be downloaded to local
                StringBuilder message = new StringBuilder();
                if (object.getClass().isArray()) {
                    for (Object obj : (Object[]) object) {
                        message.append(obj).append("\n");
                    }
                    downloadFiles((String[]) object);
                } else {
                    message.append(object.toString());
                    mBtnCaptureFrontSensorClicked.set(false);
                    mBtnCaptureRearSensorClicked.set(false);
                }
                mDialog.setContent(message.toString());
            }
        }
        mDialog.getActionButton(DialogAction.POSITIVE).setVisibility(View.VISIBLE);
    }

    @Override
    public void onError(String message) {
        mDialog.setTitle(R.string.osc_dialog_title_failed);
        mDialog.setContent(message);
        mDialog.getActionButton(DialogAction.POSITIVE).setVisibility(View.VISIBLE);
        mBtnCaptureFrontSensorClicked.set(false);
        mBtnCaptureRearSensorClicked.set(false);
    }

    private void promptToConnectCamera() {
        Toast.makeText(this, R.string.osc_toast_connect_camera, Toast.LENGTH_SHORT).show();
    }

    private void downloadFiles(String[] urls) {
        if (urls == null || urls.length == 0) {
            return;
        }

        String localFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/SDK_DEMO_OSC";
        String[] fileNames = new String[urls.length];
        String[] localPaths = new String[urls.length];
        for (int i = 0; i < localPaths.length; i++) {
            fileNames[i] = urls[i].substring(urls[i].lastIndexOf("/") + 1);
            localPaths[i] = localFolder + "/" + fileNames[i];
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
                                // Demo直接将filePaths传参打开播放页
                                // This demo directly transfers the file paths to the play page for playback
                                if (mBtnCaptureFrontSensorClicked.get()) {
                                    mFrontSensorCapturePath = localPaths[0];
                                    updateUI();
                                } else if (mBtnCaptureRearSensorClicked.get()) {
                                    mRearSensorCapturePath = localPaths[0];
                                    updateUI();
                                } else {
                                    PlayAndExportActivity.launchActivity(OscActivity.this, localPaths);
                                }
                                dialog.dismiss();
                                mBtnCaptureFrontSensorClicked.set(false);
                                mBtnCaptureRearSensorClicked.set(false);
                            }
                        }
                    });
        }
    }

    @Override
    protected void onDestroy() {
        if (mStitchTask != null) {
            mStitchTask.cancel(true);
        }
        super.onDestroy();
    }

    private static class StitchTask extends AsyncTask<Void, Void, Integer> {
        private WeakReference<OscActivity> activityWeakReference;
        private MaterialDialog mStitchDialog;

        private StitchTask(OscActivity activity) {
            super();
            activityWeakReference = new WeakReference<>(activity);
            mStitchDialog = new MaterialDialog.Builder(activity)
                    .content(R.string.export_dialog_msg_stitching)
                    .progress(true, 100)
                    .canceledOnTouchOutside(false)
                    .cancelable(false)
                    .build();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mStitchDialog.show();
        }

        @Override
        protected Integer doInBackground(Void... aVoid) {
            OscActivity oscActivity = activityWeakReference.get();
            if (oscActivity != null && !isCancelled() && oscActivity.mFrontSensorCapturePath != null && oscActivity.mRearSensorCapturePath != null) {
                oscActivity.mStitchOutputPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/SDK_DEMO_OSC/separated_fisheye_" + System.currentTimeMillis() + ".jpg";
                return StitchUtils.stitchSeparatedFisheyeFile(new WorkWrapper(new String[]{oscActivity.mFrontSensorCapturePath, oscActivity.mRearSensorCapturePath}), oscActivity.mStitchOutputPath);
            }
            return -1;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            OscActivity oscActivity = activityWeakReference.get();
            if (oscActivity != null && !isCancelled()) {
                if (result == 0 && oscActivity.mStitchOutputPath != null) {
                    PlayAndExportActivity.launchActivity(oscActivity, new String[]{oscActivity.mStitchOutputPath});
                } else {
                    oscActivity.mStitchOutputPath = null;
                    Toast.makeText(oscActivity, "failed " + result, Toast.LENGTH_SHORT).show();
                }
                oscActivity.updateUI();
            }
            mStitchDialog.dismiss();
        }
    }
}
