package com.github.manevolent.ts3j.model;

import com.github.manevolent.ts3j.enums.ClientType;
import com.github.manevolent.ts3j.identity.Uid;

import java.util.*;

public class Client {
    private final int id;
    private final int databaseId;
    private final VirtualServer server;
    private final Uid uid;
    private final ClientType clientType;

    private String nickname;

    private int channelId;
    private int channelGroupId;

    private boolean speaking;
    private boolean outputMuted, inputMuted;
    private boolean recording;
    private boolean inputHardware, outputHardware;

    private boolean away;
    private int talkPower;
    private boolean talkRequested;
    private boolean talker;
    private boolean prioritySpeaker;
    private int unreadMessages;
    private int neededServerQueryViewPower;
    private int iconId;
    private boolean channelCommander;
    private String country;
    private int inheritedChannelGroupChannelId;

    private List<Integer> serverGroupIds = new ArrayList<>();

    public Client(int id, int databaseId, VirtualServer server, Uid uid, ClientType clientType) {
        this.id = id;
        this.databaseId = databaseId;
        this.server = server;
        this.uid = uid;
        this.clientType = clientType;
    }

    public VirtualServer getServer() {
        return server;
    }

    public Uid getUid() {
        return uid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * Gets the client's current client connection ID.
     * @return client ID.
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the current connected-to channel.
     * @return channel currently connected to by the client.
     */
    public Channel getChannel() {
        return server.getChannelById(getChannelId());
    }

    /**
     * Gets the channel ID currently connected to.
     * @return channel currently connected to by the client.
     */
    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public boolean isSpeaking() {
        return speaking;
    }

    public void setSpeaking(boolean speaking) {
        this.speaking = speaking;
    }

    public boolean isInputMuted() {
        return inputMuted;
    }

    public void setInputMuted(boolean inputMuted) {
        this.inputMuted = inputMuted;
    }

    public boolean isOutputMuted() {
        return outputMuted;
    }

    public void setOutputMuted(boolean outputMuted) {
        this.outputMuted = outputMuted;
    }

    public boolean hasOutputHardware() {
        return outputHardware;
    }

    public void setOutputHardware(boolean outputHardware) {
        this.outputHardware = outputHardware;
    }

    public boolean hasInputHardware() {
        return inputHardware;
    }

    public void setInputHardware(boolean inputHardware) {
        this.inputHardware = inputHardware;
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public boolean isRecording() {
        return recording;
    }

    public void setRecording(boolean recording) {
        this.recording = recording;
    }

    public boolean isInServerGroup(int serverGroupId) {
        return serverGroupIds.contains(serverGroupId);
    }

    public void setServerGroupIds(Collection<Integer> serverGroupIds) {
        synchronized (serverGroupIds) {
            this.serverGroupIds.clear();
            this.serverGroupIds.addAll(serverGroupIds);
        }
    }

    public void setServerGroupIds(Integer... serverGroupIds) {
        setServerGroupIds(Arrays.asList(serverGroupIds));
    }

    public Iterable<Integer> getServerGroupIds() {
        return Collections.unmodifiableList(serverGroupIds);
    }

    public int getChannelGroupId() {
        return channelGroupId;
    }

    public void setChannelGroupId(int channelGroupId) {
        this.channelGroupId = channelGroupId;
    }

    public boolean isAway() {
        return away;
    }

    public void setAway(boolean away) {
        this.away = away;
    }

    public int getTalkPower() {
        return talkPower;
    }

    public void setTalkPower(int talkPower) {
        this.talkPower = talkPower;
    }

    public boolean isTalkRequested() {
        return talkRequested;
    }

    public void setTalkRequested(boolean talkRequested) {
        this.talkRequested = talkRequested;
    }

    public boolean isTalker() {
        return talker;
    }

    public void setTalker(boolean talker) {
        this.talker = talker;
    }

    public boolean isPrioritySpeaker() {
        return prioritySpeaker;
    }

    public void setPrioritySpeaker(boolean prioritySpeaker) {
        this.prioritySpeaker = prioritySpeaker;
    }

    public int getUnreadMessages() {
        return unreadMessages;
    }

    public void setUnreadMessages(int unreadMessages) {
        this.unreadMessages = unreadMessages;
    }

    public int getNeededServerQueryViewPower() {
        return neededServerQueryViewPower;
    }

    public void setNeededServerQueryViewPower(int neededServerQueryViewPower) {
        this.neededServerQueryViewPower = neededServerQueryViewPower;
    }

    public int getIconId() {
        return iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public boolean isChannelCommander() {
        return channelCommander;
    }

    public void setChannelCommander(boolean channelCommander) {
        this.channelCommander = channelCommander;
    }

    public int getInheritedChannelGroupChannelId() {
        return inheritedChannelGroupChannelId;
    }

    public void setInheritedChannelGroupChannelId(int inheritedChannelGroupChannelId) {
        this.inheritedChannelGroupChannelId = inheritedChannelGroupChannelId;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
