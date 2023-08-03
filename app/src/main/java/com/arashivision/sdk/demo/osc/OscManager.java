package com.arashivision.sdk.demo.osc;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.arashivision.sdk.demo.model.CaptureExposureData;
import com.arashivision.sdk.demo.osc.callback.IOscCallback;
import com.arashivision.sdk.demo.osc.delegate.IOscRequestDelegate;
import com.arashivision.sdkcamera.camera.InstaCameraManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class OscManager {

    private static class OscManagerHolder {
        private static OscManager instance = new OscManager();
    }

    private OscManager() {
    }

    public static OscManager getInstance() {
        return OscManagerHolder.instance;
    }

    // OSC command will return the states below
    private static final String CMD_STATE_DONE = "done";
    private static final String CMD_STATE_IN_PROGRESS = "inProgress";
    private static final String CMD_STATE_ERROR = "error";

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private ExecutorService mRequestExecutor = Executors.newSingleThreadExecutor();
    private IOscRequestDelegate mOscRequestDelegate;

    public void setOscRequestDelegate(@NonNull IOscRequestDelegate oscRequestDelegate) {
        mOscRequestDelegate = oscRequestDelegate;
    }

    /**
     * Use "/osc/commands/execute/camera.setOptions" to Set Options
     *
     * @param options  OscOptions for parameter details
     * @param callback If successful, callback returns null
     */
    public void setOptions(@NonNull String options, @Nullable IOscCallback callback) {
        mRequestExecutor.execute(() -> {
            try {
                if (callback != null) {
                    mHandler.post(callback::onStartRequest);
                }
                String cmd = "{\"name\":\"camera.setOptions\",\"parameters\":{\"options\":{" + options + "}}}";
                OSCResult oscResult = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd, getHttpHeaders());
                if (oscResult.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult.getResult());
                    if (CMD_STATE_DONE.equals(jsonObject.getString("state"))) {
                        if (callback != null) {
                            mHandler.post(() -> callback.onSuccessful(null));
                        }
                    } else {
                        if (callback != null) {
                            mHandler.post(() -> callback.onError(getErrorMessage(oscResult.getResult())));
                        }
                    }
                } else {
                    if (callback != null) {
                        mHandler.post(() -> callback.onError(oscResult.getResult()));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    mHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Use "/osc/commands/execute/camera.setOptions" to Set Options if needed
     * Then use "/osc/commands/execute/camera.takePicture" to take pictures
     * Then use "/osc/commands/status" to get result files
     *
     * @param options  Set the Options first, and then take a photo automatically.
     *                 If the current CaptureMode is image, select it, otherwise it is required
     * @param callback If successful, callback returns file address (String[] urls), could be downloaded to local
     */
    public void takePicture(@Nullable String options, @Nullable IOscCallback callback) {
        mRequestExecutor.execute(() -> {
            try {
                if (callback != null) {
                    mHandler.post(callback::onStartRequest);
                }
                // SetOptions
                if (options != null) {
                    String cmd = "{\"name\":\"camera.setOptions\",\"parameters\":{\"options\":{" + options + "}}}";
                    OSCResult oscResult = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd, getHttpHeaders());
                    if (oscResult.isSuccessful()) {
                        JSONObject jsonObject = new JSONObject(oscResult.getResult());
                        if (CMD_STATE_ERROR.equals(jsonObject.getString("state"))) {
                            if (callback != null) {
                                mHandler.post(() -> callback.onError(getErrorMessage(oscResult.getResult())));
                            }
                            return;
                        }
                    } else {
                        if (callback != null) {
                            mHandler.post(() -> callback.onError(oscResult.getResult()));
                        }
                        return;
                    }
                }
                // TakePicture
                String cmdId;
                String cmd = "{\"name\":\"camera.takePicture\"}";
                OSCResult oscResult = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd, getHttpHeaders());
                if (oscResult.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult.getResult());
                    if (CMD_STATE_IN_PROGRESS.equals(jsonObject.getString("state"))) {
                        cmdId = jsonObject.getString("id");
                    } else {
                        if (callback != null) {
                            String errorMsg = getErrorMessage(oscResult.getResult());
                            mHandler.post(() -> callback.onError(errorMsg));
                        }
                        return;
                    }
                } else {
                    if (callback != null) {
                        String errorMsg = oscResult.getResult();
                        mHandler.post(() -> callback.onError(errorMsg));
                    }
                    return;
                }
                // QueryResult
                if (!TextUtils.isEmpty(cmdId)) {
                    cmd = "{\"id\":\"" + cmdId + "\"}";
                    for (int i = 0; i < 60; i++) {
                        oscResult = mOscRequestDelegate.sendRequestByPost(getOscCmdStatusUrl(), cmd, getHttpHeaders());
                        if (oscResult.isSuccessful()) {
                            JSONObject jsonObject = new JSONObject(oscResult.getResult());
                            if (CMD_STATE_DONE.equals(jsonObject.getString("state"))) {
                                if (callback != null) {
                                    // parse file address from result
                                    JSONObject results = jsonObject.getJSONObject("results");
                                    String path = null;
                                    if (results.has("_fileGroup")) {
                                        path = results.getString("_fileGroup");
                                    }
                                    if (TextUtils.isEmpty(path) || path.equals("[]")) {
                                        path = results.getString("fileUrl");
                                    }
                                    path = path.replaceAll("\\[", "").replaceAll("]", "").replaceAll("\\\\/", "/");
                                    String[] paths = path.split(",");
                                    for (int j = 0; j < paths.length; j++) {
                                        paths[j] = paths[j].replaceAll("\"", "");
                                    }
                                    mHandler.post(() -> callback.onSuccessful(paths));
                                }
                                return;
                            }
                        }
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException exception) {
                            exception.printStackTrace();
                        }
                    }
                    if (callback != null) {
                        mHandler.post(() -> callback.onError("Timeout. Please use command \"listFiles\" to get."));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    mHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Use "/osc/commands/execute/camera.setOptions" to Set Options if needed
     * Then use "/osc/commands/execute/camera.startCapture" to start record
     *
     * @param options  Set the Options first, and then take a photo automatically.
     *                 If the current CaptureMode is video, select it, otherwise it is required
     * @param callback If successful, callback returns null
     */
    public void startRecord(@Nullable String options, @Nullable IOscCallback callback) {
        mRequestExecutor.execute(() -> {
            try {
                if (callback != null) {
                    mHandler.post(callback::onStartRequest);
                }
                // SetOptions
                if (options != null) {
                    String cmd = "{\"name\":\"camera.setOptions\",\"parameters\":{\"options\":{" + options + "}}}";
                    OSCResult oscResult = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd, getHttpHeaders());
                    if (oscResult.isSuccessful()) {
                        JSONObject jsonObject = new JSONObject(oscResult.getResult());
                        if (CMD_STATE_ERROR.equals(jsonObject.getString("state"))) {
                            if (callback != null) {
                                mHandler.post(() -> callback.onError(getErrorMessage(oscResult.getResult())));
                            }
                            return;
                        }
                    } else {
                        if (callback != null) {
                            mHandler.post(() -> callback.onError(oscResult.getResult()));
                        }
                        return;
                    }
                }
                // StartCapture
                String cmd = "{\"name\":\"camera.startCapture\"}";
                OSCResult oscResult = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd, getHttpHeaders());
                if (oscResult.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult.getResult());
                    if (CMD_STATE_DONE.equals(jsonObject.getString("state"))) {
                        if (callback != null) {
                            mHandler.post(() -> callback.onSuccessful(null));
                        }
                    } else {
                        if (callback != null) {
                            mHandler.post(() -> callback.onError(getErrorMessage(oscResult.getResult())));
                        }
                    }
                } else {
                    if (callback != null) {
                        mHandler.post(() -> callback.onError(oscResult.getResult()));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    mHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Use "/osc/commands/execute/camera.stopCapture" to stop record
     *
     * @param callback If successful, callback returns file address (String[] urls), could be downloaded to local
     */
    public void stopRecord(@Nullable IOscCallback callback) {
        mRequestExecutor.execute(() -> {
            try {
                if (callback != null) {
                    mHandler.post(callback::onStartRequest);
                }
                String cmd = "{\"name\":\"camera.stopCapture\"}";
                OSCResult oscResult = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd, getHttpHeaders());
                if (oscResult.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult.getResult());
                    if (CMD_STATE_DONE.equals(jsonObject.getString("state"))) {
                        if (callback != null) {
                            // parse file address from result
                            JSONObject results = jsonObject.getJSONObject("results");
                            String path = results.getString("fileUrls");
                            path = path.replaceAll("\\[", "").replaceAll("]", "").replaceAll("\\\\/", "/");
                            String[] paths = path.split(",");
                            for (int i = 0; i < paths.length; i++) {
                                paths[i] = paths[i].replaceAll("\"", "");
                            }
                            if (paths.length == 2 && paths[1].contains("_00_") && paths[0].contains("_10_")) {
                                String tmp = paths[0];
                                paths[0] = paths[1];
                                paths[1] = tmp;
                            }
                            mHandler.post(() -> callback.onSuccessful(paths));
                        }
                    } else {
                        if (callback != null) {
                            mHandler.post(() -> callback.onError(getErrorMessage(oscResult.getResult())));
                        }
                    }
                } else {
                    if (callback != null) {
                        mHandler.post(() -> callback.onError(oscResult.getResult()));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    mHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Generic OSC request
     * You can also encapsulate other request interfaces yourself
     *
     * @param oscApi   OscApi, such as /osc/info, /osc/state
     * @param content  Content of RequestBody. If it is null, use GET request, otherwise use POST request
     * @param callback callback the original content returned by OSC
     */
    public void customRequest(@NonNull String oscApi, @Nullable String content, @Nullable IOscCallback callback) {
        mRequestExecutor.execute(() -> {
            try {
                if (callback != null) {
                    mHandler.post(callback::onStartRequest);
                }
                OSCResult oscResult;
                if (content == null) {
                    oscResult = mOscRequestDelegate.sendRequestByGet(getOscUrl(oscApi), getHttpHeaders());
                } else {
                    oscResult = mOscRequestDelegate.sendRequestByPost(getOscUrl(oscApi), content, getHttpHeaders());
                }
                if (oscResult.isSuccessful()) {
                    if (callback != null) {
                        mHandler.post(() -> callback.onSuccessful(oscResult.getResult()));
                    }
                } else {
                    if (callback != null) {
                        mHandler.post(() -> callback.onError(oscResult.getResult()));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    mHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Just a product demand for ONE X2
     *
     * @param callback
     */
    public void getCaptureExposureParamsForX2(@Nullable IOscCallback callback) {
        mRequestExecutor.execute(() -> {
            try {
                if (callback != null) {
                    mHandler.post(callback::onStartRequest);
                }
                // 进拍照模式并切到全景镜头
                String cmd1 = "{\"name\":\"camera.setOptions\",\"parameters\":{\"options\":{\"hdr\":\"off\",\"captureMode\":\"image\",\"_FocusSensor\":3,\"_MultiVideoMode\":\"all\"}}}";
                OSCResult oscResult1 = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd1, getHttpHeaders());
                if (oscResult1.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult1.getResult());
                    if (CMD_STATE_ERROR.equals(jsonObject.getString("state"))) {
                        if (callback != null) {
                            mHandler.post(() -> callback.onError(getErrorMessage(oscResult1.getResult())));
                        }
                        return;
                    }
                } else {
                    if (callback != null) {
                        mHandler.post(() -> callback.onError(oscResult1.getResult()));
                    }
                    return;
                }
                // 打开still参数计算的接口
                String cmd2 = "{\"name\":\"camera.setOptions\",\"parameters\":{\"options\":{\"_StillExpoCalc\":1}}}";
                OSCResult oscResult2 = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd2, getHttpHeaders());
                if (oscResult2.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult2.getResult());
                    if (CMD_STATE_ERROR.equals(jsonObject.getString("state"))) {
                        if (callback != null) {
                            mHandler.post(() -> callback.onError(getErrorMessage(oscResult2.getResult())));
                        }
                        return;
                    }
                } else {
                    if (callback != null) {
                        mHandler.post(() -> callback.onError(oscResult2.getResult()));
                    }
                    return;
                }
                // 获取3a信息
                CaptureExposureData exposureData = new CaptureExposureData();
                String cmd3 = "{\"name\":\"camera.getOptions\",\"parameters\":{\"optionNames\":[\"exposureProgram\",\"iso\",\"shutterSpeed\",\"whiteBalance\",\"_WbRGain\",\"_WbBGain\"]}}";
                OSCResult oscResult3 = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd3, getHttpHeaders());
                if (oscResult3.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult3.getResult());
                    if (CMD_STATE_ERROR.equals(jsonObject.getString("state"))) {
                        if (callback != null) {
                            mHandler.post(() -> callback.onError(getErrorMessage(oscResult3.getResult())));
                        }
                        return;
                    } else {
                        JSONObject results = jsonObject.getJSONObject("results");
                        JSONObject options = results.getJSONObject("options");
                        exposureData.exposureProgram = options.getInt("exposureProgram");
                        exposureData.iso = options.getInt("iso");
                        exposureData.shutterSpeed = options.getDouble("shutterSpeed");
                        exposureData.whiteBalance = options.getString("whiteBalance");
                        exposureData._WbBGain = options.getInt("_WbBGain");
                        exposureData._WbRGain = options.getInt("_WbRGain");
                    }
                } else {
                    if (callback != null) {
                        mHandler.post(() -> callback.onError(oscResult3.getResult()));
                    }
                    return;
                }
                // 关闭still参数计算的接口
                String cmd4 = "{\"name\":\"camera.setOptions\",\"parameters\":{\"options\":{\"_StillExpoCalc\":0}}}";
                OSCResult oscResult4 = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd4, getHttpHeaders());
                if (oscResult4.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult4.getResult());
                    if (CMD_STATE_ERROR.equals(jsonObject.getString("state"))) {
                        if (callback != null) {
                            mHandler.post(() -> callback.onError(getErrorMessage(oscResult4.getResult())));
                        }
                        return;
                    }
                } else {
                    if (callback != null) {
                        mHandler.post(() -> callback.onError(oscResult4.getResult()));
                    }
                    return;
                }
                // 返回值
                if (callback != null) {
                    mHandler.post(() -> callback.onSuccessful(exposureData));
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    mHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Just a product demand for ONE X2
     *
     * @param callback
     */
    public void takeSingleSensorPictureForX2(int sensor, @NonNull CaptureExposureData exposureData, @Nullable IOscCallback callback) {
        mRequestExecutor.execute(() -> {
            try {
                if (callback != null) {
                    mHandler.post(callback::onStartRequest);
                }
                // 进拍照模式并切到单广角镜头
                String multiVideoMode = sensor == 1 ? "front" : "rear";
                String cmd1 = "{\"name\":\"camera.setOptions\",\"parameters\":{\"options\":{\"hdr\":\"off\",\"captureMode\":\"image\",\"_FocusSensor\":" + sensor + ",\"_MultiVideoMode\":\"" + multiVideoMode + "\"}}}";
                OSCResult oscResult1 = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd1, getHttpHeaders());
                if (oscResult1.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult1.getResult());
                    if (CMD_STATE_ERROR.equals(jsonObject.getString("state"))) {
                        if (callback != null) {
                            mHandler.post(() -> callback.onError(getErrorMessage(oscResult1.getResult())));
                        }
                        return;
                    }
                } else {
                    if (callback != null) {
                        mHandler.post(() -> callback.onError(oscResult1.getResult()));
                    }
                    return;
                }
                // 设置3a信息
                String cmd2 = "{\"name\":\"camera.setOptions\",\"parameters\":{\"options\":{\"exposureProgram\":"
                        + exposureData.exposureProgram + ",\"iso\":" + exposureData.iso + ",\"shutterSpeed\":"
                        + exposureData.shutterSpeed + ",\"whiteBalance\":\"" + exposureData.whiteBalance + "\",\"_WbRGain\":"
                        + exposureData._WbRGain + ",\"_WbBGain\":" + exposureData._WbBGain + "}}}";
                OSCResult oscResult2 = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd2, getHttpHeaders());
                if (oscResult2.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult2.getResult());
                    if (CMD_STATE_ERROR.equals(jsonObject.getString("state"))) {
                        if (callback != null) {
                            mHandler.post(() -> callback.onError(getErrorMessage(oscResult2.getResult())));
                        }
                        return;
                    }
                } else {
                    if (callback != null) {
                        mHandler.post(() -> callback.onError(oscResult2.getResult()));
                    }
                    return;
                }
                // TakePicture
                String cmdId;
                String cmd3 = "{\"name\":\"camera.takePicture\"}";
                OSCResult oscResult3 = mOscRequestDelegate.sendRequestByPost(getOscCmdExecuteUrl(), cmd3, getHttpHeaders());
                if (oscResult3.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(oscResult3.getResult());
                    if (CMD_STATE_IN_PROGRESS.equals(jsonObject.getString("state"))) {
                        cmdId = jsonObject.getString("id");
                    } else {
                        if (callback != null) {
                            String errorMsg = getErrorMessage(oscResult3.getResult());
                            mHandler.post(() -> callback.onError(errorMsg));
                        }
                        return;
                    }
                } else {
                    if (callback != null) {
                        String errorMsg = oscResult3.getResult();
                        mHandler.post(() -> callback.onError(errorMsg));
                    }
                    return;
                }
                // QueryResult
                if (!TextUtils.isEmpty(cmdId)) {
                    cmd3 = "{\"id\":\"" + cmdId + "\"}";
                    for (int i = 0; i < 60; i++) {
                        oscResult3 = mOscRequestDelegate.sendRequestByPost(getOscCmdStatusUrl(), cmd3, getHttpHeaders());
                        if (oscResult3.isSuccessful()) {
                            JSONObject jsonObject = new JSONObject(oscResult3.getResult());
                            if (CMD_STATE_DONE.equals(jsonObject.getString("state"))) {
                                if (callback != null) {
                                    // parse file address from result
                                    JSONObject results = jsonObject.getJSONObject("results");
                                    String path = null;
                                    if (results.has("_fileGroup")) {
                                        path = results.getString("_fileGroup");
                                    }
                                    if (TextUtils.isEmpty(path) || path.equals("[]")) {
                                        path = results.getString("fileUrl");
                                    }
                                    path = path.replaceAll("\\[", "").replaceAll("]", "").replaceAll("\\\\/", "/");
                                    String[] paths = path.split(",");
                                    for (int j = 0; j < paths.length; j++) {
                                        paths[j] = paths[j].replaceAll("\"", "");
                                    }
                                    mHandler.post(() -> callback.onSuccessful(paths));
                                }
                                return;
                            }
                        }
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException exception) {
                            exception.printStackTrace();
                        }
                    }
                    if (callback != null) {
                        mHandler.post(() -> callback.onError("Timeout. Please use command \"listFiles\" to get."));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    mHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    private String getOscUrl(String oscApi) {
        return InstaCameraManager.getInstance().getCameraHttpPrefix() + oscApi;
    }

    private String getOscCmdExecuteUrl() {
        return getOscUrl("/osc/commands/execute");
    }

    private String getOscCmdStatusUrl() {
        return getOscUrl("/osc/commands/status");
    }

    private Map<String, String> getHttpHeaders() {
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Content-Type", "application/json; charset= utf-8");
        headerMap.put("Accept", "application/json");
        headerMap.put("X-XSRF-Protected", "1");
        return headerMap;
    }

    private String getErrorMessage(String result) {
        try {
            JSONObject jsonObject = new JSONObject(result);
            if (jsonObject.has("error")) {
                JSONObject error = jsonObject.getJSONObject("error");
                String code = error.has("code") ? error.getString("code") + ". " : "";
                String message = error.has("message") ? error.getString("message") + "." : "";
                return code + message;
            }
        } catch (Exception ignore) {
        }
        return result;
    }
}
