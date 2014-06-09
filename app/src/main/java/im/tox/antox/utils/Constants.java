package im.tox.antox.utils;

public final class Constants {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_FRIEND_REQUEST = 1;
    public static final int TYPE_CONTACT = 2;
    public static final int TYPE_MAX_COUNT = 3;
    public static final String START_TOX = "im.tox.antox.START_TOX";
    public static final String STOP_TOX = "im.tox.antox.STOP_TOX";
    public static final String BROADCAST_ACTION = "im.tox.antox.BROADCAST";
    public static final String SWITCH_TO_FRIEND = "im.tox.antox.SWITCH_TO_FRIEND";
    public static final String UPDATE_MESSAGES ="im.tox.antox.UPDATE_MESSAGES";
    public static final String ON_MESSAGE = "im.tox.antox.ON_MESSAGE";
    public static final String UPDATE_LEFT_PANE = "im.tox.antox.UPDATE_REQUESTS";
    public static final String UPDATE = "im.tox.antox.UPDATE";

    //All DB Constants
    public static final String DATABASE_NAME = "antoxdb";
    public static final int DATABASE_VERSION = 6;
    public static final String TABLE_FRIENDS = "friends";
    public static final String TABLE_CHAT_LOGS = "messages";
    public static final String TABLE_FRIEND_REQUEST = "friend_requests";
    public static final String COLUMN_NAME_KEY ="tox_key";
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
    public static final int UPDATE_SETTINGS_REQUEST_CODE=2;
    public static final int WELCOME_ACTIVITY_REQUEST_CODE = 3;
}
