package im.tox.antox;

public final class Constants {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_FRIEND_REQUEST = 1;
    public static final int TYPE_CONTACT = 2;
    public static final int TYPE_MAX_COUNT = 3;
	public static final String REGISTER = "im.tox.antox.REGISTER";
	public static final String UNREGISTER = "im.tox.antox.UNREGISTER";
	public static final String REGISTER_NAME = "im.tox.antox.REGISTER_NAME";
    public static final String START_TOX = "im.tox.antox.START_TOX";
    public static final String STOP_TOX = "im.tox.antox.STOP_TOX";
    public static final String BROADCAST_ACTION = "im.tox.antox.BROADCAST";
    public static final String CONNECTED_STATUS = "im.tox.antox.STATUS";
    public static final String ADD_FRIEND = "im.tox.antox.ADD";
    public static final String UPDATE_SETTINGS = "im.tox.antox.SETTINGS";
    public static final String FRIEND_LIST = "im.tox.antox.FRIENDS";
    public static final String SEND_MESSAGE ="im.tox.antox.MESSAGE";
    public static final String FRIEND_REQUEST = "im.tox.antox.REQUEST";
    public static final String REJECT_FRIEND_REQUEST = "im.tox.antox.REJECT_REQUEST";
    public static final String ACCEPT_FRIEND_REQUEST = "im.tox.antox.ACCEPT_REQUEST";
    public static final String UPDATE_FRIEND_REQUESTS= "im.tox.antox.UPDATE_REQUESTS";
    public static final String CONNECTION_STATUS = "im.tox.antox.CONNECTION_STATUS";

    //All DB Constants
    public static final String DATABASE_NAME = "antoxdb";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_FRIENDS = "friends";
    public static final String TABLE_FRIEND_REQUEST = "friend_request";
    public static final String COLUMN_NAME_KEY ="key";
    public static final String COLUMN_NAME_MESSAGE ="message";
    public static final String COLUMN_NAME_USERNAME ="username";
    public static final String COLUMN_NAME_NOTE ="note";
    public static final String COLUMN_NAME_STATUS ="status";
    public static final String COLUMN_NAME_ISONLINE = "isonline";



}
