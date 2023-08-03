package com.arashivision.sdk.demo.model;

public class CaptureExposureData {
    public int exposureProgram;
    public int iso;
    public double shutterSpeed;
    public String whiteBalance;
    public int _WbRGain;
    public int _WbBGain;

    @Override
    public String toString() {
        return "{" +
                "exposureProgram=" + exposureProgram +
                ", iso=" + iso +
                ", shutterSpeed=" + shutterSpeed +
                ", whiteBalance='" + whiteBalance + '\'' +
                ", _WbRGain=" + _WbRGain +
                ", _WbBGain=" + _WbBGain +
                '}';
    }
}
