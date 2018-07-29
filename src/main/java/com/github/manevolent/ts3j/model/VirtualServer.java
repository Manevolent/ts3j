package com.github.manevolent.ts3j.model;

import com.github.manevolent.ts3j.enums.HostBannerMode;
import com.github.manevolent.ts3j.enums.HostMessageMode;
import com.github.manevolent.ts3j.enums.VoiceEncryptionMode;
import com.github.manevolent.ts3j.identity.Uid;

import java.util.*;
import java.util.stream.Collectors;

public class VirtualServer {
    private int id;
    private String name;
    private String nickname;
    private String welcomeMessage;
    private String platform;
    private String version;
    private int maxClients;
    private long  created;
    private VoiceEncryptionMode encryptionMode;
    private HostMessageMode hostMessageMode;
    private int defaultGroupId;
    private int defaultChannelGroupId;
    private String hostbannerUrl;
    private int hostBannerInterval;
    private double prioritySpeakerDimmModifier;
    private String namePhonetic;
    private long iconId;
    private String serverIp;
    private boolean askForPriviledgeKey;
    private HostBannerMode hostBannerMode;
    private int defaultTempChannelDeleteDelay;

    /**
     * Connected clients
     */
    private final Map<Integer, Client> clients = new HashMap<>();

    /**
     * Server channels
     */
    private final Map<Integer, Channel> channels = new HashMap<>();

    public VirtualServer() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getMaxClients() {
        return maxClients;
    }

    public void setMaxClients(int maxClients) {
        this.maxClients = maxClients;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public VoiceEncryptionMode getEncryptionMode() {
        return encryptionMode;
    }

    public void setEncryptionMode(VoiceEncryptionMode encryptionMode) {
        this.encryptionMode = encryptionMode;
    }

    public HostMessageMode getHostMessageMode() {
        return hostMessageMode;
    }

    public void setHostMessageMode(HostMessageMode hostMessageMode) {
        this.hostMessageMode = hostMessageMode;
    }

    /**
     * Gets the default server group ID users are assigned to when connecting to the server for the first time.
     * @return default server group ID.
     */
    public int getDefaultGroupId() {
        return defaultGroupId;
    }

    public void setDefaultGroupId(int defaultGroupId) {
        this.defaultGroupId = defaultGroupId;
    }

    public int getDefaultChannelGroupId() {
        return defaultChannelGroupId;
    }

    public void setDefaultChannelGroupId(int defaultChannelGroupId) {
        this.defaultChannelGroupId = defaultChannelGroupId;
    }

    public String getHostbannerUrl() {
        return hostbannerUrl;
    }

    public void setHostbannerUrl(String hostbannerUrl) {
        this.hostbannerUrl = hostbannerUrl;
    }

    public int getHostBannerInterval() {
        return hostBannerInterval;
    }

    public void setHostBannerInterval(int hostBannerInterval) {
        this.hostBannerInterval = hostBannerInterval;
    }

    public double getPrioritySpeakerDimmModifier() {
        return prioritySpeakerDimmModifier;
    }

    public void setPrioritySpeakerDimmModifier(double prioritySpeakerDimmModifier) {
        this.prioritySpeakerDimmModifier = prioritySpeakerDimmModifier;
    }

    public String getNamePhonetic() {
        return namePhonetic;
    }

    public void setNamePhonetic(String namePhonetic) {
        this.namePhonetic = namePhonetic;
    }

    public long getIconId() {
        return iconId;
    }

    public void setIconId(long iconId) {
        this.iconId = iconId;
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public boolean isAskForPriviledgeKey() {
        return askForPriviledgeKey;
    }

    public void setAskForPriviledgeKey(boolean askForPriviledgeKey) {
        this.askForPriviledgeKey = askForPriviledgeKey;
    }

    public HostBannerMode getHostBannerMode() {
        return hostBannerMode;
    }

    public void setHostBannerMode(HostBannerMode hostBannerMode) {
        this.hostBannerMode = hostBannerMode;
    }

    public int getDefaultTempChannelDeleteDelay() {
        return defaultTempChannelDeleteDelay;
    }

    public void setDefaultTempChannelDeleteDelay(int defaultTempChannelDeleteDelay) {
        this.defaultTempChannelDeleteDelay = defaultTempChannelDeleteDelay;
    }

    /**
     * Gets a channel by its numeric ID.
     * @param id channel ID.
     * @return the corresponding channel, if such a channel exists, null otherwise.
     */
    public Channel getChannelById(int id) {
        return channels.get(id);
    }

    /**
     * Tries to add a new channel to the server.  If the channel ID is already in use, IllegalStateException is thrown.
     * @param channel channel to add.
     */
    public void updateChannel(Channel channel) {
        synchronized (channels) {
            this.channels.put(channel.getChannelId(), channel);
        }
    }

    /**
     * Gets all channels by a specific name.
     * @param name Name to search for.
     * @return List of channels with the specified name.
     */
    public List<Channel> getChannelsByName(String name) {
        return this.channels.values()
                .stream()
                .filter(x -> x.getName().equalsIgnoreCase(name))
                .collect(Collectors.toList());
    }

    /**
     * Gets the first channel by a specific name.
     * @param name Name to search for.
     * @return List of channels with the specified name.
     */
    public Channel getChannelByName(String name) throws IllegalArgumentException {
        return getChannelsOrderd()
                .stream()
                .filter(x -> x.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("channel not found"));
    }

    /**
     * Gets a real channel list, potentially unordered.
     * @return Unordered channel list.
     */
    public Collection<Channel> getChannels() {
        return Collections.unmodifiableCollection(channels.values());
    }

    /**
     * Gets the channel list as ordered by the server.
     * @return Iterable list of ordered channels.
     */
    public Collection<Channel> getChannelsOrderd() {
        return Collections.unmodifiableCollection(channels.values()
                .stream()
                .sorted(Comparator.comparingInt(Channel::getOrder))
                .collect(Collectors.toList())
        );
    }

    /**
     * Gets a list of known clients to the server.
     * @return clients
     */
    public Collection<Client> getClients() {
        throw new UnsupportedOperationException(); // TODO
    }

    /**
     * Gets a list of connected clients.
     * @return connected clients.
     */
    public Collection<Client> getConnectedClients() {
        return Collections.unmodifiableCollection(clients.values());
    }

    public List<Client> getClientsByUid(Uid uid) {
        return getConnectedClients().stream().filter(x -> x.getUid().equals(uid)).collect(Collectors.toList());
    }

    public void updateConnectedClient(Client client) {
        synchronized (clients) {
            this.clients.put(client.getId(), client);
        }
    }

    public void removeConnectedClient(Client client) {
        synchronized (clients) {
            if (this.clients.containsKey(client.getId()))
                this.clients.remove(client.getId());
            else
                throw new IllegalStateException("client ID doesn't exist");
        }
    }

    public Client getClientById(int clientId) {
        return this.clients.get(clientId);
    }
}
