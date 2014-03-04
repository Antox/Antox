package im.tox.antox;

/**
 * Created by ollie on 04/03/14.
 */
public class LeftPaneItem {

    public int viewType; //Either a header, request, or contact
    public String first; //Either the header, the request key, or the contact name
    public String second; //Either null, the request message, or the contact status message
    public int icon; //Null unless friend, if friend then icon status

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

    public String first() {
        return first;
    }
    public String second() {
        return second;
    }
    public int icon() {
        return icon;
    }
    public int viewType() {
        return viewType;
    }
}
