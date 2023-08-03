package com.arashivision.sdk.demo.activity;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.arashivision.insta360.basecamera.camera.CameraType;
import com.arashivision.sdk.demo.R;
import com.arashivision.sdk.demo.util.NetworkManager;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.arashivision.sdkcamera.camera.callback.ILiveStatusListener;
import com.arashivision.sdkcamera.camera.callback.IPreviewStatusListener;
import com.arashivision.sdkcamera.camera.live.LiveParamsBuilder;
import com.arashivision.sdkcamera.camera.preview.PreviewParamsBuilder;
import com.arashivision.sdkcamera.camera.resolution.PreviewStreamResolution;
import com.arashivision.sdkmedia.player.capture.CaptureParamsBuilder;
import com.arashivision.sdkmedia.player.capture.InstaCapturePlayerView;
import com.arashivision.sdkmedia.player.config.InstaStabType;
import com.arashivision.sdkmedia.player.listener.PlayerViewListener;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

public class LiveActivity extends BaseObserveCameraActivity implements IPreviewStatusListener, ILiveStatusListener {

    private EditText mEtRtmp;
    private EditText mEtWidth;
    private EditText mEtHeight;
    private EditText mEtFps;
    private EditText mEtBitrate;
    private CheckBox mCbPanorama;
    private CheckBox mCbAudioEnabled;
    private ToggleButton mBtnLive;
    private ToggleButton mBtnBindNetwork;
    private TextView mTvLiveStatus;
    private Spinner mSpinnerResolution;
    private Spinner mSpinnerStabType;
    private InstaCapturePlayerView mCapturePlayerView;

    private PreviewStreamResolution mCurrentResolution;

    private ImageReader mImageReader;
    private HandlerThread mImageReaderHandlerThread;
    private Handler mImageReaderHandler;

    private final static String TAG = LiveActivity.class.getName();

    private LiveActivity.WebRTCClient WC;

    private boolean is_OK = false;

    private int SeqNum = 0;

    private byte[] img_bytes;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);
        setTitle(R.string.live_toolbar_title);
        bindViews();
        restoreLiveData();

        // Auto open preview after page gets focus
        List<PreviewStreamResolution> list = InstaCameraManager.getInstance().getSupportedPreviewStreamResolution(InstaCameraManager.PREVIEW_TYPE_LIVE);
        if (!list.isEmpty()) {
            mCurrentResolution = list.get(0);
            InstaCameraManager.getInstance().setPreviewStatusChangedListener(this);
            // mSpinnerResolution的onItemSelected会自动触发，故此处注释掉
            // mSpinnerResolution -> onItemSelected() Will automatically trigger to open the preview, so comment it out here
//            restartPreview();
        }

        WC = new LiveActivity.WebRTCClient();

        WC.createPeerConnection();

        WC.createDataChannel();

        WC.peerConnection1.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                // Set local description
                // Offer or Answer 생성 성공 시 호출됨
                Log.d(TAG, "onCreateSuccess: " + sessionDescription.type);
                // 생성된 SDP 설정을 로컬 SDP로 설정하고 상대방에게 전송하는 작업을 수행
                WC.peerConnection1.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        // peerConnection.setLocalDescription(sdpObserver, sessionDescription);
                        // Local description set successfully
                        // Now you can get the local description
                        Log.d(TAG, "onCreateSuccess-setLocalDescription: " + sessionDescription.type);
                        SessionDescription localDescription = WC.peerConnection1.getLocalDescription();

                        if (localDescription != null) {
                            // Local description is available
                            // You can access it using localDescription.description
                            Log.d(TAG, "Local Description: " + localDescription.description);
                        } else {
                            // Local description is not available
                            Log.d(TAG, "Local Description is null");
                        }
                    }

                    // Implement other SdpObserver methods
                    @Override
                    public void onSetSuccess() {
                        // setLocalDescription 또는 setRemoteDescription 성공 시 호출됨
                        Log.d(TAG, "onSetSuccess-setLocalDescription");
                        // 상대방에게 전달된 SDP 설정이 완료되었을 때 수행할 작업을 처리
                    }

                    @Override
                    public void onCreateFailure(String s) {
                        Log.e(TAG, "onCreateFailure-setLocalDescription");
                    }

                    @Override
                    public void onSetFailure(String s) {
                        Log.e(TAG, "onSetFailure-setLocalDescription");
                    }
                }, sessionDescription);
            }
            // Implement other SdpObserver methods
            @Override
            public void onSetSuccess() {
                // setLocalDescription 또는 setRemoteDescription 성공 시 호출됨
                Log.d(TAG, "onSetSuccess");
                // 상대방에게 전달된 SDP 설정이 완료되었을 때 수행할 작업을 처리
            }
            @Override
            public void onCreateFailure(String s) {
                Log.e(TAG, "onCreateFailure");
            }
            @Override
            public void onSetFailure(String s) {
                Log.e(TAG, "onSetFailure");
            }
        }, new MediaConstraints());

        WC.peerConnection2.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(TAG, "onCreateSuccess: " + sessionDescription.type);
                WC.peerConnection2.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        Log.d(TAG, "onCreateSuccess-setLocalDescription: " + sessionDescription.type);
                        SessionDescription localDescription = WC.peerConnection2.getLocalDescription();
                        if (localDescription != null) {
                            Log.d(TAG, "Local Description: " + localDescription.description);
                        } else {
                            Log.d(TAG, "Local Description is null");
                        }
                    }
                    @Override
                    public void onSetSuccess() {
                        Log.d(TAG, "onSetSuccess-setLocalDescription");
                    }
                    @Override
                    public void onCreateFailure(String s) {
                        Log.e(TAG, "onCreateFailure-setLocalDescription");
                    }
                    @Override
                    public void onSetFailure(String s) {
                        Log.e(TAG, "onSetFailure-setLocalDescription");
                    }
                }, sessionDescription);
            }
            @Override
            public void onSetSuccess() {
                Log.d(TAG, "onSetSuccess");
            }
            @Override
            public void onCreateFailure(String s) {
                Log.e(TAG, "onCreateFailure");
            }
            @Override
            public void onSetFailure(String s) {
                Log.e(TAG, "onSetFailure");
            }
        }, new MediaConstraints());
/*
        WC.peerConnection3.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(TAG, "onCreateSuccess: " + sessionDescription.type);
                WC.peerConnection3.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        Log.d(TAG, "onCreateSuccess-setLocalDescription: " + sessionDescription.type);
                        SessionDescription localDescription = WC.peerConnection3.getLocalDescription();
                        if (localDescription != null) {
                            Log.d(TAG, "Local Description: " + localDescription.description);
                        } else {
                            Log.d(TAG, "Local Description is null");
                        }
                    }
                    @Override
                    public void onSetSuccess() {
                        Log.d(TAG, "onSetSuccess-setLocalDescription");
                    }
                    @Override
                    public void onCreateFailure(String s) {
                        Log.e(TAG, "onCreateFailure-setLocalDescription");
                    }
                    @Override
                    public void onSetFailure(String s) {
                        Log.e(TAG, "onSetFailure-setLocalDescription");
                    }
                }, sessionDescription);
            }
            @Override
            public void onSetSuccess() {
                Log.d(TAG, "onSetSuccess");
            }
            @Override
            public void onCreateFailure(String s) {
                Log.e(TAG, "onCreateFailure");
            }
            @Override
            public void onSetFailure(String s) {
                Log.e(TAG, "onSetFailure");
            }
        }, new MediaConstraints());
 */

        Timer timer = new Timer();

        TimerTask TT = new TimerTask() {
            @Override
            public void run() {
                // 반복실행할 구문
                is_OK = true;
            }
        };

        timer.schedule(TT, 0, 3000); //Timer 실행; 5000: 5000ms network 이슈로 5MB 크기 이미지 보낼때는 5초가 적당함.
        // answer_max-message-size: 65536
    }

    private void bindViews() {
        mCapturePlayerView = findViewById(R.id.player_capture);
        mCapturePlayerView.setLifecycle(getLifecycle());

        mEtRtmp = findViewById(R.id.et_rtmp);
        mEtWidth = findViewById(R.id.et_width);
        mEtHeight = findViewById(R.id.et_height);
        mEtFps = findViewById(R.id.et_fps);
        mEtBitrate = findViewById(R.id.et_bitrate);
        mCbPanorama = findViewById(R.id.cb_panorama);
        mCbAudioEnabled = findViewById(R.id.cb_audio_enabled);
        mBtnLive = findViewById(R.id.btn_live);
        mBtnBindNetwork = findViewById(R.id.btn_bind_network);
        mTvLiveStatus = findViewById(R.id.tv_live_status);

        mCbAudioEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mCurrentResolution != null) {
                restartPreview();
            }
        });

        mBtnLive.setEnabled(false);
        mBtnLive.setOnClickListener(v -> {
            if (mBtnLive.isChecked()) {
                saveLiveData();
                mBtnLive.setChecked(checkToStartLive());
            } else {
                stopLive();
            }
        });
        mBtnLive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mSpinnerResolution.setEnabled(!isChecked);
            mCbPanorama.setEnabled(!isChecked);
            mCbAudioEnabled.setEnabled(!isChecked);
        });

        mBtnBindNetwork.setChecked(NetworkManager.getInstance().isBindingMobileNetwork());
        mBtnBindNetwork.setOnClickListener(v -> {
            if (mBtnBindNetwork.isChecked()) {
                NetworkManager.getInstance().exchangeNetToMobile();
            } else {
                NetworkManager.getInstance().clearBindProcess();
            }
        });

        mSpinnerResolution = findViewById(R.id.spinner_resolution);
        ArrayAdapter<PreviewStreamResolution> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        adapter.addAll(InstaCameraManager.getInstance().getSupportedPreviewStreamResolution(InstaCameraManager.PREVIEW_TYPE_LIVE));
        mSpinnerResolution.setAdapter(adapter);
        mSpinnerResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentResolution = adapter.getItem(position);
                restartPreview();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mSpinnerStabType = findViewById(R.id.spinner_stab_type);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        adapter2.add(getString(R.string.stab_type_auto));
        adapter2.add(getString(R.string.stab_type_panorama));
        adapter2.add(getString(R.string.stab_type_calibrate_horizon));
        adapter2.add(getString(R.string.stab_type_footage_motion_smooth));
        adapter2.add(getString(R.string.stab_type_off));
        mSpinnerStabType.setAdapter(adapter2);
        mSpinnerStabType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 4 && mCapturePlayerView.isStabEnabled()
                        || position != 4 && !mCapturePlayerView.isStabEnabled()) {
                    if (mCurrentResolution != null) {
                        restartPreview();
                    }
                } else {
                    mCapturePlayerView.setStabType(getStabType());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        boolean isNanoS = TextUtils.equals(InstaCameraManager.getInstance().getCameraType(), CameraType.NANOS.type);
        mSpinnerStabType.setVisibility(isNanoS ? View.GONE : View.VISIBLE);

    }

    private int getStabType() {
        switch (mSpinnerStabType.getSelectedItemPosition()) {
            case 0:
            default:
                return InstaStabType.STAB_TYPE_AUTO;
            case 1:
                return InstaStabType.STAB_TYPE_PANORAMA;
            case 2:
                return InstaStabType.STAB_TYPE_CALIBRATE_HORIZON;
            case 3:
                return InstaStabType.STAB_TYPE_FOOTAGE_MOTION_SMOOTH;
        }
    }

    private void restartPreview() {
        PreviewParamsBuilder builder = new PreviewParamsBuilder()
                .setStreamResolution(mCurrentResolution)
                .setPreviewType(InstaCameraManager.PREVIEW_TYPE_LIVE) // PREVIEW_TYPE_NORMAL
                .setAudioEnabled(mCbAudioEnabled.isChecked());
        InstaCameraManager.getInstance().closePreviewStream();
        InstaCameraManager.getInstance().startPreviewStream(builder);
    }

    private boolean checkToStartLive() {
        String rtmp = mEtRtmp.getText().toString();
        String width = mEtWidth.getText().toString();
        String height = mEtHeight.getText().toString();
        String fps = mEtFps.getText().toString();
        String bitrate = mEtBitrate.getText().toString();
        if (TextUtils.isEmpty(rtmp) || TextUtils.isEmpty(width) || TextUtils.isEmpty(height)
                || TextUtils.isEmpty(fps) || TextUtils.isEmpty(bitrate)) {
            Toast.makeText(this, R.string.live_toast_input_parameters, Toast.LENGTH_SHORT).show();
        } else if (!Pattern.matches("(rtmp|rtmps)://([\\w.]+/?)\\S*", rtmp)) {
            Toast.makeText(this, R.string.live_toast_invalid_rtmp, Toast.LENGTH_SHORT).show();
        } else {
            mCapturePlayerView.setLiveType(mCbPanorama.isChecked() ? InstaCapturePlayerView.LIVE_TYPE_PANORAMA : InstaCapturePlayerView.LIVE_TYPE_RECORDING);
            LiveParamsBuilder builder = new LiveParamsBuilder()
                    .setRtmp(rtmp)
                    .setWidth(Integer.parseInt(width))
                    .setHeight(Integer.parseInt(height))
                    .setFps(Integer.parseInt(fps))
                    .setBitrate(Integer.parseInt(bitrate) * 1024 * 1024)
                    .setPanorama(mCbPanorama.isChecked())
                    // 设置网络ID即可在使用WIFI连接相机时使用4G网络推流
                    // set NetId to use 4G to push live streaming when connecting camera by WIFI
                    .setNetId(NetworkManager.getInstance().getMobileNetId());
            InstaCameraManager.getInstance().startLive(builder, this);
            return true;
        }
        return false;
    }

    private void stopLive() {
        InstaCameraManager.getInstance().stopLive();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            // Auto close preview after page loses focus
            InstaCameraManager.getInstance().stopLive();
            InstaCameraManager.getInstance().closePreviewStream();
            InstaCameraManager.getInstance().setPreviewStatusChangedListener(null);
            mCapturePlayerView.destroy();
            NetworkManager.getInstance().clearBindProcess();
            mBtnBindNetwork.setChecked(false);
        }
    }

    @Override
    public void onOpened() {
        // Preview stream is on and can be played
        InstaCameraManager.getInstance().setStreamEncode();
        mCapturePlayerView.setPlayerViewListener(new PlayerViewListener() {
            @Override
            public void onLoadingFinish() {
                mBtnLive.setEnabled(true);
                InstaCameraManager.getInstance().setPipeline(mCapturePlayerView.getPipeline());
            }

            @Override
            public void onReleaseCameraPipeline() {
                InstaCameraManager.getInstance().setPipeline(null);
            }
        });
        mCapturePlayerView.prepare(createParams());
        mCapturePlayerView.play();
        mCapturePlayerView.setKeepScreenOn(true);
    }

    private CaptureParamsBuilder createParams() {
        CaptureParamsBuilder builder = new CaptureParamsBuilder()
                .setCameraType(InstaCameraManager.getInstance().getCameraType())
                .setMediaOffset(InstaCameraManager.getInstance().getMediaOffset())
                .setMediaOffsetV2(InstaCameraManager.getInstance().getMediaOffsetV2())
                .setMediaOffsetV3(InstaCameraManager.getInstance().getMediaOffsetV3())
                .setCameraSelfie(InstaCameraManager.getInstance().isCameraSelfie())
                .setGyroTimeStamp(InstaCameraManager.getInstance().getGyroTimeStamp())
                .setBatteryType(InstaCameraManager.getInstance().getBatteryType())
                .setStabType(getStabType())
                .setStabEnabled(mSpinnerStabType.getSelectedItemPosition() != 4)
                .setLive(true)
                .setResolutionParams(mCurrentResolution.width, mCurrentResolution.height, mCurrentResolution.fps);

        builder.setRenderModelType(CaptureParamsBuilder.RENDER_MODE_PLANE_STITCH)
                .setCameraRenderSurfaceInfo(mImageReader.getSurface(), mImageReader.getWidth(), mImageReader.getHeight())
                .setScreenRatio(1, 1);

        return builder;
    }

    @Override
    public void onIdle() {
        // Preview Stopped
        mBtnLive.setEnabled(false);
        mCapturePlayerView.destroy();
        mCapturePlayerView.setKeepScreenOn(false);
    }

    @Override
    public void onLivePushStarted() {
        mTvLiveStatus.setText(R.string.live_push_started);
    }

    @Override
    public void onLivePushFinished() {
        mBtnLive.setChecked(false);
        mTvLiveStatus.setText(R.string.live_push_finished);
    }

    @Override
    public void onLivePushError(int error, String desc) {
        mBtnLive.setChecked(false);
        mTvLiveStatus.setText(getString(R.string.live_push_error) + " (" + error + ")");
    }

    @Override
    public void onLiveFpsUpdate(int fps) {
        mTvLiveStatus.setText(getString(R.string.live_fps_update, fps));
    }

    @Override
    public void onCameraStatusChanged(boolean enabled) {
        super.onCameraStatusChanged(enabled);
        if (!enabled) {
            mBtnLive.setChecked(false);
            mBtnLive.setEnabled(false);
        }
    }

    private void saveLiveData() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.edit().putString("rtmp", mEtRtmp.getText().toString())
                .putString("width", mEtWidth.getText().toString())
                .putString("height", mEtHeight.getText().toString())
                .putString("fps", mEtFps.getText().toString())
                .putString("bitrate", mEtBitrate.getText().toString())
                .putBoolean("panorama", mCbPanorama.isChecked())
                .putBoolean("audio", mCbAudioEnabled.isChecked())
                .apply();
    }

    private void restoreLiveData() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        mEtRtmp.setText(sp.getString("rtmp", ""));
        mEtWidth.setText(sp.getString("width", ""));
        mEtHeight.setText(sp.getString("height", ""));
        mEtFps.setText(sp.getString("fps", ""));
        mEtBitrate.setText(sp.getString("bitrate", ""));
        mCbPanorama.setChecked(sp.getBoolean("panorama", true));
        mCbAudioEnabled.setChecked(sp.getBoolean("audio", true));
    }

    @Override
    public void onOpening() {
        // Preview Opening
        // If you want to set your custom surface, do like this.
        createSurfaceView();
    }

    private void createSurfaceView() {
        if (mImageReader != null) {
            return;
        }

        File dir = new File(getExternalCacheDir(), "preview_jpg");
        dir.mkdirs();
        mImageReaderHandlerThread = new HandlerThread("camera render surface");
        mImageReaderHandlerThread.start();

        mImageReaderHandler = new Handler(mImageReaderHandlerThread.getLooper());
//        mImageReader = ImageReader.newInstance(mCapturePlayerView.getWidth(), mCapturePlayerView.getHeight(), PixelFormat.RGBA_8888, 1);
        mImageReader = ImageReader.newInstance(3840, 1920, PixelFormat.RGBA_8888, 1);


        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                Log.i(TAG, "image format " + image.getFormat()
                        + " getWidth " + image.getWidth()
                        + " get height " + image.getHeight()
                        + " timestamp " + image.getTimestamp());
                int planeCount = image.getPlanes().length;

                Log.i(TAG, "plane count " + planeCount);
                Image.Plane plane = image.getPlanes()[0];
                int pixelStride = plane.getPixelStride();
                int rowStride = plane.getRowStride();
                int rowPadding = rowStride - pixelStride * image.getWidth();
                Log.i(TAG, " plane getPixelStride " + pixelStride + " getRowStride " + rowStride);

                Bitmap bitmap = Bitmap.createBitmap(image.getWidth() + rowPadding / pixelStride, image.getHeight(), Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(plane.getBuffer());

                plane.getBuffer().position(0);
                Log.d(TAG,"bitmap size HxW: "+bitmap.getHeight()+"x"+bitmap.getWidth());
                Bitmap cropped = Bitmap.createBitmap(bitmap, 0,0,image.getWidth(),image.getHeight());
                Log.d(TAG,"cropped bitmap size HxW: "+cropped.getHeight()+"x"+cropped.getWidth());

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                cropped.compress(Bitmap.CompressFormat.PNG, 100, out);
                img_bytes = out.toByteArray();

                try {
                    // SeqNUM[0,1,2] // dataChannel 1 ~ 3
                    if(WC.dataChannel1.state() == DataChannel.State.OPEN && WC.dataChannel2.state() == DataChannel.State.OPEN /*&& WC.dataChannel3.state() == DataChannel.State.OPEN*/){
                        if (WC.dataChannels.get(SeqNum).state() == DataChannel.State.OPEN && WC.dataChannels.get(SeqNum).bufferedAmount() <= 2000000 /*&& is_OK*/) {
                            is_OK = false;
                            String encodedString = "";
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                encodedString = Base64.getEncoder().encodeToString(img_bytes);
                                Log.d(TAG,"img_bytes Length!"+img_bytes.length);
                                Log.d(TAG,"encoded String!!"+encodedString);
                            }
                            int size = encodedString.length();
                            Log.i(TAG,"encodedString length: "+size);

                            String s = "-i "+size;
                            DataChannel.Buffer msgBuf = new DataChannel.Buffer(ByteBuffer.wrap(s.getBytes(Charset.defaultCharset())), false);
                            WC.dataChannels.get(SeqNum).send(msgBuf);
                            Log.i(TAG,"Send msgBuf: "+s);

                            final int CHUNK_SIZE = 64000; // default : 64000
                            int numberOfChunks = size / CHUNK_SIZE;
                            int remainder = size % CHUNK_SIZE;

                            for (int j = 0; j < numberOfChunks; j++){
                                ByteBuffer wrap = ByteBuffer.wrap(encodedString.getBytes(Charset.defaultCharset()), j * CHUNK_SIZE, CHUNK_SIZE);
                                WC.dataChannels.get(SeqNum).send(new DataChannel.Buffer(wrap, false));
//                                Log.i(TAG,WC.dataChannels.get(SeqNum).label()+".send");
                            }
                            if (remainder > 0) {
                                ByteBuffer wrap = ByteBuffer.wrap(encodedString.getBytes(Charset.defaultCharset()), numberOfChunks * CHUNK_SIZE, remainder);
                                WC.dataChannels.get(SeqNum).send(new DataChannel.Buffer(wrap, false));
                            }
                            Log.i(TAG,WC.dataChannels.get(SeqNum).label()+".sent Last Part!!");
                            SeqNum++;
                            if (SeqNum > 1) SeqNum = 0;
                        }
                    }
                }catch (Exception e){
                    Log.e(TAG, "Image send ERROR!" + e);
                }
/*
                String filePath = dir.getAbsolutePath() + "/" + image.getTimestamp() + ".png";
                File imageFile = new File(filePath);

                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(imageFile);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                    Log.i(TAG, "path " + filePath);
                    try {
                        os.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
*/
                image.close();
            }
        }, mImageReaderHandler);
    }

    // WebRTC
    public class WebRTCClient {
        private static final String TAG = "WebRTCClient";
        public LiveActivity.WebRTCClient.SendOfferTask SendOfferTask;

        private PeerConnectionFactory peerConnectionFactory;

        private PeerConnection peerConnection1;
        private PeerConnection peerConnection2;
//        private PeerConnection peerConnection3;

        private List<DataChannel> dataChannels = new ArrayList<>();

        private DataChannel dataChannel1;
        private DataChannel dataChannel2;
//        private DataChannel dataChannel3;



        public WebRTCClient() {
            // PeerConnectionFactory 초기화
            PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(getApplicationContext())
                            .createInitializationOptions()
            );

            // PeerConnectionFactory 생성
            PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
            peerConnectionFactory = PeerConnectionFactory.builder()
                    .setOptions(options)
                    .createPeerConnectionFactory();
            Log.d(TAG, "WebRTCClient initialize and peerConnectionFactory create");
        }

        public void createPeerConnection() {
            // PeerConnection 생성
            ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
            // IceServer 설정 추가
            iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

            // PeerConnection 설정
            PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
            peerConnection1 = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
                @Override
                public void onIceCandidate(IceCandidate iceCandidate) {
                    // 로컬 ICE 후보가 생성되었을 때 호출됨
                    Log.d(TAG, "onIceCandidate: " + iceCandidate.toString());
                    // 상대방에게 전달할 ICE 후보를 처리
                }

                @Override
                public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                    // 로컬 ICE 후보가 제거되었을 때 호출됨
                    Log.d(TAG, "onIceCandidatesRemoved");
                    // 상대방에게 제거된 ICE 후보를 처리
                }

                @Override
                public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                    // 시그널링 상태 변경 시 호출됨
                    Log.d(TAG, "onSignalingChange: " + signalingState.toString());
                }

                @Override
                public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                    // ICE 연결 상태 변경 시 호출됨
                    Log.d(TAG, "onIceConnectionChange: " + iceConnectionState.toString());
                }

                @Override
                public void onIceConnectionReceivingChange(boolean b) {
                    // ICE 연결 수신 상태 변경 시 호출됨
                    Log.d(TAG, "onIceConnectionReceivingChange: " + b);
                }

                @Override
                public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                    // ICE 후보 수집 상태 변경 시 호출됨
                    Log.d(TAG, "onIceGatheringChange: " + iceGatheringState.toString());
                    if (iceGatheringState == PeerConnection.IceGatheringState.COMPLETE) {
                        String localSDP = String.valueOf(peerConnection1.getLocalDescription().description);
                        Log.i("info", "localSDP : " + localSDP);
                        LiveActivity.WebRTCClient.SendOfferTask sendOfferTask = new LiveActivity.WebRTCClient.SendOfferTask(localSDP, 1);
                        sendOfferTask.execute();
                    }
                }

                @Override
                public void onAddStream(MediaStream mediaStream) {
                    Log.d(TAG, "onAddStream: " + mediaStream.toString());
                }
                @Override
                public void onRemoveStream(MediaStream mediaStream) {
                    Log.d(TAG, "onRemoveStream: " + mediaStream.toString());
                }
                @Override
                public void onDataChannel(DataChannel dataChannel) {
                    Log.d(TAG, "onDataChannel: " + dataChannel.label());
                    Log.d(TAG, "onDataChannel: " + dataChannel.state());

                    dataChannel.registerObserver(new DataChannel.Observer() {
                        @Override
                        public void onBufferedAmountChange(long l) {
//                            Log.d(TAG, dataChannel.label()+" onBufferedAmountChange: " + l);
//                            Log.d(TAG, dataChannel.label()+" onBufferedAmount: " + dataChannel.bufferedAmount());
                        }
                        @Override
                        public void onStateChange() {
                            Log.d(TAG, "onStateChange: " + dataChannel.label());
                            Log.d(TAG, "onStateChange: " + dataChannel.state());
                        }
                        @Override
                        public void onMessage(DataChannel.Buffer buffer) {
                            ByteBuffer data = buffer.data;
                            byte[] bytes = new byte[data.remaining()];
                            data.get(bytes);
                            String msg = new String(bytes);
                            Log.d(TAG, "DataChannel onMessage: " + msg);
                        }
                    });
                }
                @Override
                public void onRenegotiationNeeded() {
                    Log.d(TAG, "onRenegotiationNeeded");
                }
                @Override
                public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                    Log.d(TAG, "onAddTrack");
                }
            });
            peerConnection2 = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
                @Override
                public void onIceCandidate(IceCandidate iceCandidate) {
                    // 로컬 ICE 후보가 생성되었을 때 호출됨
                    Log.d(TAG, "onIceCandidate: " + iceCandidate.toString());
                    // 상대방에게 전달할 ICE 후보를 처리
                }

                @Override
                public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                    // 로컬 ICE 후보가 제거되었을 때 호출됨
                    Log.d(TAG, "onIceCandidatesRemoved");
                    // 상대방에게 제거된 ICE 후보를 처리
                }

                @Override
                public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                    // 시그널링 상태 변경 시 호출됨
                    Log.d(TAG, "onSignalingChange: " + signalingState.toString());
                }

                @Override
                public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                    // ICE 연결 상태 변경 시 호출됨
                    Log.d(TAG, "onIceConnectionChange: " + iceConnectionState.toString());
                }

                @Override
                public void onIceConnectionReceivingChange(boolean b) {
                    // ICE 연결 수신 상태 변경 시 호출됨
                    Log.d(TAG, "onIceConnectionReceivingChange: " + b);
                }

                @Override
                public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                    // ICE 후보 수집 상태 변경 시 호출됨
                    Log.d(TAG, "onIceGatheringChange: " + iceGatheringState.toString());
                    if (iceGatheringState == PeerConnection.IceGatheringState.COMPLETE) {
                        String localSDP = String.valueOf(peerConnection2.getLocalDescription().description);
                        Log.i("info", "localSDP : " + localSDP);
                        LiveActivity.WebRTCClient.SendOfferTask sendOfferTask = new LiveActivity.WebRTCClient.SendOfferTask(localSDP, 2);
                        sendOfferTask.execute();
                    }
                }

                @Override
                public void onAddStream(MediaStream mediaStream) {
                    Log.d(TAG, "onAddStream: " + mediaStream.toString());
                }
                @Override
                public void onRemoveStream(MediaStream mediaStream) {
                    Log.d(TAG, "onRemoveStream: " + mediaStream.toString());
                }
                @Override
                public void onDataChannel(DataChannel dataChannel) {
                    Log.d(TAG, "onDataChannel: " + dataChannel.label());
                    Log.d(TAG, "onDataChannel: " + dataChannel.state());

                    dataChannel.registerObserver(new DataChannel.Observer() {
                        @Override
                        public void onBufferedAmountChange(long l) {
//                            Log.d(TAG, dataChannel.label()+" onBufferedAmountChange: " + l);
//                            Log.d(TAG, dataChannel.label()+" onBufferedAmount: " + dataChannel.bufferedAmount());
                        }
                        @Override
                        public void onStateChange() {
                            Log.d(TAG, "onStateChange: " + dataChannel.label());
                            Log.d(TAG, "onStateChange: " + dataChannel.state());
                        }
                        @Override
                        public void onMessage(DataChannel.Buffer buffer) {
                            ByteBuffer data = buffer.data;
                            byte[] bytes = new byte[data.remaining()];
                            data.get(bytes);
                            String msg = new String(bytes);
                            Log.d(TAG, "DataChannel onMessage: " + msg);
                        }
                    });
                }
                @Override
                public void onRenegotiationNeeded() {
                    Log.d(TAG, "onRenegotiationNeeded");
                }
                @Override
                public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                    Log.d(TAG, "onAddTrack");
                }
            });
            /*
            peerConnection3 = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
                @Override
                public void onIceCandidate(IceCandidate iceCandidate) {
                    // 로컬 ICE 후보가 생성되었을 때 호출됨
                    Log.d(TAG, "onIceCandidate: " + iceCandidate.toString());
                    // 상대방에게 전달할 ICE 후보를 처리
                }

                @Override
                public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                    // 로컬 ICE 후보가 제거되었을 때 호출됨
                    Log.d(TAG, "onIceCandidatesRemoved");
                    // 상대방에게 제거된 ICE 후보를 처리
                }

                @Override
                public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                    // 시그널링 상태 변경 시 호출됨
                    Log.d(TAG, "onSignalingChange: " + signalingState.toString());
                }

                @Override
                public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                    // ICE 연결 상태 변경 시 호출됨
                    Log.d(TAG, "onIceConnectionChange: " + iceConnectionState.toString());
                }

                @Override
                public void onIceConnectionReceivingChange(boolean b) {
                    // ICE 연결 수신 상태 변경 시 호출됨
                    Log.d(TAG, "onIceConnectionReceivingChange: " + b);
                }

                @Override
                public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                    // ICE 후보 수집 상태 변경 시 호출됨
                    Log.d(TAG, "onIceGatheringChange: " + iceGatheringState.toString());
                    if (iceGatheringState == PeerConnection.IceGatheringState.COMPLETE) {
                        String localSDP = String.valueOf(peerConnection3.getLocalDescription().description);
                        Log.i("info", "localSDP : " + localSDP);
                        LiveActivity.WebRTCClient.SendOfferTask sendOfferTask = new LiveActivity.WebRTCClient.SendOfferTask(localSDP, 3);
                        sendOfferTask.execute();
                    }
                }

                @Override
                public void onAddStream(MediaStream mediaStream) {
                    Log.d(TAG, "onAddStream: " + mediaStream.toString());
                }
                @Override
                public void onRemoveStream(MediaStream mediaStream) {
                    Log.d(TAG, "onRemoveStream: " + mediaStream.toString());
                }
                @Override
                public void onDataChannel(DataChannel dataChannel) {
                    Log.d(TAG, "onDataChannel: " + dataChannel.label());
                    Log.d(TAG, "onDataChannel: " + dataChannel.state());

                    dataChannel.registerObserver(new DataChannel.Observer() {
                        @Override
                        public void onBufferedAmountChange(long l) {
//                            Log.d(TAG, dataChannel.label()+" onBufferedAmountChange: " + l);
//                            Log.d(TAG, dataChannel.label()+" onBufferedAmount: " + dataChannel.bufferedAmount());
                        }
                        @Override
                        public void onStateChange() {
                            Log.d(TAG, "onStateChange: " + dataChannel.label());
                            Log.d(TAG, "onStateChange: " + dataChannel.state());
                        }
                        @Override
                        public void onMessage(DataChannel.Buffer buffer) {
                            ByteBuffer data = buffer.data;
                            byte[] bytes = new byte[data.remaining()];
                            data.get(bytes);
                            String msg = new String(bytes);
                            Log.d(TAG, "DataChannel onMessage: " + msg);
                        }
                    });
                }
                @Override
                public void onRenegotiationNeeded() {
                    Log.d(TAG, "onRenegotiationNeeded");
                }
                @Override
                public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                    Log.d(TAG, "onAddTrack");
                }
            });*/
        }

        public void createDataChannel() {
//            // DataChannel 초기화 설정
            DataChannel.Init dataChannelInit = new DataChannel.Init();
            dataChannelInit.id = 1;
            dataChannelInit.ordered = true;
            dataChannelInit.maxRetransmits = -1;

            // DataChannel 생성
            try {
                dataChannel1 = peerConnection1.createDataChannel("channel1", dataChannelInit);
                dataChannel1.registerObserver(new DataChannel.Observer() {
                    @Override
                    public void onBufferedAmountChange(long l) {
//                        Log.d(TAG, "onBufferedAmountChange: " + l);
//                        Log.d(TAG, dataChannel1.label()+" onBufferedAmount: " + dataChannel1.bufferedAmount());
                    }
                    @Override
                    public void onStateChange() {
                        Log.d(TAG, "onStateChange: " + dataChannel1.state());
                        if (dataChannel1.state() == DataChannel.State.OPEN) {
                            Log.d(TAG, "onStateChange: " + dataChannel1.label());
                            Log.d(TAG, "onStateChange: " + dataChannel1.state());
                        }
                    }
                    @Override
                    public void onMessage(DataChannel.Buffer buffer) {
                        ByteBuffer data = buffer.data;
                        byte[] bytes = new byte[data.remaining()];
                        data.get(bytes);
                        String msg = new String(bytes);
                        Log.d(TAG, "DataChannel1 onMessage: " + msg);
                    }
                });
                dataChannels.add(dataChannel1);
                Log.i("INFO", "createDataChannel1 success : " + dataChannel1.id());
            } catch (Exception e) {
                Log.e("ERROR!", "createDataChannel1 ERROR : " + e);
            }
            try {
                dataChannel2 = peerConnection2.createDataChannel("channel2", dataChannelInit);
                dataChannel2.registerObserver(new DataChannel.Observer() {
                    @Override
                    public void onBufferedAmountChange(long l) {
//                        Log.d(TAG, "onBufferedAmountChange: " + l);
//                        Log.d(TAG, dataChannel2.label()+" onBufferedAmount: " + dataChannel2.bufferedAmount());
                    }
                    @Override
                    public void onStateChange() {
                        Log.d(TAG, "onStateChange: " + dataChannel2.state());
                        if (dataChannel2.state() == DataChannel.State.OPEN) {
                            Log.d(TAG, "onStateChange: " + dataChannel2.label());
                            Log.d(TAG, "onStateChange: " + dataChannel2.state());
                        }
                    }
                    @Override
                    public void onMessage(DataChannel.Buffer buffer) {
                        ByteBuffer data = buffer.data;
                        byte[] bytes = new byte[data.remaining()];
                        data.get(bytes);
                        String msg = new String(bytes);
                        Log.d(TAG, "DataChannel2 onMessage: " + msg);
                    }
                });
                dataChannels.add(dataChannel2);
                Log.i("INFO", "createDataChannel2 success : " + dataChannel2.id());
            } catch (Exception e) {
                Log.e("ERROR!", "createDataChannel2 ERROR : " + e);
            }
            /*
            try {
                dataChannel3 = peerConnection3.createDataChannel("channel3", dataChannelInit);
                dataChannel3.registerObserver(new DataChannel.Observer() {
                    @Override
                    public void onBufferedAmountChange(long l) {
//                        Log.d(TAG, "onBufferedAmountChange: " + l);
//                        Log.d(TAG, dataChannel3.label()+" onBufferedAmount: " + dataChannel3.bufferedAmount());
                    }
                    @Override
                    public void onStateChange() {
                        Log.d(TAG, "onStateChange: " + dataChannel3.state());
                        if (dataChannel3.state() == DataChannel.State.OPEN) {
                            Log.d(TAG, "onStateChange: " + dataChannel3.label());
                            Log.d(TAG, "onStateChange: " + dataChannel3.state());
                        }
                    }
                    @Override
                    public void onMessage(DataChannel.Buffer buffer) {
                        ByteBuffer data = buffer.data;
                        byte[] bytes = new byte[data.remaining()];
                        data.get(bytes);
                        String msg = new String(bytes);
                        Log.d(TAG, "DataChannel3 onMessage: " + msg);
                    }
                });
                dataChannels.add(dataChannel3);
                Log.i("INFO", "createDataChannel3 success : " + dataChannel3.id());
            } catch (Exception e) {
                Log.e("ERROR!", "createDataChannel3 ERROR : " + e);
            }
            */
        }

        // send offer
        public class SendOfferTask extends AsyncTask<Void, Void, String> {

            private static final String TAG = "SendOfferTask";
            private static final String TARGET_URL = "https://a0b4-165-132-140-76.ngrok-free.app/offer"; // 대상 URL

            private String localSDP;
            private int peerNum;
            private PeerConnection peerConnection;

            public SendOfferTask(String localSDP, int peerNum) {
                this.localSDP = localSDP;
                this.peerNum = peerNum;
            }

            @Override
            protected String doInBackground(Void... voids) {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                try {
                    // 연결 설정
                    URL url = new URL(TARGET_URL);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setDoOutput(true);

                    // 전송할 데이터 생성
                    JSONObject requestData = new JSONObject();
                    requestData.put("sdp", localSDP);
                    requestData.put("type", "offer");

                    // 데이터 전송
                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(requestData.toString().getBytes());
                    outputStream.flush();

                    // 응답 받기
                    int responseCode = connection.getResponseCode();
                    Log.i("WebRTC INFO", "responseCode" + responseCode);
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // 응답 성공
                        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        return response.toString();
                    } else {
                        // 응답 실패
                        return null;
                    }
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    return null;
                } finally {
                    // 연결 종료 및 리소스 해제
                    if (connection != null) {
                        connection.disconnect();
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            protected void onPostExecute(String response) {
                if (response != null) {
                    // 응답 처리
                    try {
                        JSONObject responseJson = new JSONObject(response);
                        Log.i("INFO", "res:" + responseJson.toString());
                        String answerSDP = responseJson.getString("sdp");
                        // Answer SDP 처리
                        if(peerNum == 1){
                            peerConnection = peerConnection1;
                        }else if(peerNum == 2){
                            peerConnection = peerConnection2;
                        }/*else if(peerNum == 3){
                            peerConnection = peerConnection3;
                        }*/
                        peerConnection.setRemoteDescription(new SdpObserver() {
                            @Override
                            public void onCreateSuccess(SessionDescription sessionDescription) {
                                // Now you can get the local description
                                Log.d(TAG, "onCreateSuccess-setRemoteDescription: " + sessionDescription.type);
                                SessionDescription remoteDescription = peerConnection.getRemoteDescription();

                                if (remoteDescription != null) {
                                    // Local description is available
                                    // You can access it using localDescription.description
                                    Log.d(TAG, "Remote Description: " + remoteDescription.description);
                                } else {
                                    // Local description is not available
                                    Log.d(TAG, "Remote Description is null");
                                }
                            }

                            @Override
                            public void onSetSuccess() {
                                Log.d(TAG, "onSetSuccess-setRemoteDescription");
                            }

                            @Override
                            public void onCreateFailure(String s) {
                                Log.e(TAG, "onCreateFailure-setRemoteDescription" + s);
                            }

                            @Override
                            public void onSetFailure(String s) {
                                Log.e(TAG, "onSetFailure-setRemoteDescription" + s);
                            }
                        }, new SessionDescription(SessionDescription.Type.ANSWER, answerSDP));
                        Log.i("info", "remoteSDP : " + peerConnection.getRemoteDescription().description);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    // 응답 실패 처리
                    Log.e(TAG, "Failed to send offer");
                }
            }
        }
    }

}
