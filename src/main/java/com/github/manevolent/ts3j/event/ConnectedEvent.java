package com.github.manevolent.ts3j.event;

import java.util.Map;

public class ConnectedEvent extends BaseEvent {
    public ConnectedEvent(Map<String, String> map) {
        super(map);
    }

    @Override
    public void fire(TS3Listener listener) {
        listener.onConnected(this);
    }
}
