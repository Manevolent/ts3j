package com.github.manevolent.ts3j.enums;

public enum DisconnectReason {
    USER_ACTION(0),
    USER_OR_CHANNEL_MOVED(1),
    SUBSCRIPTION_CHANGED(2),
    TIMEOUT(3),
    KICKED_FROM_CHANNEL(4),
    KICKED_FROM_SERVER(5),
    BANNED(6),
    SERVER_STOPPED(7),
    LEFT_SERVER(8),
    CHANNEL_UPDATED(9),
    SERVER_OR_CHANNEL_EDITED(10),
    SERVER_SHUTDOWN(11)

    ;

    private final int index;

    DisconnectReason(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static DisconnectReason fromId(int index) {
        for (DisconnectReason value : values())
            if (value.getIndex() == index) return value;

        throw new IllegalArgumentException("invalid index: " + index);
    }
}
