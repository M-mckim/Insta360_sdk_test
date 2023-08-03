package com.arashivision.sdk.demo.osc.delegate;

import com.arashivision.sdk.demo.osc.OSCResult;

import java.util.Map;

public interface IOscRequestDelegate {

    /**
     * Send a Http network request by Get
     *
     * @param url       Request address
     * @param headerMap HTTP request headers to use
     * @return Network Request Response Body or Error Message
     */
    OSCResult sendRequestByGet(String url, Map<String, String> headerMap);

    /**
     * Send a Http network request by Post
     *
     * @param url       Request address
     * @param content   osc command content
     * @param headerMap HTTP request headers to use
     * @return Network Request Response Body or Error Message
     */
    OSCResult sendRequestByPost(String url, String content, Map<String, String> headerMap);

}
