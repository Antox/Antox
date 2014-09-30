package im.tox.antox.utils;

import java.util.ArrayList;

/**
 * Used for storing either the downloaded node details or the details of a node the user has entered
 * themselves in SettingsFragment
 */
public final class DhtNodes {
    public static ArrayList<String> ipv4 = new ArrayList<String>();
    public static ArrayList<String> ipv6 = new ArrayList<String>();
    public static ArrayList<String> port = new ArrayList<String>();
    public static ArrayList<String> key = new ArrayList<String>();
    public static ArrayList<String> owner = new ArrayList<String>();
    public static int counter = 0;
    public static boolean connected = false;
    public static boolean sorted = false;
}
