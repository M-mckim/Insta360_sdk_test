package com.arashivision.sdk.demo.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.RadioGroup;

import com.afollestad.materialdialogs.MaterialDialog;
import com.arashivision.sdk.demo.MyApp;
import com.arashivision.sdk.demo.R;
import com.arashivision.sdkmedia.player.image.ImageParamsBuilder;
import com.arashivision.sdkmedia.player.image.InstaImagePlayerView;
import com.arashivision.sdkmedia.stitch.StitchUtils;
import com.arashivision.sdkmedia.work.WorkWrapper;

import java.lang.ref.WeakReference;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class StitchActivity extends AppCompatActivity {

    public static final String HDR_COPY_DIR = MyApp.getInstance().getCacheDir() + "/hdr_source";
    public static final String[] HDR_URLS = new String[]{
            HDR_COPY_DIR + "/img1.jpg",
            HDR_COPY_DIR + "/img2.jpg",
            HDR_COPY_DIR + "/img3.jpg"
    };

    public static final String PURE_SHOT_COPY_DIR = MyApp.getInstance().getCacheDir() + "/pure_shot_source";
    public static final String[] PURE_SHOT_URLS = new String[]{
            PURE_SHOT_COPY_DIR + "/pureshot.insp",
            PURE_SHOT_COPY_DIR + "/pureshot.dng"
    };

    private WorkWrapper mWorkWrapper = new WorkWrapper(HDR_URLS);
    private String mOutputPath = MyApp.getInstance().getFilesDir() + "/hdr_generate/generate.jpg";
    private StitchTask mStitchTask;

    private InstaImagePlayerView mImagePlayerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stitch);
        setTitle(R.string.stitch_toolbar_title);
        bindViews();

        // 初始显示无HDR效果的图片
        // Initial display image effect without HDR stitching
        showGenerateResult(false);
    }

    private void bindViews() {
        mImagePlayerView = findViewById(R.id.player_image);
        mImagePlayerView.setLifecycle(getLifecycle());

        RadioGroup rgStitchMode = findViewById(R.id.rg_stitch_mode);
        rgStitchMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_none) {
                showGenerateResult(false);
            } else if (checkedId == R.id.rb_hdr) {
                startGenerate();
            }
        });
    }

    private void startGenerate() {
        mStitchTask = new StitchTask(this);
        mStitchTask.execute();
    }

    private void showGenerateResult(boolean successful) {
        ImageParamsBuilder builder = new ImageParamsBuilder()
                // 如果HDR合成成功，则将其文件路径设置为播放参数
                // If HDR stitching is successful then set it as the playback proxy
                .setUrlForPlay(successful ? mOutputPath : null);
        mImagePlayerView.prepare(mWorkWrapper, builder);
        mImagePlayerView.play();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mStitchTask != null) {
            mStitchTask.cancel(true);
        }
        mImagePlayerView.destroy();
    }

    private static class StitchTask extends AsyncTask<Void, Void, Boolean> {
        private WeakReference<StitchActivity> activityWeakReference;
        private MaterialDialog mDialog;

        private StitchTask(StitchActivity activity) {
            super();
            activityWeakReference = new WeakReference<>(activity);
            mDialog = new MaterialDialog.Builder(activity)
                    .progress(true, 100)
                    .canceledOnTouchOutside(false)
                    .cancelable(false)
                    .build();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            StitchActivity stitchActivity = activityWeakReference.get();
            if (stitchActivity != null && !isCancelled()) {
                // Start HDR stitching
                return StitchUtils.generateHDR(stitchActivity.mWorkWrapper, stitchActivity.mOutputPath);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            StitchActivity stitchActivity = activityWeakReference.get();
            if (stitchActivity != null && !isCancelled()) {
                stitchActivity.showGenerateResult(result);
            }
            mDialog.dismiss();
        }
    }

}
