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

public interface TS3Listener {
	default void onTextMessage(TextMessageEvent e) {}
    default void onClientJoin(ClientJoinEvent e) {}
    default void onClientLeave(ClientLeaveEvent e) {}
    default void onServerEdit(ServerEditedEvent e) {}
    default void onChannelEdit(ChannelEditedEvent e) {}
	default void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e) {}
    default void onClientMoved(ClientMovedEvent e) {}
    default void onChannelCreate(ChannelCreateEvent e) {}
    default void onChannelDeleted(ChannelDeletedEvent e) {}
    default void onChannelMoved(ChannelMovedEvent e) {}
    default void onChannelPasswordChanged(ChannelPasswordChangedEvent e) {}
    default void onChannelList(ChannelListEvent e) {}
    default void onPrivilegeKeyUsed(PrivilegeKeyUsedEvent e) {}
    default void onChannelGroupList(ChannelGroupListEvent e) {}
    default void onServerGroupList(ServerGroupListEvent e) {}
    default void onClientNeededPermissions(ClientNeededPermissionsEvent e) {}
    default void onClientChannelGroupChanged(ClientChannelGroupChangedEvent e) {}
    default void onClientChanged(ClientUpdatedEvent e) {}
    default void onDisconnected(DisconnectedEvent e) {}
    default void onChannelSubscribed(ChannelSubscribedEvent e) {}
    default void onChannelUnsubscribed(ChannelUnsubscribedEvent e) {}
    default void onServerGroupClientAdded(ServerGroupClientAddedEvent e) {}
    default void onServerGroupClientDeleted(ServerGroupClientDeletedEvent e) {}
    default void onClientPoke(ClientPokeEvent e) {}
    default void onClientComposing(ClientChatComposingEvent e) {}
}
