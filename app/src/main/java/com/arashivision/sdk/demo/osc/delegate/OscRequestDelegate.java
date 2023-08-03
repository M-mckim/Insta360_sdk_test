package com.arashivision.sdk.demo.osc.delegate;

import com.arashivision.sdk.demo.osc.OSCResult;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.model.HttpHeaders;

import java.io.IOException;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * This demo use OkGo to implements Http Request
 * https://github.com/jeasonlzy/okhttp-OkGo
 */
public class OscRequestDelegate implements IOscRequestDelegate {

    @Override
    public OSCResult sendRequestByGet(String url, Map<String, String> headerMap) {
        try {
            Response response = OkGo.get(url)
                    .headers(getHttpHeaders(headerMap))
                    .execute();
            if (response.isSuccessful()) {
                return new OSCResult(true, response.body().string());
            } else {
                return new OSCResult(false, response.message());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new OSCResult(false, e.getMessage());
        }
    }

    @Override
    public OSCResult sendRequestByPost(String url, String content, Map<String, String> headerMap) {
        try {
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody requestBody = RequestBody.create(mediaType, content);
            Response response = OkGo.post(url)
                    .headers(getHttpHeaders(headerMap))
                    .upRequestBody(requestBody)
                    .execute();
            if (response.isSuccessful()) {
                return new OSCResult(true, response.body().string());
            } else {
                return new OSCResult(false, response.message());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new OSCResult(false, e.getMessage());
        }
    }

    private HttpHeaders getHttpHeaders(Map<String, String> headerMap) {
        HttpHeaders httpHeaders = new HttpHeaders();
        for (String name : headerMap.keySet()) {
            String value = headerMap.get(name);
            httpHeaders.put(name, value);
        }
        return httpHeaders;
    }

}
