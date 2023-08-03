package com.arashivision.sdk.demo.osc;

public class OSCResult {

    private boolean isSuccessful;
    private String result; // Success is response.body().string()，Failure is response.message()

    public OSCResult(boolean isSuccessful, String result) {
        this.isSuccessful = isSuccessful;
        this.result = result;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public String getResult() {
        return result;
    }

}
