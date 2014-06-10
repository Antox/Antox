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
    public String key;
    public Timestamp timestamp;

    public LeftPaneItem() {
        super();
    }

    public LeftPaneItem(String header) {
        this.viewType = Constants.TYPE_HEADER;
        this.first = header;
        this.key = "";
    }

    public LeftPaneItem(String key, String message) {
        super();
        this.viewType = Constants.TYPE_FRIEND_REQUEST;
        this.first = key;
        this.key = key;
        this.second = message;
    }

    public LeftPaneItem(String key, String name, String message, int icon, int count, Timestamp t) {
        super();
        this.viewType = Constants.TYPE_CONTACT;
        this.key = key;
        this.first = name;
        this.second = message;
        this.icon = icon;
        this.count = count;
        this.timestamp = t;
    }
}
