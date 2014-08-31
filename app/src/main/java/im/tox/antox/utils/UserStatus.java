package im.tox.antox.utils;

import im.tox.jtoxcore.ToxUserStatus;

/**
 * Created by fireflystorm on 8/14/14.
 */
public class UserStatus {
    public static ToxUserStatus getToxUserStatusFromString(String status)
    {
        if (status.equals("online"))
            return ToxUserStatus.TOX_USERSTATUS_NONE;
        if (status.equals("away"))
            return ToxUserStatus.TOX_USERSTATUS_AWAY;
        if (status.equals("busy"))
            return ToxUserStatus.TOX_USERSTATUS_BUSY;
        return ToxUserStatus.TOX_USERSTATUS_NONE;
    }

    public static String getStringFromToxUserStatus(ToxUserStatus status)
    {
        if (status == ToxUserStatus.TOX_USERSTATUS_NONE)
            return "online";
        if (status == ToxUserStatus.TOX_USERSTATUS_AWAY)
            return "away";
        if (status == ToxUserStatus.TOX_USERSTATUS_BUSY)
            return "busy";
        return "invalid";
    }
}
