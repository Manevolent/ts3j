package com.github.manevolent.ts3j.util;

public final class Ts3Debugging {
    private static boolean enabled;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        Ts3Debugging.enabled = enabled;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static final String HEXES  = "0123456789ABCDEF";
    public static String getHex(byte[] raw) {
        return getHex(raw, raw.length);
    }
    public static String getHex(byte[] raw, int len) {
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (int x = 0; x < len; x ++) {
            byte b = raw[x];
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }

    public static void info(String message) {
        if (isEnabled()) {
            System.err.println("[INFO] " + message);
        }
    }

    public static void debug(Object message) {
        if (isEnabled()) System.err.println("[DEBUG] " + message.toString());
    }

    public static void debug(byte[] message) {
        if (isEnabled()) debug(getHex(message));
    }

    public static void debug(String message, Throwable ex) {
        if (isEnabled()) {
            System.err.println("[DEBUG] " + message);
            ex.printStackTrace();
        }
    }
}
