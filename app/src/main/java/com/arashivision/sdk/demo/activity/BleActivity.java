package com.arashivision.sdk.demo.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.arashivision.sdk.demo.R;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.arashivision.sdkcamera.camera.callback.IScanBleListener;
import com.clj.fastble.data.BleDevice;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class BleActivity extends BaseObserveCameraActivity implements IScanBleListener {

    private TextView mTvScanState;
    private RecyclerView mRvBleDevice;
    private BleDeviceAdapter mBleDeviceAdapter;

    private MaterialDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);
        setTitle(R.string.ble_toolbar_title);

        mTvScanState = findViewById(R.id.tv_scan_state);

        mRvBleDevice = findViewById(R.id.rv_ble_device);
        mRvBleDevice.setLayoutManager(new LinearLayoutManager(this));
        mRvBleDevice.setAdapter(mBleDeviceAdapter = new BleDeviceAdapter());
        mBleDeviceAdapter.setOnItemClickListener(bleDevice -> {
            InstaCameraManager.getInstance().stopBleScan();
            InstaCameraManager.getInstance().connectBle(bleDevice);
            showConnectingDialog();
        });

        checkLocaltionPermission();
    }

    private void checkLocaltionPermission() {
        AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.LOCATION)
                .onDenied(permissions -> {
                    if (AndPermission.hasAlwaysDeniedPermission(this, permissions)) {
                        AndPermission.with(this)
                                .runtime()
                                .setting()
                                .start(1000);
                    }
                    finish();
                }).onGranted(data -> {
                    InstaCameraManager.getInstance().setScanBleListener(this);
                    InstaCameraManager.getInstance().startBleScan(30_000);
                }).start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            InstaCameraManager.getInstance().setScanBleListener(null);
            InstaCameraManager.getInstance().stopBleScan();
        }
    }

    @Override
    public void onScanStartSuccess() {
        mTvScanState.setText(R.string.ble_state_start_scan);
    }

    @Override
    public void onScanStartFail() {
        mTvScanState.setText(R.string.ble_state_start_failed);
    }

    @Override
    public void onScanning(BleDevice bleDevice) {
        mTvScanState.setText(R.string.ble_state_scanning);
        mBleDeviceAdapter.addData(bleDevice);
    }

    @Override
    public void onScanFinish(List<BleDevice> bleDeviceList) {
        mTvScanState.setText(R.string.ble_state_scan_finish);
        mBleDeviceAdapter.updateData(bleDeviceList);
    }

    private void showConnectingDialog() {
        mDialog = new MaterialDialog.Builder(this)
                .content(R.string.ble_state_connecting)
                .progress(true, 100)
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .show();
    }

    private void hideConnectingDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    @Override
    public void onCameraStatusChanged(boolean enabled) {
        super.onCameraStatusChanged(enabled);
        if (enabled) {
            hideConnectingDialog();
            finish();
        }
    }

    private static class BleDeviceAdapter extends RecyclerView.Adapter<BleDeviceAdapter.MyHolder> {

        private final List<BleDevice> deviceList = new ArrayList<>();

        private void addData(BleDevice bleDevice) {
            deviceList.add(bleDevice);
            notifyDataSetChanged();
        }

        private void updateData(List<BleDevice> list) {
            deviceList.clear();
            deviceList.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public BleDeviceAdapter.MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recycler_item_ble_device, parent, false);
            return new MyHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyHolder holder, int position) {
            BleDevice bleDevice = deviceList.get(position);
            holder.tvDeviceName.setText(bleDevice.getName() + "  " + bleDevice.getMac());
            holder.itemView.setOnClickListener(v -> {
                if (mItemClickListener != null) {
                    mItemClickListener.onItemClick(bleDevice);
                }
            });
        }


        @Override
        public int getItemCount() {
            return deviceList.size();
        }

        private static class MyHolder extends RecyclerView.ViewHolder {
            TextView tvDeviceName;

            MyHolder(@NonNull View itemView) {
                super(itemView);
                tvDeviceName = itemView.findViewById(R.id.tv_device_name);
            }
        }

        private IItemClickListener mItemClickListener;

        private void setOnItemClickListener(IItemClickListener listener) {
            mItemClickListener = listener;
        }

        private interface IItemClickListener {
            void onItemClick(BleDevice bleDevice);
        }
    }

}
