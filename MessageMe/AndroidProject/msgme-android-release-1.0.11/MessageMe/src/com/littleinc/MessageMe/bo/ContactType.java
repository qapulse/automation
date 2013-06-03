package com.littleinc.MessageMe.bo;

public enum ContactType {
    USER(0), GROUP(1);

    private int value;

    private ContactType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}