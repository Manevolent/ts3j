package com.github.manevolent.ts3j.event;

public interface TeamspeakListener {

    void onTextMessage(TextMessageEvent e);

    void onClientJoin(ClientJoinEvent e);

    void onClientLeave(ClientLeaveEvent e);

    void onServerEdit(ServerEditedEvent e);

    void onChannelEdit(ChannelEditedEvent e);

    void onChannelDescriptionChanged(ChannelDescriptionEditedEvent e);

    void onClientMoved(ClientMovedEvent e);

    void onChannelCreate(ChannelCreateEvent e);

    void onChannelDeleted(ChannelDeletedEvent e);

    void onChannelMoved(ChannelMovedEvent e);

    void onChannelPasswordChanged(ChannelPasswordChangedEvent e);

    void onPrivilegeKeyUsed(PrivilegeKeyUsedEvent e);

}
