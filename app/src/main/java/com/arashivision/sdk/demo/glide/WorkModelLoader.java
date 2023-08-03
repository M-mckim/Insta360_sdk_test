package com.arashivision.sdk.demo.glide;

import android.content.Context;

import com.arashivision.sdkmedia.work.WorkWrapper;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

import java.io.InputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WorkModelLoader implements ModelLoader<WorkWrapper, InputStream> {

    private Context mContext;

    WorkModelLoader(Context context) {
        mContext = context;
    }

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(@NonNull WorkWrapper workWrapper, int width, int height, @NonNull Options options) {
        Key diskCacheKey = new ObjectKey(workWrapper.getIdenticalKey());
        return new LoadData<>(diskCacheKey, new WorkDataFetcher(mContext, workWrapper));
    }

    @Override
    public boolean handles(@NonNull WorkWrapper workWrapper) {
        return true;
    }
}
