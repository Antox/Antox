package im.tox.antox.utils;

/**
 * Created by ollie on 30/05/14.
 */
public class IconColor {
    public static String iconColor (int i) {
        String color;
        if (i == 0) {
            color = "#B0B0B0"; //offline
        } else if (i == 1) {
            color = "#5ec245"; //online
        } else if (i == 2) {
            color = "#E5C885"; //away
        } else if (i == 3) {
            color = "#CF4D58"; //busy
        } else {
            color = "#FFFFFF";
        }
        return color;
    }
}
