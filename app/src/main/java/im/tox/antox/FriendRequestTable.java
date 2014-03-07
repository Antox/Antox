package im.tox.antox;

import android.provider.BaseColumns;

/**
 * Created by ollie on 04/03/14.
 */
public final class FriendRequestTable {
        // To prevent someone from accidentally instantiating the contract class,
        // give it an empty constructor.
        public FriendRequestTable() {}

        /* Inner class that defines the table contents */
        public static abstract class FriendRequestEntry implements BaseColumns {
            public static final String TABLE_NAME = "friend_request";
            public static final String COLUMN_NAME_KEY = "key";
            public static final String COLUMN_NAME_MESSAGE = "message";



        }
}
