package com.github.manevolent.ts3j.event;

import com.github.manevolent.ts3j.api.ClientProperty;

import java.util.Map;

public class ClientChatClosedEvent extends BaseEvent {
	public ClientChatClosedEvent(Map<String, String> map) {
        super(map);
	}

	@Override
	public void fire(TS3Listener listener) {
		listener.onClientChatClosed(this);
	}

	public int getClientId() {
		return getInt("clid");
	}

	public String getUniqueClientIdentifier() {
		return get("cluid");
	}
}