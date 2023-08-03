package com.arashivision.sdk.demo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.arashivision.sdk.demo.R;
import com.arashivision.sdk.demo.util.CameraBindNetworkManager;
import com.arashivision.sdk.demo.util.NetworkManager;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

public class MainActivity extends BaseObserveCameraActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.main_toolbar_title);

        checkStoragePermission();
        if (InstaCameraManager.getInstance().getCameraConnectedType() != InstaCameraManager.CONNECT_TYPE_NONE) {
            onCameraStatusChanged(true);
        }

        findViewById(R.id.btn_full_demo).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, FullDemoActivity.class));
        });

        findViewById(R.id.btn_connect_by_wifi).setOnClickListener(v -> {
            CameraBindNetworkManager.getInstance().bindNetwork(errorCode -> {
                InstaCameraManager.getInstance().openCamera(InstaCameraManager.CONNECT_TYPE_WIFI);
            });
        });

        findViewById(R.id.btn_connect_by_usb).setOnClickListener(v -> {
            InstaCameraManager.getInstance().openCamera(InstaCameraManager.CONNECT_TYPE_USB);
        });

        findViewById(R.id.btn_connect_by_ble).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, BleActivity.class));
        });

        findViewById(R.id.btn_close_camera).setOnClickListener(v -> {
            CameraBindNetworkManager.getInstance().unbindNetwork();
            InstaCameraManager.getInstance().closeCamera();
        });

        findViewById(R.id.btn_capture).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CaptureActivity.class));
        });

        findViewById(R.id.btn_preview).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, PreviewActivity.class));
        });

        findViewById(R.id.btn_preview2).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, Preview2Activity.class));
        });

        findViewById(R.id.btn_preview3).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, Preview3Activity.class));
        });

        findViewById(R.id.btn_live).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LiveActivity.class));
        });

        findViewById(R.id.btn_osc).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, OscActivity.class));
        });

        findViewById(R.id.btn_list_camera_file).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CameraFilesActivity.class));
        });

        findViewById(R.id.btn_settings).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MoreSettingActivity.class));
        });

        findViewById(R.id.btn_play).setOnClickListener(v -> {
            // HDR
//            PlayAndExportActivity.launchActivity(this, StitchActivity.HDR_URLS);
            // PureShot
            PlayAndExportActivity.launchActivity(this, StitchActivity.PURE_SHOT_URLS);
        });

        findViewById(R.id.btn_stitch).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, StitchActivity.class));
        });

        findViewById(R.id.btn_firmware_upgrade).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, FwUpgradeActivity.class));
        });
    }

    private void checkStoragePermission() {
        AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.STORAGE)
                .onDenied(permissions -> {
                    if (AndPermission.hasAlwaysDeniedPermission(this, permissions)) {
                        AndPermission.with(this)
                                .runtime()
                                .setting()
                                .start(1000);
                    }
                    finish();
                })
                .start();
    }

    @Override
    public void onCameraStatusChanged(boolean enabled) {
        super.onCameraStatusChanged(enabled);
        findViewById(R.id.btn_capture).setEnabled(enabled);
        findViewById(R.id.btn_preview).setEnabled(enabled);
        findViewById(R.id.btn_preview2).setEnabled(enabled);
        findViewById(R.id.btn_preview3).setEnabled(enabled);
        findViewById(R.id.btn_live).setEnabled(enabled);
        findViewById(R.id.btn_osc).setEnabled(enabled);
        findViewById(R.id.btn_list_camera_file).setEnabled(enabled);
        findViewById(R.id.btn_settings).setEnabled(enabled);
        findViewById(R.id.btn_firmware_upgrade).setEnabled(enabled);
        if (enabled) {
            Toast.makeText(this, R.string.main_toast_camera_connected, Toast.LENGTH_SHORT).show();
        } else {
            CameraBindNetworkManager.getInstance().unbindNetwork();
            NetworkManager.getInstance().clearBindProcess();
            Toast.makeText(this, R.string.main_toast_camera_disconnected, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCameraConnectError(int errorCode) {
        super.onCameraConnectError(errorCode);
        CameraBindNetworkManager.getInstance().unbindNetwork();
        Toast.makeText(this, getResources().getString(R.string.main_toast_camera_connect_error, errorCode), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCameraSDCardStateChanged(boolean enabled) {
        super.onCameraSDCardStateChanged(enabled);
        if (enabled) {
            Toast.makeText(this, R.string.main_toast_sd_enabled, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.main_toast_sd_disabled, Toast.LENGTH_SHORT).show();
        }
    }

}
