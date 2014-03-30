package im.tox.antox.utils;

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
    public static final String SWITCH_TO_FRIEND = "im.tox.antox.SWITCH_TO_FRIEND";
    public static final String SEND_MESSAGE ="im.tox.antox.MESSAGE";
    public static final String UPDATE_MESSAGES ="im.tox.antox.UPDATE_MESSAGES";
    public static final String CLEAR_NOTIFICATIONS_FOR_FRIENDNUMBER  = "im.tox.antox.CLEAR_NOTIFICATIONS_FOR_FRIENDNUMBER";
    public static final String FRIEND_REQUEST = "im.tox.antox.REQUEST";
    public static final String DELETE_FRIEND = "im.tox.antox.DELETE_FRIEND";
    public static final String DELIVERY_RECEIPT = "im.tox.antox.DELIVERY_RECEIPT";
    public static final String DELETE_FRIEND_AND_CHAT = "im.tox.antox.DELETE_FRIEND_AND_CHAT";
    public static final String ON_MESSAGE = "im.tox.antox.ON_MESSAGE";
    public static final String REJECT_FRIEND_REQUEST = "im.tox.antox.REJECT_REQUEST";
    public static final String ACCEPT_FRIEND_REQUEST = "im.tox.antox.ACCEPT_REQUEST";
    public static final String UPDATE_LEFT_PANE = "im.tox.antox.UPDATE_REQUESTS";
    public static final String CONNECTION_STATUS = "im.tox.antox.CONNECTION_STATUS";
    public static final String SEND_UNSENT_MESSAGES = "im.tox.antox.SEND_UNSENT_MESSAGES";
    public static final String UPDATE = "im.tox.antox.UPDATE";

    //All DB Constants
    public static final String DATABASE_NAME = "antoxdb";
    public static final int DATABASE_VERSION = 5;
    public static final String TABLE_FRIENDS = "friends";
    public static final String TABLE_CHAT_LOGS = "chat_logs";
    public static final String TABLE_FRIEND_REQUEST = "friend_request";
    public static final String COLUMN_NAME_KEY ="key";
    public static final String COLUMN_NAME_MESSAGE ="message";
    public static final String COLUMN_NAME_USERNAME ="username";
    public static final String COLUMN_NAME_TIMESTAMP ="timestamp";
    public static final String COLUMN_NAME_NOTE ="note";
    public static final String COLUMN_NAME_STATUS ="status";
    public static final String COLUMN_NAME_MESSAGE_ID ="message_id";
    public static final String COLUMN_NAME_IS_OUTGOING ="is_outgoing";
    public static final String COLUMN_NAME_HAS_BEEN_RECEIVED ="has_been_received";
    public static final String COLUMN_NAME_HAS_BEEN_READ ="has_been_read";
    public static final String COLUMN_NAME_SUCCESSFULLY_SENT ="successfully_sent";
    public static final String COLUMN_NAME_ISONLINE = "isonline";
    public static final String COLUMN_NAME_ALIAS = "alias";
    public static final String COLUMN_NAME_ISBLOCKED = "isblocked";

    //Activity request code for onActivityResult methods
    public static final int ADD_FRIEND_REQUEST_CODE=0;
    public static final int SENDFILE_PICKEDFRIEND_CODE=1;

}
