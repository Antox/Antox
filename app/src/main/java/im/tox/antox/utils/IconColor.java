package im.tox.antox.utils;

import android.graphics.Color;
import im.tox.jtoxcore.ToxUserStatus;

/**
 * Created by ollie on 30/05/14.
 */
public class IconColor {
    public static String iconColorAsString(Boolean isOnline, ToxUserStatus status)
    {
        String color;
        if (!isOnline) {
            color = "#B0B0B0"; //offline
        } else if (status == ToxUserStatus.TOX_USERSTATUS_NONE) {
            color = "#5ec245"; //online
        } else if (status == ToxUserStatus.TOX_USERSTATUS_AWAY) {
            color = "#E5C885"; //away
        } else if (status == ToxUserStatus.TOX_USERSTATUS_BUSY) {
            color = "#CF4D58"; //busy
        } else {
            color = "#FFFFFF";
        }
        return color;
    }

    public static int iconColorAsColor(Boolean isOnline, ToxUserStatus status)
    {
        return Color.parseColor(iconColorAsString(isOnline,status));

    }
}
