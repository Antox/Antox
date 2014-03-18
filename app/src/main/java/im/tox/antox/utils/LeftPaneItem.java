package im.tox.antox.utils;

import java.sql.Timestamp;

/**
 * Created by ollie on 04/03/14.
 */
public class LeftPaneItem {

    public int viewType; //Either a header, request, or contact
    public String first; //Either the header, the request key, or the contact name
    public String second; //Either null, the request message, or the contact status message
    public int icon; //Null unless friend, if friend then icon status
    public int count;
    public Timestamp timestamp;

    public LeftPaneItem() {
        super();
    }

    public LeftPaneItem(int viewType, String first, String second, int icon) {
        super();
        this.viewType = viewType;
        this.first = first;
        this.second = second;
        this.icon = icon;
    }

    public LeftPaneItem(int viewType, String first, String second, int icon, int count, Timestamp t) {
        super();
        this.viewType = viewType;
        this.first = first;
        this.second = second;
        this.icon = icon;
        this.count = count;
        this.timestamp = t;
    }
}
