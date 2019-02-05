package com.github.manevolent.ts3j.event;

/*
 * #%L
 * TeamSpeak 3 Java API
 * %%
 * Copyright (C) 2014 Bert De Geyter
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import com.github.manevolent.ts3j.command.Command;

public interface TS3Event {
	void fire(TS3Listener listener);

	static TS3Event createEvent(Command command) {
		switch (command.getName()) {
			case "notifytextmessage":
				return new TextMessageEvent(command.toMap());
			case "notifycliententerview":
				return new ClientJoinEvent(command.toMap());
			case "notifyclientleftview":
				return new ClientLeaveEvent(command.toMap());
			case "notifyserveredited":
				return new ServerEditedEvent(command.toMap());
			case "notifychanneledited":
				return new ChannelEditedEvent(command.toMap());
			case "notifychanneldescriptionchanged":
				return new ChannelDescriptionEditedEvent(command.toMap());
			case "notifyclientmoved":
				return new ClientMovedEvent(command.toMap());
			case "notifychannelcreated":
				return new ChannelCreateEvent(command.toMap());
			case "notifychanneldeleted":
				return new ChannelDeletedEvent(command.toMap());
			case "notifychannelmoved":
				return new ChannelMovedEvent(command.toMap());
			case "notifychannelpasswordchanged":
				return new ChannelPasswordChangedEvent(command.toMap());
			case "notifytokenused":
				return new PrivilegeKeyUsedEvent(command.toMap());
			case "notifyservergrouplist": // CLIENT
				return new ServerGroupListEvent(command.toMap());
			case "notifychannelgrouplist": // CLIENT
				return new ChannelGroupListEvent(command.toMap());
			case "notifyclientchannelgroupchanged": // CLIENT
				return new ClientChannelGroupChangedEvent(command.toMap());
			case "notifyclientneededpermissions": // CLIENT
				return new ClientNeededPermissionsEvent(command.toMap());
			case "notifyclientupdated": // CLIENT
				return new ClientUpdatedEvent(command.toMap());
            case "notifychannelsubscribed": // CLIENT
                return new ChannelSubscribedEvent(command.toMap());
            case "notifychannelunsubscribed": // CLIENT
                return new ChannelUnsubscribedEvent(command.toMap());
            case "notifyservergroupclientadded": // CLIENT
                return new ServerGroupClientAddedEvent(command.toMap());
            case "notifyservergroupclientdeleted": // CLIENT
                return new ServerGroupClientDeletedEvent(command.toMap());
            case "notifyclientpoke": // CLIENT
                return new ClientPokeEvent(command.toMap());
            case "notifyclientchatcomposing": // CLIENT
                return new ClientChatComposingEvent(command.toMap());
			case "notifyclientchatclosed": // CLIENT
				return new ClientChatClosedEvent(command.toMap());
			case "notifypermissionlist": // CLIENT
				return new PermissionListEvent(command.toMap());
			default:
				throw new IllegalArgumentException("unknown event: " + command.getName());
		}
	}
}
