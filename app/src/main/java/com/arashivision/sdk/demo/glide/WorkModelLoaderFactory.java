package com.arashivision.sdk.demo.glide;

import android.content.Context;

import com.arashivision.sdkmedia.work.WorkWrapper;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import java.io.InputStream;

import androidx.annotation.NonNull;

public class WorkModelLoaderFactory implements ModelLoaderFactory<WorkWrapper, InputStream> {

    private Context mContext;

    WorkModelLoaderFactory(Context context) {
        mContext = context;
    }

    @NonNull
    @Override
    public ModelLoader<WorkWrapper, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
        return new WorkModelLoader(mContext);
    }

    @Override
    public void teardown() {

    }

}
