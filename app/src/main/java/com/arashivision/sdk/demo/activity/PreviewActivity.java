package com.arashivision.sdk.demo.activity;

import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.ToggleButton;

import com.arashivision.insta360.basecamera.camera.CameraType;
import com.arashivision.sdk.demo.R;
import com.arashivision.sdkcamera.camera.InstaCameraManager;
import com.arashivision.sdkcamera.camera.callback.IPreviewStatusListener;
import com.arashivision.sdkcamera.camera.resolution.PreviewStreamResolution;
import com.arashivision.sdkmedia.player.capture.CaptureParamsBuilder;
import com.arashivision.sdkmedia.player.capture.InstaCapturePlayerView;
import com.arashivision.sdkmedia.player.config.InstaStabType;
import com.arashivision.sdkmedia.player.listener.PlayerViewListener;

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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

public class PreviewActivity extends BaseObserveCameraActivity implements IPreviewStatusListener {

    private ViewGroup mLayoutContent;
    private InstaCapturePlayerView mCapturePlayerView;
    private ToggleButton mBtnSwitch;
    private RadioButton mRbNormal;
    private RadioButton mRbFisheye;
    private RadioButton mRbPerspective;
    private RadioButton mRbPlane;
    private Spinner mSpinnerResolution;
    private Spinner mSpinnerStabType;

    private PreviewStreamResolution mCurrentResolution;

    private ImageReader mImageReader;
    private HandlerThread mImageReaderHandlerThread;
    private Handler mImageReaderHandler;

    private final static String TAG = PreviewActivity.class.getName();

    private PreviewActivity.WebRTCClient WC;

    private int i = 1;

    private byte[] img_bytes;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        setTitle(R.string.preview_toolbar_title);
        bindViews();

        // 进入页面后可自动开启预览
        // Auto open preview after page gets focus
        InstaCameraManager.getInstance().setPreviewStatusChangedListener(this);
        // mSpinnerResolution的onItemSelected会自动触发开启预览，故此处注释掉
        // mSpinnerResolution -> onItemSelected() Will automatically trigger to open the preview, so comment it out here
//        InstaCameraManager.getInstance().startPreviewStream();

        WC = new PreviewActivity.WebRTCClient();

        WC.createPeerConnection();

        WC.createDataChannel();

        WC.peerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                // Set local description
                // Offer or Answer 생성 성공 시 호출됨
                Log.d(TAG, "onCreateSuccess: " + sessionDescription.type);
                // 생성된 SDP 설정을 로컬 SDP로 설정하고 상대방에게 전송하는 작업을 수행
                WC.peerConnection.setLocalDescription(new SdpObserver() {
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        //peerConnection.setLocalDescription(sdpObserver, sessionDescription);
                        // Local description set successfully
                        // Now you can get the local description
                        Log.d(TAG, "onCreateSuccess-setLocalDescription: " + sessionDescription.type);
                        SessionDescription localDescription = WC.peerConnection.getLocalDescription();

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
    }

    private void bindViews() {
        mLayoutContent = findViewById(R.id.layout_content);
        mCapturePlayerView = findViewById(R.id.player_capture);
        mCapturePlayerView.setLifecycle(getLifecycle());

        mBtnSwitch = findViewById(R.id.btn_switch);
        mBtnSwitch.setOnClickListener(v -> {
            if (mBtnSwitch.isChecked()) {
                if (mCurrentResolution == null) {
                    InstaCameraManager.getInstance().startPreviewStream();
                } else {
                    InstaCameraManager.getInstance().startPreviewStream(mCurrentResolution);
                }
            } else {
                InstaCameraManager.getInstance().closePreviewStream();
            }
        });

        mRbNormal = findViewById(R.id.rb_normal);
        mRbFisheye = findViewById(R.id.rb_fisheye);
        mRbPerspective = findViewById(R.id.rb_perspective);
        mRbPlane = findViewById(R.id.rb_plane);
        RadioGroup radioGroup = findViewById(R.id.rg_preview_mode);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // 在平铺和其他模式之间切换需要重启预览流
            // Need to restart the preview stream when switching between plane and others
            if (checkedId == R.id.rb_plane) {
                InstaCameraManager.getInstance().closePreviewStream();
                if (mCurrentResolution == null) {
                    InstaCameraManager.getInstance().startPreviewStream();
                    createSurfaceView();
                } else {
                    InstaCameraManager.getInstance().startPreviewStream(mCurrentResolution);
                    createSurfaceView();
                }
                mRbFisheye.setEnabled(false);
                mRbPerspective.setEnabled(false);
            } else if (checkedId == R.id.rb_normal) {
                if (!mRbFisheye.isEnabled() || !mRbPerspective.isEnabled()) {
                    InstaCameraManager.getInstance().closePreviewStream();
                    if (mCurrentResolution == null) {
                        InstaCameraManager.getInstance().startPreviewStream();
                    } else {
                        InstaCameraManager.getInstance().startPreviewStream(mCurrentResolution);
                    }
                    mRbFisheye.setEnabled(true);
                    mRbPerspective.setEnabled(true);
                } else {
                    // 切换到普通模式
                    // Switch to Normal Mode
                    mCapturePlayerView.switchNormalMode();
                }
            } else if (checkedId == R.id.rb_fisheye) {
                // 切换到鱼眼模式
                // Switch to Fisheye Mode
                mCapturePlayerView.switchFisheyeMode();
            } else if (checkedId == R.id.rb_perspective) {
                // 切换到透视模式
                // Switch to Perspective Mode
                mCapturePlayerView.switchPerspectiveMode();
            }
        });

        mSpinnerResolution = findViewById(R.id.spinner_resolution);
        ArrayAdapter<PreviewStreamResolution> adapter1 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        adapter1.addAll(InstaCameraManager.getInstance().getSupportedPreviewStreamResolution(InstaCameraManager.PREVIEW_TYPE_NORMAL));
        mSpinnerResolution.setAdapter(adapter1);
        mSpinnerResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurrentResolution = adapter1.getItem(position);
                InstaCameraManager.getInstance().closePreviewStream();
                InstaCameraManager.getInstance().startPreviewStream(mCurrentResolution);
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
                    InstaCameraManager.getInstance().closePreviewStream();
                    if (mCurrentResolution == null) {
                        InstaCameraManager.getInstance().startPreviewStream();
                    } else {
                        InstaCameraManager.getInstance().startPreviewStream(mCurrentResolution);
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

    @Override
    protected void onStop() {
        super.onStop();
        if (isFinishing()) {
            // 退出页面时需要关闭预览
            // Auto close preview after page loses focus
            InstaCameraManager.getInstance().setPreviewStatusChangedListener(null);
            InstaCameraManager.getInstance().closePreviewStream();
            mCapturePlayerView.destroy();
        }
    }

    @Override
    public void onOpening() {
        // 预览开启中
        // Preview Opening
        mBtnSwitch.setChecked(true);
    }

    @Override
    public void onOpened() {
        // 预览开启成功，可以播放预览流
        // Preview stream is on and can be played
        InstaCameraManager.getInstance().setStreamEncode();
        mCapturePlayerView.setPlayerViewListener(new PlayerViewListener() {
            @Override
            public void onLoadingFinish() {
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
                .setCameraRenderSurfaceInfo(mImageReader.getSurface(), mImageReader.getWidth(), mImageReader.getHeight());
        if (mCurrentResolution != null) {
            builder.setResolutionParams(mCurrentResolution.width, mCurrentResolution.height, mCurrentResolution.fps);
        }
        if (mRbPlane.isChecked()) {
            // 平铺模式
            // Plane Mode
            builder.setRenderModelType(CaptureParamsBuilder.RENDER_MODE_PLANE_STITCH)
                    .setScreenRatio(1, 1);
        } else {
            // 普通模式
            // Normal Mode
            builder.setRenderModelType(CaptureParamsBuilder.RENDER_MODE_AUTO);
        }
        return builder;
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
//        mImageReader = ImageReader.newInstance(mCurrentResolution.width, mCurrentResolution.height, PixelFormat.RGBA_8888, 1);
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

                Bitmap bitmap = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    bitmap = Bitmap.createBitmap(image.getWidth() + rowPadding / pixelStride, image.getHeight(), Bitmap.Config.ARGB_8888);
                }

                bitmap.copyPixelsFromBuffer(plane.getBuffer());
                plane.getBuffer().position(0);
                Log.d(TAG,"bitmap size HxW: "+bitmap.getHeight()+"x"+bitmap.getWidth());
                Bitmap cropped = Bitmap.createBitmap(bitmap, 0,0,image.getWidth(),image.getHeight());
                Log.d(TAG,"cropped bitmap size HxW: "+cropped.getHeight()+"x"+cropped.getWidth());

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                cropped.compress(Bitmap.CompressFormat.PNG, 100, out);
                img_bytes = out.toByteArray();

                try {
                    if (WC.dataChannel != null) {
                        if (WC.dataChannel.state() == DataChannel.State.OPEN && i == 1) {
                            i = 0;
//                            String s = "try to send image!";
//                            DataChannel.Buffer msgBuf = new DataChannel.Buffer(ByteBuffer.wrap(s.getBytes(Charset.defaultCharset())), false);
//                            WC.dataChannel2.send(msgBuf);
//                            Log.i(TAG,"Send msgBuf: "+msgBuf);

//                            ByteBuffer planeBuffer = plane.getBuffer();
//                            Log.i(TAG,"plane.getBuffer()"+planeBuffer);
//                            img_bytes = new byte[planeBuffer.remaining()];
//                            planeBuffer.get(img_bytes);
//                            planeBuffer.position(0);

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
                            WC.dataChannel.send(msgBuf);
                            Log.i(TAG,"Send msgBuf: "+s);

                            final int CHUNK_SIZE = 64000;
                            int numberOfChunks = size / CHUNK_SIZE;

                            for (int j = 0; j < numberOfChunks; j++){
                                ByteBuffer wrap = ByteBuffer.wrap(encodedString.getBytes(Charset.defaultCharset()), j * CHUNK_SIZE, CHUNK_SIZE);
                                WC.dataChannel.send(new DataChannel.Buffer(wrap, false));
                                Log.i(TAG,"WC.dataChannel.send");
                            }
                            int remainder = size % CHUNK_SIZE;
                            if (remainder > 0 ){
                                ByteBuffer wrap = ByteBuffer.wrap(encodedString.getBytes(Charset.defaultCharset()), numberOfChunks * CHUNK_SIZE, remainder);
                                WC.dataChannel.send(new DataChannel.Buffer(wrap, false));
                                Log.i(TAG,"WC.dataChannel.send LAST PART");
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
                        }
                    }
                }catch (Exception e){
                    Log.e(TAG,"Image send ERROR!"+e);
                }


                image.close();
            }
        }, mImageReaderHandler);
    }

    // WebRTC
    public class WebRTCClient {
        private static final String TAG = "WebRTCClient";
        public PreviewActivity.WebRTCClient.SendOfferTask SendOfferTask;

        private PeerConnectionFactory peerConnectionFactory;
        PeerConnection peerConnection;
        private DataChannel dataChannel;

        private DataChannel.Observer dataChannelObserver = new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long l) {
                // 데이터 채널의 버퍼 크기가 변경되었을 때 호출됨
                Log.d(TAG, "onBufferedAmountChange: " + l);
            }

            @Override
            public void onStateChange() {
                // 데이터 채널의 상태 변경 시 호출됨
                Log.d(TAG, "onStateChange: " + dataChannel.state());
                if (dataChannel.state() == DataChannel.State.OPEN) {
                    Log.d(TAG, "onStateChange: " + dataChannel.label());
                    Log.d(TAG, "onStateChange: " + dataChannel.id());
                    Log.d(TAG, "onStateChange: " + dataChannel.state());
                    Log.d(TAG, "onStateChange: " + dataChannel);

                    String message = "Hello, World!";
                    DataChannel.Buffer buffer = new DataChannel.Buffer(ByteBuffer.wrap(message.getBytes()), false);
                    dataChannel.send(buffer);
                }
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                // 데이터 채널에서 메시지를 수신할 때 호출됨
                ByteBuffer data = buffer.data;
                byte[] bytes = new byte[data.remaining()];
                data.get(bytes);
                String msg = new String(bytes);
                Log.d(TAG, "DataChannel onMessage: " + msg);
            }
        };

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
            peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
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
                        String localSDP = String.valueOf(peerConnection.getLocalDescription().description);
                        Log.i("info", "localSDP : " + localSDP);
                        PreviewActivity.WebRTCClient.SendOfferTask sendOfferTask = new PreviewActivity.WebRTCClient.SendOfferTask(localSDP);
                        sendOfferTask.execute();
                    }
                }

                @Override
                public void onAddStream(MediaStream mediaStream) {
                    // 미디어 스트림이 추가되었을 때 호출됨
                    Log.d(TAG, "onAddStream: " + mediaStream.toString());
                }

                @Override
                public void onRemoveStream(MediaStream mediaStream) {
                    // 미디어 스트림이 제거되었을 때 호출됨
                    Log.d(TAG, "onRemoveStream: " + mediaStream.toString());
                }

                @Override
                public void onDataChannel(DataChannel dataChannel) {
                    // 데이터 채널이 생성되었을 때 호출됨
                    Log.d(TAG, "onDataChannel Event!!");
                    Log.d(TAG, "onDataChannel: " + dataChannel.label());
                    Log.d(TAG, "onDataChannel: " + dataChannel.id());
                    Log.d(TAG, "onDataChannel: " + dataChannel.state());

                    // 데이터 채널 처리 작업 수행
                    // dataChannelObserver를 등록하여 데이터 채널 이벤트 처리
                    dataChannel.registerObserver(dataChannelObserver);
                }

                @Override
                public void onRenegotiationNeeded() {
                    // renegotiation이 필요할 때 호출됨
                    Log.d(TAG, "onRenegotiationNeeded");
                    // renegotiation 작업 수행
                }

                @Override
                public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
                    Log.d(TAG, "onAddTrack");
                }
            });
        }

        public void createDataChannel() {
//            // DataChannel 초기화 설정
            DataChannel.Init dataChannelInit = new DataChannel.Init();
            dataChannelInit.id = 1;
            dataChannelInit.ordered = true;
            dataChannelInit.maxRetransmits = -1;

            // DataChannel 생성
            try {
                dataChannel = peerConnection.createDataChannel("chat123", dataChannelInit);
                dataChannel.registerObserver(dataChannelObserver);

                Log.i("INFO", "createDataChannel success : " + dataChannel.id());
            } catch (Exception e) {
                Log.e("ERROR!", "createDataChannel ERROR : " + e);
            }
        }

        public void sendMessage(String message) {
            // 데이터 채널을 통해 메시지 전송
            DataChannel.Buffer buffer = new DataChannel.Buffer(ByteBuffer.wrap(message.getBytes()), false);
            dataChannel.send(buffer);
        }

        // send offer
        public class SendOfferTask extends AsyncTask<Void, Void, String> {

            private static final String TAG = "SendOfferTask";
            private static final String TARGET_URL = "http://e748-165-132-105-118.ngrok-free.app/offer"; // 대상 URL

            private final String localSDP;

            public SendOfferTask(String localSDP) {
                this.localSDP = localSDP;
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

    @Override
    public void onIdle() {
        // 预览已停止
        // Preview Stopped
        mCapturePlayerView.destroy();
        mCapturePlayerView.setKeepScreenOn(false);
    }

    @Override
    public void onError() {
        // 预览开启失败
        // Preview Failed
        mBtnSwitch.setChecked(false);
    }

}
