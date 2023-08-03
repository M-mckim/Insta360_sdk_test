package com.arashivision.sdk.demo.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.arashivision.sdk.demo.R;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.arashivision.sdkcamera.camera.callback.ICameraOperateCallback;
import com.xw.repo.BubbleSeekBar;

import androidx.annotation.Nullable;

public class MoreSettingActivity extends BaseObserveCameraActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more_setting);
        setTitle(R.string.setting_toolbar_title);
        initFunctionModeGroup();
        resetCameraEV();
        resetCameraExposureMode();
        resetCameraISO();
        resetCameraISOTopLimit();
        resetCameraShutterMode();
        resetCameraShutterSpeed();
        resetCameraWhiteBalance();
        initCameraBeepSwitch();
        initCalibrateGyro();
        initFormatStorage();
    }

    private void initFunctionModeGroup() {
        RadioGroup rgFunctionMode = findViewById(R.id.rg_function_mode);
        rgFunctionMode.setOnCheckedChangeListener((group, checkedId) -> {
            resetCameraEV();
            resetCameraExposureMode();
            resetCameraISO();
            resetCameraISOTopLimit();
            resetCameraShutterMode();
            resetCameraShutterSpeed();
            resetCameraWhiteBalance();
        });
    }

    private void resetCameraEV() {
        TextView tvEV = findViewById(R.id.tv_ev_value);
        tvEV.setText(String.valueOf(InstaCameraManager.getInstance().getExposureEV(getCurrentFuncMode())));

        BubbleSeekBar sbEv = findViewById(R.id.sb_ev);
        sbEv.setProgress(InstaCameraManager.getInstance().getExposureEV(getCurrentFuncMode()));

        findViewById(R.id.btn_set_ev).setOnClickListener(v -> {
            InstaCameraManager.getInstance().setExposureEV(getCurrentFuncMode(), sbEv.getProgressFloat());
            tvEV.setText(String.valueOf(InstaCameraManager.getInstance().getExposureEV(getCurrentFuncMode())));
        });
    }

    private void resetCameraExposureMode() {
        RadioGroup rgExposureMode = findViewById(R.id.rg_exposure_mode);
        int exposureMode = InstaCameraManager.getInstance().getExposureMode(getCurrentFuncMode());
        switch (exposureMode) {
            case InstaCameraManager.EXPOSURE_MODE_AUTO:
                rgExposureMode.check(R.id.rb_exposure_auto);
                break;
            case InstaCameraManager.EXPOSURE_MODE_ISO_FIRST:
                rgExposureMode.check(R.id.rb_exposure_iso_first);
                break;
            case InstaCameraManager.EXPOSURE_MODE_SHUTTER_FIRST:
                rgExposureMode.check(R.id.rb_exposure_shutter_first);
                break;
            case InstaCameraManager.EXPOSURE_MODE_MANUAL:
                rgExposureMode.check(R.id.rb_exposure_manual);
                break;
            default:
                rgExposureMode.clearCheck();
        }

        findViewById(R.id.btn_set_exposure_mode).setOnClickListener(v -> {
            switch (rgExposureMode.getCheckedRadioButtonId()) {
                case R.id.rb_exposure_auto:
                    InstaCameraManager.getInstance().setExposureMode(getCurrentFuncMode(), InstaCameraManager.EXPOSURE_MODE_AUTO);
                    break;
                case R.id.rb_exposure_iso_first:
                    InstaCameraManager.getInstance().setExposureMode(getCurrentFuncMode(), InstaCameraManager.EXPOSURE_MODE_ISO_FIRST);
                    break;
                case R.id.rb_exposure_shutter_first:
                    InstaCameraManager.getInstance().setExposureMode(getCurrentFuncMode(), InstaCameraManager.EXPOSURE_MODE_SHUTTER_FIRST);
                    break;
                case R.id.rb_exposure_manual:
                    InstaCameraManager.getInstance().setExposureMode(getCurrentFuncMode(), InstaCameraManager.EXPOSURE_MODE_MANUAL);
                    break;
            }
        });
    }

    private void resetCameraISO() {
        EditText etIso = findViewById(R.id.et_iso);
        etIso.setText(String.valueOf(InstaCameraManager.getInstance().getISO(getCurrentFuncMode())));

        findViewById(R.id.btn_set_iso).setOnClickListener(v -> {
            try {
                int iso = Integer.parseInt(etIso.getText().toString());
                InstaCameraManager.getInstance().setISO(getCurrentFuncMode(), iso);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.osc_dialog_title_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetCameraISOTopLimit() {
        EditText etIsoTopLimit = findViewById(R.id.et_iso_top_limit);
        etIsoTopLimit.setText(String.valueOf(InstaCameraManager.getInstance().getISOTopLimit(getCurrentFuncMode())));

        findViewById(R.id.btn_set_iso_top_limit).setOnClickListener(v -> {
            try {
                int isoTopLimit = Integer.parseInt(etIsoTopLimit.getText().toString());
                InstaCameraManager.getInstance().setISOTopLimit(getCurrentFuncMode(), isoTopLimit);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.osc_dialog_title_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetCameraShutterMode() {
        RadioGroup rgShutterMode = findViewById(R.id.rg_shutter_mode);
        int shutterMode = InstaCameraManager.getInstance().getShutterMode(getCurrentFuncMode());
        switch (shutterMode) {
            case InstaCameraManager.SHUTTER_MODE_OFF:
                rgShutterMode.check(R.id.rb_shutter_mode_off);
                break;
            case InstaCameraManager.SHUTTER_MODE_SPORT:
                rgShutterMode.check(R.id.rb_shutter_mode_sport);
                break;
            case InstaCameraManager.SHUTTER_MODE_FASTER:
                rgShutterMode.check(R.id.rb_shutter_mode_faster);
                break;
            default:
                rgShutterMode.clearCheck();
        }

        findViewById(R.id.btn_set_shutter_mode).setOnClickListener(v -> {
            switch (rgShutterMode.getCheckedRadioButtonId()) {
                case R.id.rb_shutter_mode_off:
                    InstaCameraManager.getInstance().setShutterMode(getCurrentFuncMode(), InstaCameraManager.SHUTTER_MODE_OFF);
                    break;
                case R.id.rb_shutter_mode_sport:
                    InstaCameraManager.getInstance().setShutterMode(getCurrentFuncMode(), InstaCameraManager.SHUTTER_MODE_SPORT);
                    break;
                case R.id.rb_shutter_mode_faster:
                    InstaCameraManager.getInstance().setShutterMode(getCurrentFuncMode(), InstaCameraManager.SHUTTER_MODE_FASTER);
                    break;
            }
        });
    }

    private void resetCameraShutterSpeed() {
        EditText etShutterSpeed = findViewById(R.id.et_shutter_speed);
        etShutterSpeed.setText(String.valueOf(InstaCameraManager.getInstance().getShutterSpeed(getCurrentFuncMode())));

        findViewById(R.id.btn_set_shutter_speed).setOnClickListener(v -> {
            try {
                double shutter = Double.parseDouble(etShutterSpeed.getText().toString());
                InstaCameraManager.getInstance().setShutterSpeed(getCurrentFuncMode(), shutter);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.osc_dialog_title_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetCameraWhiteBalance() {
        RadioGroup rgWb = findViewById(R.id.rg_wb);
        int wb = InstaCameraManager.getInstance().getWhiteBalance(getCurrentFuncMode());
        switch (wb) {
            case InstaCameraManager.WHITE_BALANCE_AUTO:
                rgWb.check(R.id.rb_wb_auto);
                break;
            case InstaCameraManager.WHITE_BALANCE_2700K:
                rgWb.check(R.id.rb_wb_2700k);
                break;
            case InstaCameraManager.WHITE_BALANCE_4000K:
                rgWb.check(R.id.rb_wb_4000k);
                break;
            case InstaCameraManager.WHITE_BALANCE_5000K:
                rgWb.check(R.id.rb_wb_5000k);
                break;
            case InstaCameraManager.WHITE_BALANCE_6500K:
                rgWb.check(R.id.rb_wb_6500k);
                break;
            case InstaCameraManager.WHITE_BALANCE_7500K:
                rgWb.check(R.id.rb_wb_7500k);
                break;
            default:
                rgWb.clearCheck();
        }

        findViewById(R.id.btn_set_wb).setOnClickListener(v -> {
            switch (rgWb.getCheckedRadioButtonId()) {
                case R.id.rb_wb_auto:
                    InstaCameraManager.getInstance().setWhiteBalance(getCurrentFuncMode(), InstaCameraManager.WHITE_BALANCE_AUTO);
                    break;
                case R.id.rb_wb_2700k:
                    InstaCameraManager.getInstance().setWhiteBalance(getCurrentFuncMode(), InstaCameraManager.WHITE_BALANCE_2700K);
                    break;
                case R.id.rb_wb_4000k:
                    InstaCameraManager.getInstance().setWhiteBalance(getCurrentFuncMode(), InstaCameraManager.WHITE_BALANCE_4000K);
                    break;
                case R.id.rb_wb_5000k:
                    InstaCameraManager.getInstance().setWhiteBalance(getCurrentFuncMode(), InstaCameraManager.WHITE_BALANCE_5000K);
                    break;
                case R.id.rb_wb_6500k:
                    InstaCameraManager.getInstance().setWhiteBalance(getCurrentFuncMode(), InstaCameraManager.WHITE_BALANCE_6500K);
                    break;
                case R.id.rb_wb_7500k:
                    InstaCameraManager.getInstance().setWhiteBalance(getCurrentFuncMode(), InstaCameraManager.WHITE_BALANCE_7500K);
                    break;
            }
        });
    }

    private int getCurrentFuncMode() {
        RadioGroup rgFunctionMode = findViewById(R.id.rg_function_mode);
        switch (rgFunctionMode.getCheckedRadioButtonId()) {
            case R.id.rb_capture_normal:
            default:
                return InstaCameraManager.FUNCTION_MODE_CAPTURE_NORMAL;
            case R.id.rb_hdr_capturel:
                return InstaCameraManager.FUNCTION_MODE_HDR_CAPTURE;
            case R.id.rb_interval_shooting:
                return InstaCameraManager.FUNCTION_MODE_INTERVAL_SHOOTING;
            case R.id.rb_night_scene:
                return InstaCameraManager.FUNCTION_MODE_NIGHT_SCENE;
            case R.id.rb_burst:
                return InstaCameraManager.FUNCTION_MODE_BURST;
            case R.id.rb_record_normal:
                return InstaCameraManager.FUNCTION_MODE_RECORD_NORMAL;
            case R.id.rb_hdr_record:
                return InstaCameraManager.FUNCTION_MODE_HDR_RECORD;
            case R.id.rb_bullet_time:
                return InstaCameraManager.FUNCTION_MODE_BULLETTIME;
            case R.id.rb_timelapse:
                return InstaCameraManager.FUNCTION_MODE_TIMELAPSE;
            case R.id.rb_timeshift:
                return InstaCameraManager.FUNCTION_MODE_TIME_SHIFT;
        }
    }

    private void initCameraBeepSwitch() {
        Switch switchCameraBeep = findViewById(R.id.switch_camera_beep);
        switchCameraBeep.setChecked(InstaCameraManager.getInstance().isCameraBeep());
        switchCameraBeep.setOnCheckedChangeListener((buttonView, isChecked) -> {
            InstaCameraManager.getInstance().setCameraBeepSwitch(isChecked);
        });
    }

    private void initCalibrateGyro() {
        findViewById(R.id.btn_calibrate_gyro).setOnClickListener(v -> {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .content(R.string.setting_dialog_msg_gyro_calirate_prompt)
                    .negativeText(R.string.setting_dialog_cancel)
                    .positiveText(R.string.setting_dialog_start)
                    .show();

            dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(v1 -> {
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setContent(R.string.setting_dialog_msg_gyro_calirating);
                dialog.getActionButton(DialogAction.NEGATIVE).setVisibility(View.GONE);
                dialog.getActionButton(DialogAction.POSITIVE).setVisibility(View.GONE);

                InstaCameraManager.getInstance().calibrateGyro(new ICameraOperateCallback() {
                    @Override
                    public void onSuccessful() {
                        updateDialog(getString(R.string.setting_dialog_msg_gyro_calirate_success));
                    }

                    @Override
                    public void onFailed() {
                        updateDialog(getString(R.string.setting_dialog_msg_gyro_calirate_failed));
                    }

                    @Override
                    public void onCameraConnectError() {
                        updateDialog(getString(R.string.setting_dialog_msg_camera_connect_error));
                    }

                    private void updateDialog(String content) {
                        dialog.setContent(content);
                        dialog.getActionButton(DialogAction.POSITIVE).setText(R.string.setting_dialog_ok);
                        dialog.getActionButton(DialogAction.POSITIVE).setVisibility(View.VISIBLE);
                        dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(v2 -> {
                            dialog.dismiss();
                        });
                    }
                });
            });
        });
    }

    private void initFormatStorage() {
        findViewById(R.id.btn_format_storage).setOnClickListener(v -> {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .content(R.string.setting_dialog_msg_format_prompt)
                    .negativeText(R.string.setting_dialog_cancel)
                    .positiveText(R.string.setting_dialog_sure)
                    .show();
            dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(v1 -> {
                dialog.setCancelable(false);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setContent(R.string.setting_dialog_msg_formatting);
                dialog.getActionButton(DialogAction.NEGATIVE).setVisibility(View.GONE);
                dialog.getActionButton(DialogAction.POSITIVE).setVisibility(View.GONE);

                InstaCameraManager.getInstance().formatStorage(new ICameraOperateCallback() {
                    @Override
                    public void onSuccessful() {
                        updateDialog(getString(R.string.setting_dialog_msg_format_success));
                    }

                    @Override
                    public void onFailed() {
                        updateDialog(getString(R.string.setting_dialog_msg_format_failed));
                    }

                    @Override
                    public void onCameraConnectError() {
                        updateDialog(getString(R.string.setting_dialog_msg_camera_connect_error));
                    }

                    private void updateDialog(String content) {
                        dialog.setContent(content);
                        dialog.getActionButton(DialogAction.POSITIVE).setVisibility(View.VISIBLE);
                        dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener(v2 -> {
                            dialog.dismiss();
                        });
                    }
                });
            });
        });
    }

    @Override
    public void onCameraStatusChanged(boolean enabled) {
        super.onCameraStatusChanged(enabled);
        if (!enabled) {
            finish();
        }
    }
}
