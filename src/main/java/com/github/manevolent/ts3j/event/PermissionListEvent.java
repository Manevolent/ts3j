package com.github.manevolent.ts3j.event;

import java.util.Map;

public class PermissionListEvent extends BaseEvent {
    public PermissionListEvent(Map<String, String> map) {
        super(map);
    }

    @Override
    public void fire(TS3Listener listener) {
        listener.onPermissionList(this);
    }
}
