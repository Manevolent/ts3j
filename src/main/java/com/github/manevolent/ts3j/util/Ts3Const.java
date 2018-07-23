package com.github.manevolent.ts3j.util;

public final class Ts3Const {
    // Common Definitions

    //limited length, measured in characters
    public static int MaxSizeChannelName = 40;
    public static int MaxSizeVirtualserverName = 64;
    public static int MaxSizeClientNicknameSdk = 64;
    public static int MinSizeClientNicknameSdk = 3;
    public static int MaxSizeReasonMessage = 80;

    //limited length, measured in bytes (utf8 encoded)
    public static int MaxSizeTextMessage = 1024;
    public static int MaxSizeChannelTopic = 255;
    public static int MaxSizeChannelDescription = 8192;
    public static int MaxSizeVirtualserverWelcomeMessage = 1024;

    // Rare Definitions

    //limited length, measured in characters
    public static int MaxSizeClientNickname = 30;
    public static int MinSizeClientNickname = 3;
    public static int MaxSizeAwayMessage = 80;
    public static int MaxSizeGroupName = 30;
    public static int MaxSizeTalkRequestMessage = 50;
    public static int MaxSizeComplainMessage = 200;
    public static int MaxSizeClientDescription = 200;
    public static int MaxSizeHostMessage = 200;
    public static int MaxSizeHostbuttonTooltip = 50;
    public static int MaxSizePokeMessage = 100;
    public static int MaxSizeOfflineMessage = 4096;
    public static int MaxSizeOfflineMessageSubject = 200;

    //limited length, measured in bytes (utf8 encoded)
    public static int MaxSizePluginCommand = 1024 * 8;
    public static int MaxSizeVirtualserverHostbannerGfxUrl = 2000;
}