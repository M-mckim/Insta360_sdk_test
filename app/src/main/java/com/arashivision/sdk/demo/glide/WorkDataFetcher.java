package com.arashivision.sdk.demo.glide;

import android.content.Context;

import com.arashivision.sdkmedia.export.ExportImageParamsBuilder;
import com.arashivision.sdkmedia.export.ExportUtils;
import com.arashivision.sdkmedia.export.IExportCallback;
import com.arashivision.sdkmedia.work.WorkWrapper;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.annotation.NonNull;

public class WorkDataFetcher implements DataFetcher<InputStream> {

    private Context mContext;
    private WorkWrapper mWorkWrapper;
    private int mExportId = -1;
    private FileInputStream mFileInputStream;

    public WorkDataFetcher(Context context, WorkWrapper workWrapper) {
        mContext = context;
        mWorkWrapper = workWrapper;
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
        String targetPath = mContext.getCacheDir() + "/glide_thumbnail/" + mWorkWrapper.getIdenticalKey();
        IExportCallback exportCallback = new IExportCallback() {
            @Override
            public void onSuccess() {
                try {
                    mFileInputStream = new FileInputStream(targetPath);
                    callback.onDataReady(mFileInputStream);
                } catch (Exception e) {
                    try {
                        callback.onLoadFailed(e);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

            @Override
            public void onFail(int errorCode, String errorMsg) {
                try {
                    callback.onLoadFailed(new Exception("export failed"));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onCancel() {
                try {
                    callback.onLoadFailed(new Exception("export cancel"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        if (mWorkWrapper.isVideo()) {
            ExportImageParamsBuilder builder = new ExportImageParamsBuilder()
                    .setExportMode(ExportUtils.ExportMode.SPHERE)
                    .setTargetPath(targetPath)
                    .setWidth(256)
                    .setHeight(256);
            mExportId = ExportUtils.exportVideoToImage(mWorkWrapper, builder, exportCallback);
        } else {
            ExportImageParamsBuilder builder = new ExportImageParamsBuilder()
                    .setExportMode(ExportUtils.ExportMode.SPHERE)
                    .setTargetPath(targetPath)
                    .setWidth(256)
                    .setHeight(256);
            mExportId = ExportUtils.exportImage(mWorkWrapper, builder, exportCallback);
        }
    }


    @Override
    public void cleanup() {
        try {
            if (mFileInputStream != null) {
                mFileInputStream.close();
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void cancel() {
        if (mExportId >= 0) {
            ExportUtils.stopExport(mExportId);
            mExportId = -1;
        }
    }

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
        return InputStream.class;
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
        return DataSource.LOCAL;
    }

}
