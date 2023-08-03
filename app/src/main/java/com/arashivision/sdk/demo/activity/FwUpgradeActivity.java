package com.arashivision.sdk.demo.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.arashivision.sdk.demo.R;
import com.arashivision.sdkcamera.upgrade.FwUpgradeListener;
import com.arashivision.sdkcamera.upgrade.FwUpgradeManager;

import java.io.File;
import java.util.List;
import java.util.Locale;

import androidx.annotation.Nullable;
import me.rosuh.filepicker.config.FilePickerManager;

public class FwUpgradeActivity extends BaseObserveCameraActivity implements FwUpgradeListener {

    private TextView mTvFwFilePath;
    private ToggleButton mBtnUpgrade;
    private TextView mTvState;

    private File mFwFile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fw_upgrade);
        setTitle(R.string.fw_toolbar_title);
        bindViews();
        initViews();
    }

    private void bindViews() {
        mTvFwFilePath = findViewById(R.id.tv_file_path);
        mBtnUpgrade = findViewById(R.id.btn_upgrade);
        mTvState = findViewById(R.id.tv_state);
    }

    private void initViews() {
        mTvFwFilePath.setText(getString(R.string.fw_file_path, ""));

        mBtnUpgrade.setChecked(false);
        mBtnUpgrade.setOnClickListener(v -> {
            if (mBtnUpgrade.isChecked()) {
                if (mFwFile == null || !mFwFile.exists()) {
                    mBtnUpgrade.setChecked(false);
                    mTvFwFilePath.setText(getString(R.string.fw_file_path, ""));
                    Toast.makeText(this, R.string.fw_toast_choose_file, Toast.LENGTH_SHORT).show();
                } else {
                    FwUpgradeManager.getInstance().startUpgrade(mFwFile.getAbsolutePath(), this);
                }
            } else {
                FwUpgradeManager.getInstance().cancelUpgrade();
            }
        });

        findViewById(R.id.btn_choose_file).setOnClickListener(v -> {
            FilePickerManager.INSTANCE
                    .from(this)
                    .enableSingleChoice()
                    .forResult(FilePickerManager.REQUEST_CODE);
        });
    }

    @Override
    public void onUpgradeSuccess() {
        mTvState.setText(R.string.fw_upgrade_success);
        mBtnUpgrade.setChecked(false);
    }

    @Override
    public void onUpgradeFail(int errorCode, String message) {
        mTvState.setText(getString(R.string.fw_upgrade_fail, errorCode, message));
        mBtnUpgrade.setChecked(false);
    }

    @Override
    public void onUpgradeCancel() {
        mTvState.setText(R.string.fw_upgrade_cancel);
        mBtnUpgrade.setChecked(false);
    }

    @Override
    public void onUpgradeProgress(double progress) {
        mTvState.setText(getString(R.string.fw_upgrade_progress, String.format(Locale.getDefault(), "%.2f%%", (float) (progress * 100))));
    }

    @Override
    public void onBackPressed() {
        if (!FwUpgradeManager.getInstance().isUpgrading()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FilePickerManager.REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                List<String> list = FilePickerManager.INSTANCE.obtainData();
                if (!list.isEmpty()) {
                    mFwFile = new File(list.get(0));
                    mTvFwFilePath.setText(getString(R.string.fw_file_path, mFwFile.getAbsolutePath()));
                }
            }
        }
    }
}
