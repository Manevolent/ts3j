package com.github.manevolent.ts3j.event;

import java.util.Map;

public class UnknownTeamspeakEvent extends BaseEvent {
    String command;

    public UnknownTeamspeakEvent(String command, Map<String, String> map) {
        super(map);
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public void fire(TS3Listener listener) {
        listener.onUnknownEvent(this);
    }
}
