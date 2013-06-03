package com.littleinc.MessageMe.bo;

public enum DeviceType {
    IOS(0), ANDROID(1);

    private int value;

    private DeviceType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}