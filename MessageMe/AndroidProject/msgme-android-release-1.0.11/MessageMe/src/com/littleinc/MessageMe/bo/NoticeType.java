package com.littleinc.MessageMe.bo;

/**
 * Enum class to identify the
 * different types to room messages
 *
 */
public enum NoticeType {

    ROOM_NEW(0, 1), ROOM_JOIN(1, 2), ROOM_LEAVE(2, 3), ROOM_UPDATE(3, 4), USER_JOIN(
            4, 5);

    private final int index;

    private final int value;

    public final int getIndex() {
        return index;
    }

    public final int getNumber() {
        return value;
    }

    NoticeType(int index, int value) {
        this.index = index;
        this.value = value;
    }
}