package com.github.manevolent.ts3j.event;

import java.util.Map;

public class ClientPokeEvent extends BaseEvent {
	public ClientPokeEvent(Map<String, String> map) {
        super(map);
	}

    public String getMessage() {
        return get("msg");
    }

	@Override
	public void fire(TS3Listener listener) {
		listener.onClientPoke(this);
	}
}