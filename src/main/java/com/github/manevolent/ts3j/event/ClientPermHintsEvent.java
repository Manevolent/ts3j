package com.github.manevolent.ts3j.event;

import java.util.*;

public class ClientPermHintsEvent extends BaseEvent {
    public ClientPermHintsEvent(Map<String, String> map) {
	super(map);
    }

    @Override
    public void fire(TS3Listener listener) {
	listener.onClientPermHints(this);
    }

    public int getClientId() {
	return getInt("clid");
    }

    public int getPermId() {
	return getInt("permid");
    }

    public int getPermValue() {
	return getInt("permvalue");
    }
}