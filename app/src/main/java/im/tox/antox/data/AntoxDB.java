package im.tox.antox.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.Constants;
import im.tox.antox.utils.Friend;
import im.tox.antox.utils.FriendRequest;
import im.tox.antox.utils.Message;
import im.tox.antox.utils.Tuple;
import im.tox.jtoxcore.ToxUserStatus;

/**
 * Created by Aagam Shah on 7/3/14.
 */
public class AntoxDB extends SQLiteOpenHelper {
    // After modifying one of this tables, update the database version in Constants.DATABASE_VERSION
    // and also update the onUpgrade method
    public String CREATE_TABLE_FRIENDS = "CREATE TABLE IF NOT EXISTS friends" +
            " (tox_key text primary key, " +
            "username text, " +
            "status text, " +
            "note text, " +
            "alias text, " +
            "isonline boolean, " +
            "isblocked boolean);";

    public String CREATE_TABLE_MESSAGES = "CREATE TABLE IF NOT EXISTS messages" +
            " ( _id integer primary key , " +
            "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "message_id integer, " +
            "tox_key text, " +
            "message text, " +
            "has_been_received boolean, " +
            "has_been_read boolean, " +
            "successfully_sent boolean, " +
            "size integer, " +
            "type int, " +
            "FOREIGN KEY(tox_key) REFERENCES friends(tox_key))";

    public String CREATE_TABLE_FRIEND_REQUESTS = "CREATE TABLE IF NOT EXISTS friend_requests" +
            " ( _id integer primary key, " +
            "tox_key text, " +
            "message text)";

    public AntoxDB(Context ctx) {
        super(ctx, Constants.ACTIVE_DATABASE_NAME, null, Constants.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_FRIENDS);
        db.execSQL(CREATE_TABLE_FRIEND_REQUESTS);
        db.execSQL(CREATE_TABLE_MESSAGES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_FRIENDS);
        db.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_CHAT_LOGS);
        db.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_FRIEND_REQUEST);
        onCreate(db);
    }

    //check if a column is in a table
    private boolean isColumnInTable(SQLiteDatabase db, String table, String column) {
        try {
            Cursor cursor = db.rawQuery("SELECT * FROM " + table + " LIMIT 0", null);

            //if it is -1 the column does not exists
            if(cursor.getColumnIndex(column) == -1) {
                return false;
            }
            else {
                return true;
            }
        }
        catch (Exception e) {
            return false;
        }
    }

    public void addFriend(String key, String message, String alias, String username) {
        SQLiteDatabase db = this.getWritableDatabase();

        if(username.contains("@"))
            username = username.substring(0, username.indexOf("@"));

        if(username == null || username.length() == 0)
            username = key.substring(0,7);

        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_NAME_KEY, key);
        values.put(Constants.COLUMN_NAME_STATUS, "0");
        values.put(Constants.COLUMN_NAME_NOTE, message);
        values.put(Constants.COLUMN_NAME_USERNAME, username);
        values.put(Constants.COLUMN_NAME_ISONLINE, false);
        values.put(Constants.COLUMN_NAME_ALIAS, alias);
        values.put(Constants.COLUMN_NAME_ISBLOCKED, false);
        db.insert(Constants.TABLE_FRIENDS, null, values);
        db.close();
    }

    public void addFileTransfer(String key, String path, int fileNumber, int size, boolean sending) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_NAME_KEY, key);
        values.put(Constants.COLUMN_NAME_MESSAGE, path);
        values.put(Constants.COLUMN_NAME_MESSAGE_ID, fileNumber);
        values.put(Constants.COLUMN_NAME_HAS_BEEN_RECEIVED, false);
        values.put(Constants.COLUMN_NAME_HAS_BEEN_READ, false);
        values.put(Constants.COLUMN_NAME_SUCCESSFULLY_SENT, false);
        if (sending) {
            values.put("type", Constants.MESSAGE_TYPE_FILE_TRANSFER);
        } else {
            values.put("type", Constants.MESSAGE_TYPE_FILE_TRANSFER_FRIEND);
        }
        values.put("size", size);
        db.insert(Constants.TABLE_CHAT_LOGS, null, values);
        db.close();
    }

    public void fileTransferStarted(String key, int fileNumber) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "UPDATE messages SET " + Constants.COLUMN_NAME_SUCCESSFULLY_SENT + " = 1 WHERE (type == 3 OR type == 4) AND message_id == " + fileNumber + " AND tox_key = '" + key + "'";
        db.execSQL(query);
        db.close();
    }

    public void addFriendRequest(String key, String message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_NAME_KEY, key);
        values.put(Constants.COLUMN_NAME_MESSAGE, message);
        db.insert(Constants.TABLE_FRIEND_REQUEST, null, values);
        db.close();
    }

    public void addMessage(int message_id, String key, String message, boolean has_been_received, boolean has_been_read, boolean successfully_sent, int type){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_NAME_MESSAGE_ID, message_id);
        values.put(Constants.COLUMN_NAME_KEY, key);
        values.put(Constants.COLUMN_NAME_MESSAGE, message);
        values.put(Constants.COLUMN_NAME_HAS_BEEN_RECEIVED, has_been_received);
        values.put(Constants.COLUMN_NAME_HAS_BEEN_READ, has_been_read);
        values.put(Constants.COLUMN_NAME_SUCCESSFULLY_SENT, successfully_sent);
        values.put("type", type);

        db.insert(Constants.TABLE_CHAT_LOGS, null, values);
        db.close();
    }

    public HashMap getUnreadCounts() {
        SQLiteDatabase db = this.getReadableDatabase();
        HashMap map = new HashMap();
        String selectQuery = "SELECT friends.tox_key, COUNT(messages._id) " +
                "FROM messages " +
                "JOIN friends ON friends.tox_key = messages.tox_key " +
                "WHERE messages.has_been_read == 0 AND (messages.type == 2 OR messages.type == 4)" +
                "GROUP BY friends.tox_key";
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                String key = cursor.getString(0);
                int count = cursor.getInt(1);
                map.put(key, count);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return map;
    };

    public String getFilePath(String key, int fileNumber) {
        SQLiteDatabase db = this.getReadableDatabase();
        String path = "";
        String selectQuery = "SELECT message FROM messages WHERE tox_key = '" + key + "' AND (type == 3 OR type == 4) AND message_id == " +
                fileNumber;
        Cursor cursor = db.rawQuery(selectQuery, null);
        Log.d("getFilePath count: ", Integer.toString(cursor.getCount()) + " filenumber: " + fileNumber);
        if (cursor.moveToFirst()) {
            path = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return path;
    }

    public int getFileId(String key, int fileNumber) {
        SQLiteDatabase db = this.getReadableDatabase();
        int id = -1;
        String selectQuery = "SELECT _id FROM messages WHERE tox_key = '" + key + "' AND (type == 3 OR type == 4) AND message_id == " +
                fileNumber;
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return id;
    }

    public void clearFileNumbers() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "UPDATE messages SET message_id = -1 WHERE (type == 3 OR type == 4)";
        db.execSQL(query);
        db.close();
    }

    public void clearFileNumber(String key, int fileNumber) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "UPDATE messages SET message_id = -1 WHERE (type == 3 OR type == 4) AND message_id == " + fileNumber + " AND tox_key = '" + key + "'";
        db.execSQL(query);
        db.close();
    }

    public void fileFinished(String key, int fileNumber) {
        Log.d("AntoxDB","fileFinished");
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "UPDATE messages SET " + Constants.COLUMN_NAME_HAS_BEEN_RECEIVED + "=1, message_id = -1 WHERE (type == 3 OR type == 4) AND message_id == " + fileNumber + " AND tox_key = '" + key + "'";
        db.execSQL(query);
        db.close();
    }

    public HashMap getLastMessages() {
        SQLiteDatabase db = this.getReadableDatabase();
        HashMap map = new HashMap();
        String selectQuery = "SELECT tox_key, message, timestamp FROM messages WHERE _id IN (" +
                "SELECT MAX(_id) " +
                "FROM messages WHERE (type == 1 OR type == 2) " +
                "GROUP BY tox_key)";
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                String key = cursor.getString(0);
                String message = cursor.getString(1);
                Timestamp timestamp = Timestamp.valueOf(cursor.getString(2));
                map.put(key, new Tuple<String,Timestamp>(message,timestamp));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return map;
    }

    public ArrayList<Message> getMessageList(String key, boolean actionMessages) {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<Message> messageList = new ArrayList<Message>();
        String selectQuery;
        if (key.equals("")) {
            selectQuery = "SELECT * FROM " + Constants.TABLE_CHAT_LOGS + " ORDER BY " + Constants.COLUMN_NAME_TIMESTAMP + " DESC";
        } else {
            String act;
            if (actionMessages) {
                act = "";
            } else {
                act = "AND (type == 1 OR type == 2 OR type == 3 OR type == 4) ";
            }
            selectQuery = "SELECT * FROM " + Constants.TABLE_CHAT_LOGS + " WHERE " + Constants.COLUMN_NAME_KEY + " = '" + key + "' " + act + "ORDER BY " + Constants.COLUMN_NAME_TIMESTAMP + " ASC";
        }
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                Timestamp time = Timestamp.valueOf(cursor.getString(1));
                int message_id = cursor.getInt(2);
                String k = cursor.getString(3);
                String m = cursor.getString(4);
                boolean received = cursor.getInt(5)>0;
                boolean read = cursor.getInt(6)>0;
                boolean sent = cursor.getInt(7)>0;
                int size = cursor.getInt(8);
                int type = cursor.getInt(9);
                messageList.add(new Message(id, message_id, k, m,received, read, sent, time, size, type));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return messageList;
    }

    public ArrayList<FriendRequest> getFriendRequestsList() {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<FriendRequest> friendRequests = new ArrayList<FriendRequest>();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                Constants.COLUMN_NAME_KEY,
                Constants.COLUMN_NAME_MESSAGE
        };

        Cursor cursor = db.query(
                Constants.TABLE_FRIEND_REQUEST,  // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                 // The sort order
        );
        if (cursor.moveToFirst()) {
            do {
                String key = cursor.getString(
                        cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_KEY)
                );
                String message = cursor.getString(
                        cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_MESSAGE)
                );
                friendRequests.add(new FriendRequest(key, message));
            } while (cursor.moveToNext()) ;
        }

        cursor.close();
        db.close();

        return friendRequests;
    }

    public ArrayList<Message> getUnsentMessageList() {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<Message> messageList = new ArrayList<Message>();
        String selectQuery = "SELECT * FROM " + Constants.TABLE_CHAT_LOGS + " WHERE " + Constants.COLUMN_NAME_SUCCESSFULLY_SENT + "=0 AND type == 1 ORDER BY " + Constants.COLUMN_NAME_TIMESTAMP + " ASC";
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                Timestamp time = Timestamp.valueOf(cursor.getString(1));
                int m_id = cursor.getInt(2);
                Log.d("UNSENT MESAGE ID: ", "" + m_id);
                String k = cursor.getString(3);
                String m = cursor.getString(4);
                boolean received = cursor.getInt(5)>0;
                boolean read = cursor.getInt(6)>0;
                boolean sent = cursor.getInt(7)>0;
                int size = cursor.getInt(8);
                int type = cursor.getInt(9);
                messageList.add(new Message(id, m_id, k, m, received, read, sent, time, size, type));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return messageList;
    }

    public void updateUnsentMessage(int m_id) {
        Log.d("UPDATE UNSENT MESSAGE - ID : ", "" + m_id);
        String messageId = m_id + "";
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_NAME_SUCCESSFULLY_SENT, "1");
        values.put("type", 1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = new Date();
        values.put(Constants.COLUMN_NAME_TIMESTAMP, dateFormat.format(date));
        db.update(Constants.TABLE_CHAT_LOGS, values, Constants.COLUMN_NAME_MESSAGE_ID + "=" + messageId
                + " AND " + Constants.COLUMN_NAME_SUCCESSFULLY_SENT + "=0", null);
        db.close();
    }

    public String setMessageReceived(int receipt) { //returns public key of who the message was sent to
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + Constants.TABLE_CHAT_LOGS + " SET " + Constants.COLUMN_NAME_HAS_BEEN_RECEIVED + "=1 WHERE " + Constants.COLUMN_NAME_MESSAGE_ID + "=" + receipt + " AND " + Constants.COLUMN_NAME_SUCCESSFULLY_SENT + "=1 AND type =1";
        db.execSQL(query);
        String selectQuery = "SELECT * FROM " + Constants.TABLE_CHAT_LOGS + " WHERE " + Constants.COLUMN_NAME_MESSAGE_ID + "=" + receipt + " AND " + Constants.COLUMN_NAME_SUCCESSFULLY_SENT + "=1 AND type=1";
        Cursor cursor = db.rawQuery(selectQuery, null);
        String k = "";
        if (cursor.moveToFirst()) {
            k = cursor.getString(3);
        }
        cursor.close();
        db.close();
        return k;
    }

    public void markIncomingMessagesRead(String key) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + Constants.TABLE_CHAT_LOGS + " SET " + Constants.COLUMN_NAME_HAS_BEEN_READ + "=1 WHERE " + Constants.COLUMN_NAME_KEY + "='" + key +"' AND (type == 2 OR type == 4)";
        db.execSQL(query);
        db.close();
        Log.d("", "marked incoming messages as read");
    }

    public void deleteMessage(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + Constants.TABLE_CHAT_LOGS + " WHERE _id == " + id;
        db.execSQL(query);
        db.close();
        Log.d("", "Deleted message");
    }

    public ArrayList<Friend> getFriendList() {
        SQLiteDatabase db = this.getReadableDatabase();

        ArrayList<Friend> friendList = new ArrayList<Friend>();
        String selectQuery = "SELECT  * FROM " + Constants.TABLE_FRIENDS;

        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(1);
                String key = cursor.getString(0);
                String status = cursor.getString(2);
                String note = cursor.getString(3);
                String alias = cursor.getString(4);
                int online = cursor.getInt(5);
                boolean isBlocked = cursor.getInt(6)>0;

                if(alias == null)
                    alias = "";

                if(!alias.equals(""))
                    name = alias;
                else if(name.equals(""))
                    name = key.substring(0,7);

                if(!isBlocked)
                    friendList.add(new Friend(online, name, status, note, key, alias));

            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return friendList;
    }

    public boolean doesFriendExist(String key) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor mCount = db.rawQuery("SELECT count(*) FROM " + Constants.TABLE_FRIENDS
                + " WHERE " + Constants.COLUMN_NAME_KEY + "='" + key + "'", null);
        mCount.moveToFirst();
        int count = mCount.getInt(0);
        if(count > 0) {
            mCount.close();
            db.close();
            return true;
        }
        mCount.close();
        db.close();
        return false;
    }

    public void setAllOffline() {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_NAME_ISONLINE, "0");
        db.update(Constants.TABLE_FRIENDS, values, Constants.COLUMN_NAME_ISONLINE + "='1'", null);
        db.close();
    }

    public void deleteFriend(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        db.delete(Constants.TABLE_FRIENDS, Constants.COLUMN_NAME_KEY + "='" + key + "'", null);
        db.close();
    }

    public void deleteFriendRequest(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        db.delete(Constants.TABLE_FRIEND_REQUEST, Constants.COLUMN_NAME_KEY + "='" + key + "'", null);
        db.close();
    }

    public String getFriendRequestMessage(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT message FROM " + Constants.TABLE_FRIEND_REQUEST + " WHERE tox_key='" + key + "'";

        Cursor cursor = db.rawQuery(selectQuery, null);
        String message = "";
        if (cursor.moveToFirst()) {
            message = cursor.getString(0);
        }
        cursor.close();
        db.close();

        return message;
    }

    public void deleteChat(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        db.delete(Constants.TABLE_CHAT_LOGS, Constants.COLUMN_NAME_KEY + "='" + key + "'", null);
        db.close();
    }

    public void updateFriendName(String key, String newName) {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_NAME_USERNAME, newName);
        db.update(Constants.TABLE_FRIENDS, values, Constants.COLUMN_NAME_KEY + "='" + key + "'", null);
        db.close();
    }

    public void updateStatusMessage(String key, String newMessage) {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_NAME_NOTE, newMessage);
        db.update(Constants.TABLE_FRIENDS, values, Constants.COLUMN_NAME_KEY + "='" + key + "'", null);
        db.close();
    }

    public void updateUserStatus(String key, ToxUserStatus status) {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues values = new ContentValues();
        String tmp = "";
        if (status == ToxUserStatus.TOX_USERSTATUS_BUSY) {
            tmp = "busy";
        } else if (status == ToxUserStatus.TOX_USERSTATUS_AWAY) {
            tmp = "away";
        }
        values.put(Constants.COLUMN_NAME_STATUS, tmp);
        db.update(Constants.TABLE_FRIENDS, values, Constants.COLUMN_NAME_KEY + "='" + key + "'", null);
        db.close();
    }

    public void updateUserOnline(String key, boolean online) {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_NAME_ISONLINE, online);
        db.update(Constants.TABLE_FRIENDS, values, Constants.COLUMN_NAME_KEY + "='" + key + "'", null);
        db.close();
    }

    public String[] getFriendDetails(String key) {
        String[] details = { null, null, null };

        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + Constants.TABLE_FRIENDS + " WHERE " + Constants.COLUMN_NAME_KEY + "='" + key + "'";
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(1);
                String note = cursor.getString(3);
                String alias = cursor.getString(4);

                if(name == null)
                    name = "";

                if(name.equals(""))
                    name = key.substring(0, 7);

                details[0] = name;
                details[1] = alias;
                details[2] = note;

            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return details;
    }

    public void updateAlias(String alias, String key) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + Constants.TABLE_FRIENDS + " SET " + Constants.COLUMN_NAME_ALIAS + "='" + alias + "' WHERE " + Constants.COLUMN_NAME_KEY + "='" + key + "'";
        db.execSQL(query);
        db.close();
    }

    public boolean isFriendBlocked(String key) {
        boolean isBlocked = false;
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT isBlocked FROM " + Constants.TABLE_FRIENDS + " WHERE " + Constants.COLUMN_NAME_KEY + "='" + key + "'";
        Cursor cursor = db.rawQuery(selectQuery, null);
        if(cursor.moveToFirst()) {
            isBlocked = cursor.getInt(0)>0;
        }
        cursor.close();
        db.close();
        return isBlocked;
    }

    public void blockUser(String key) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + Constants.TABLE_FRIENDS + " SET " + Constants.COLUMN_NAME_ISBLOCKED + "='1' WHERE " + Constants.COLUMN_NAME_KEY + "='" + key + "'";
        db.execSQL(query);
        db.close();
    }

    public void unblockUser(String key) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + Constants.TABLE_FRIENDS + " SET " + Constants.COLUMN_NAME_ISBLOCKED + "='0' WHERE " + Constants.COLUMN_NAME_KEY + "='" + key + "'";
        db.execSQL(query);
        db.close();
    }
}
