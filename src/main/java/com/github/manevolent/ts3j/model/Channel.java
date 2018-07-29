package com.github.manevolent.ts3j.model;

import com.github.manevolent.ts3j.enums.CodecType;

public class Channel {
    private final VirtualServer server;
    private final int channelId;
    private String name;
    private CodecType codec;
    private int codecQuality;
    private int maxClients;
    private int maxFamilyClients;
    private int order;
    private boolean permanent;
    private boolean semiPermanent;
    private boolean serverDefault;
    private boolean hasPassword;
    private boolean codecLatencyFactor;
    private boolean unencrypted;
    private int deleteDelay;
    private boolean unlimited;
    private boolean familyUnlimited;
    private boolean inheritingMaxfamilyUnlimited;
    private int neededTalkPower;
    private boolean forcingSilence;
    private int iconId;
    private boolean privated;

    public Channel(VirtualServer server, int channelId) {
        this.server = server;
        this.channelId = channelId;
    }

    public int getChannelId() {
        return channelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CodecType getCodec() {
        return codec;
    }

    public void setCodec(CodecType codec) {
        this.codec = codec;
    }

    public int getCodecQuality() {
        return codecQuality;
    }

    public void setCodecQuality(int codecQuality) {
        this.codecQuality = codecQuality;
    }

    public int getMaxClients() {
        return maxClients;
    }

    public void setMaxClients(int maxClients) {
        this.maxClients = maxClients;
    }

    public int getMaxFamilyClients() {
        return maxFamilyClients;
    }

    public void setMaxFamilyClients(int maxFamilyClients) {
        this.maxFamilyClients = maxFamilyClients;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isPermanent() {
        return permanent;
    }

    public void setPermanent(boolean permanent) {
        this.permanent = permanent;
    }

    public boolean isSemiPermanent() {
        return semiPermanent;
    }

    public void setSemiPermanent(boolean semiPermanent) {
        this.semiPermanent = semiPermanent;
    }

    public boolean isServerDefault() {
        return serverDefault;
    }

    public void setServerDefault(boolean serverDefault) {
        this.serverDefault = serverDefault;
    }

    public boolean hasPassword() {
        return hasPassword;
    }

    public void setHasPassword(boolean hasPassword) {
        this.hasPassword = hasPassword;
    }

    public boolean isCodecLatencyFactor() {
        return codecLatencyFactor;
    }

    public void setCodecLatencyFactor(boolean codecLatencyFactor) {
        this.codecLatencyFactor = codecLatencyFactor;
    }

    public boolean isUnencrypted() {
        return unencrypted;
    }

    public void setUnencrypted(boolean unencrypted) {
        this.unencrypted = unencrypted;
    }

    public int getDeleteDelay() {
        return deleteDelay;
    }

    public void setDeleteDelay(int deleteDelay) {
        this.deleteDelay = deleteDelay;
    }

    public boolean isUnlimited() {
        return unlimited;
    }

    public void setUnlimited(boolean unlimited) {
        this.unlimited = unlimited;
    }

    public boolean isFamilyUnlimited() {
        return familyUnlimited;
    }

    public void setFamilyUnlimited(boolean familyUnlimited) {
        this.familyUnlimited = familyUnlimited;
    }

    public boolean isInheritingMaxfamilyUnlimited() {
        return inheritingMaxfamilyUnlimited;
    }

    public void setInheritingMaxfamilyUnlimited(boolean inheritingMaxfamilyUnlimited) {
        this.inheritingMaxfamilyUnlimited = inheritingMaxfamilyUnlimited;
    }

    public int getNeededTalkPower() {
        return neededTalkPower;
    }

    public void setNeededTalkPower(int neededTalkPower) {
        this.neededTalkPower = neededTalkPower;
    }

    public boolean isForcingSilence() {
        return forcingSilence;
    }

    public void setForcingSilence(boolean forcingSilence) {
        this.forcingSilence = forcingSilence;
    }

    public int getIconId() {
        return iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public boolean isPrivated() {
        return privated;
    }

    public void setPrivated(boolean privated) {
        this.privated = privated;
    }

    public VirtualServer getServer() {
        return server;
    }
}
