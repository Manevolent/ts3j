package com.github.manevolent.ts3j.util;

public final class Ts3String {
    public static String escape(String string) {
        StringBuilder sb = new StringBuilder(string.length());

        for (int i = 0; i < string.length(); i ++) {
            switch (string.charAt(i)) {
                case '\\': sb.append("\\\\"); break;
                case '/': sb.append("\\/"); break;
                case ' ': sb.append("\\s"); break;
                case '|': sb.append("\\p"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                //case '\v': sb.append("\\v"); break;
                default:
                    sb.append(string.charAt(i));
            }
        }

        return sb.toString();
    }

    public static String unescape(String string) {
        StringBuilder sb = new StringBuilder(string.length());

        for (int i = 0; i < string.length(); i ++) {
            char c = string.charAt(i);
            if (c == '\\') {
                if (i++ >= string.length()) throw new IllegalArgumentException("illegal escape at end of string");
                switch (string.charAt(i)) {
                    //case 'v': sb.append('\v'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'n': sb.append('\n'); break;
                    case 'f': sb.append('\f'); break;
                    case 'p': sb.append('|'); break;
                    case 's': sb.append(' '); break;
                    case '/': sb.append('/'); break;
                    case '\\': sb.append('\\'); break;
                    default:
                        throw new IllegalArgumentException("invalid escape code: " + string.charAt(i) + " (" + Character.getName(string.charAt(i)) + ")");
                }
            } else sb.append(c);
        }

        return sb.toString();
    }

    public static int getTokenLength(String s) {
        return (int) s.length() + (int) s.chars().filter(x -> isDoubleChar((char)x)).count();
    }

    public static boolean isDoubleChar(char c) {
        return c == '\\' ||
                c == '/' ||
                c == ' ' ||
                c == '|' ||
                c == '\f' ||
                c == '\n' ||
                c == '\r' ||
                c == '\t';// ||
                //c == '\v';
    }
}
