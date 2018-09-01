package com.github.manevolent.ts3j.event;

import java.util.Map;

public class ClientChatComposingEvent extends BaseEvent {
	public ClientChatComposingEvent(Map<String, String> map) {
        super(map);
	}

	@Override
	public void fire(TS3Listener listener) {
		listener.onClientComposing(this);
	}
}