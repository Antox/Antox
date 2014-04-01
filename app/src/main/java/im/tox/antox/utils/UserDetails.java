package im.tox.antox.utils;

import im.tox.jtoxcore.ToxUserStatus;

/**
 * Class for storing the users details so that they are readily available without having to read
 * from the preferences each time
 */
public final class UserDetails {
    public static String username = null;
    public static ToxUserStatus status = null;
    public static String note = null;
}
