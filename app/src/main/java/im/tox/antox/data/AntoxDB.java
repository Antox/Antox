package im.tox.antox.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.TimeZone;

import im.tox.antox.utils.Constants;
import im.tox.antox.utils.Friend;
import im.tox.antox.utils.FriendRequest;
import im.tox.antox.utils.Message;
import im.tox.jtoxcore.ToxUserStatus;
import im.tox.antox.tox.ToxSingleton;

/**
 * Created by Aagam Shah on 7/3/14.
 */
public class AntoxDB extends SQLiteOpenHelper {

    private ToxSingleton toxSingleton = ToxSingleton.getInstance();

    // After modifying one of this tables, update the database version in Constants.DATABASE_VERSION
    // and also update the onUpgrade method
    public String CREATE_TABLE_FRIENDS = "CREATE TABLE IF NOT EXISTS " + Constants.TABLE_FRIENDS +
            " ( _id integer primary key , key text, username text, status text, note text,  alias text, isonline boolean, isblocked boolean)";

    public String CREATE_TABLE_CHAT_LOGS = "CREATE TABLE IF NOT EXISTS " + Constants.TABLE_CHAT_LOGS +
            " ( _id integer primary key , timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, message_id integer, key text, message text, is_outgoing boolean, has_been_received boolean, has_been_read boolean, successfully_sent boolean)";

    public String CREATE_TABLE_FRIEND_REQUEST = "CREATE TABLE IF NOT EXISTS " + Constants.TABLE_FRIEND_REQUEST +
            " ( _id integer primary key, key text, message text)";

    public AntoxDB(Context ctx) {
        super(ctx, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_FRIENDS);
        db.execSQL(CREATE_TABLE_FRIEND_REQUEST);
        db.execSQL(CREATE_TABLE_CHAT_LOGS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch(oldVersion) {
            case 1:
                db.execSQL("ALTER TABLE " + Constants.TABLE_CHAT_LOGS + " ADD COLUMN has_been_read boolean");
                break;
            case 2:
                db.execSQL("ALTER TABLE " + Constants.TABLE_CHAT_LOGS + " ADD COLUMN successfully_sent boolean");
                break;
            case 3:
                //There are some possibilities when in version 3 there is already the alis column
                if (!isColumnInTable(db, Constants.TABLE_FRIENDS, Constants.COLUMN_NAME_ALIAS)) {
                    db.execSQL("ALTER TABLE " + Constants.TABLE_FRIENDS + " ADD COLUMN alias text");
                    break;
                }
            case 4:
                if (!isColumnInTable(db, Constants.TABLE_FRIENDS, Constants.COLUMN_NAME_ISBLOCKED)) {
                    db.execSQL("ALTER TABLE " + Constants.TABLE_FRIENDS + " ADD COLUMN isblocked boolean");
                    break;
                }
        }
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

    //Adding friend using his key.
    // Currently we are not able to fetch Note,username so keep it null.
    //So storing the received message as his/her personal note.

    public void addFriend(String key, String message, String alias) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_NAME_KEY, key);
        values.put(Constants.COLUMN_NAME_STATUS, "0");
        values.put(Constants.COLUMN_NAME_NOTE, message);
        values.put(Constants.COLUMN_NAME_USERNAME, "");
        values.put(Constants.COLUMN_NAME_ISONLINE, false);
        values.put(Constants.COLUMN_NAME_ALIAS, alias);
        db.insert(Constants.TABLE_FRIENDS, null, values);
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

    public void addMessage(int message_id, String key, String message, boolean is_outgoing, boolean has_been_received, boolean has_been_read, boolean successfully_sent){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_NAME_MESSAGE_ID, message_id);
        values.put(Constants.COLUMN_NAME_KEY, key);
        values.put(Constants.COLUMN_NAME_MESSAGE, message);
        values.put(Constants.COLUMN_NAME_IS_OUTGOING, is_outgoing);
        values.put(Constants.COLUMN_NAME_HAS_BEEN_RECEIVED, has_been_received);
        values.put(Constants.COLUMN_NAME_HAS_BEEN_READ, has_been_read);
        values.put(Constants.COLUMN_NAME_SUCCESSFULLY_SENT, successfully_sent);
        db.insert(Constants.TABLE_CHAT_LOGS, null, values);
        db.close();
    }

    public ArrayList<Message> getMessageList(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<Message> messageList = new ArrayList<Message>();
        String selectQuery;
        if (key == "") {
            selectQuery = "SELECT * FROM " + Constants.TABLE_CHAT_LOGS + " ORDER BY " + Constants.COLUMN_NAME_TIMESTAMP + " DESC";
        } else {
            selectQuery = "SELECT * FROM " + Constants.TABLE_CHAT_LOGS + " WHERE " + Constants.COLUMN_NAME_KEY + " = '" + key + "' ORDER BY " + Constants.COLUMN_NAME_TIMESTAMP + " ASC";
        }
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                int m_id = cursor.getInt(0);
                String k = cursor.getString(3);
                String m = cursor.getString(4);
                boolean outgoing = cursor.getInt(5)>0;
                boolean received = cursor.getInt(6)>0;
                boolean read = cursor.getInt(7)>0;
                boolean sent = cursor.getInt(8)>0;
                Timestamp time = Timestamp.valueOf(cursor.getString(1));
                messageList.add(new Message(m_id, k, m, outgoing, received, read, sent, time));
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
        try {
            int count = cursor.getCount();
            cursor.moveToFirst();
            for (int i = 0; i < count; i++) {
                String key = cursor.getString(
                        cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_KEY)
                );
                String message = cursor.getString(
                        cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_MESSAGE)
                );
                friendRequests.add(new FriendRequest(key, message));
                cursor.moveToNext();
            }
        } finally {
            cursor.close();
        }

        db.close();

        return friendRequests;
    }

    public ArrayList<Message> getUnsentMessageList() {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<Message> messageList = new ArrayList<Message>();
        String selectQuery = "SELECT * FROM " + Constants.TABLE_CHAT_LOGS + " WHERE " + Constants.COLUMN_NAME_SUCCESSFULLY_SENT + "=0 ORDER BY " + Constants.COLUMN_NAME_TIMESTAMP + " ASC";
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                int m_id = cursor.getInt(2);
                Log.d("UNSENT MESAGE ID: ", "" + m_id);
                String k = cursor.getString(3);
                String m = cursor.getString(4);
                boolean outgoing = cursor.getInt(5)>0;
                boolean received = cursor.getInt(6)>0;
                boolean read = cursor.getInt(7)>0;
                boolean sent = cursor.getInt(8)>0;
                Timestamp time = Timestamp.valueOf(cursor.getString(1));
                messageList.add(new Message(m_id, k, m, outgoing, received, read, sent, time));
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
        values.put(Constants.COLUMN_NAME_IS_OUTGOING, "1");
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
        String query = "UPDATE " + Constants.TABLE_CHAT_LOGS + " SET " + Constants.COLUMN_NAME_HAS_BEEN_RECEIVED + "=1 WHERE " + Constants.COLUMN_NAME_MESSAGE_ID + "=" + receipt + " AND " + Constants.COLUMN_NAME_SUCCESSFULLY_SENT + "=1 AND " + Constants.COLUMN_NAME_IS_OUTGOING + "=1";
        db.execSQL(query);
        String selectQuery = "SELECT * FROM " + Constants.TABLE_CHAT_LOGS + " WHERE " + Constants.COLUMN_NAME_MESSAGE_ID + "=" + receipt + " AND " + Constants.COLUMN_NAME_SUCCESSFULLY_SENT + "=1 AND " + Constants.COLUMN_NAME_IS_OUTGOING + "=1";
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
        String query = "UPDATE " + Constants.TABLE_CHAT_LOGS + " SET " + Constants.COLUMN_NAME_HAS_BEEN_READ + "=1 WHERE " + Constants.COLUMN_NAME_KEY + "='" + key +"' AND " + Constants.COLUMN_NAME_IS_OUTGOING + "=0";
        db.execSQL(query);
        db.close();
        Log.d("", "marked incoming messages as read");
    }

    public ArrayList<Friend> getFriendList() {
        SQLiteDatabase db = this.getReadableDatabase();

        ArrayList<Friend> friendList = new ArrayList<Friend>();
        // Getting all friends
        String selectQuery = "SELECT  * FROM " + Constants.TABLE_FRIENDS;

        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(2);
                String key = cursor.getString(1);
                String status = cursor.getString(3);
                String note = cursor.getString(4);
                String alias = cursor.getString(5);
                int online = cursor.getInt(6);
                boolean isBlocked = cursor.getInt(7)>0;

                if(alias == null)
                    alias = "";

                if(!alias.equals(""))
                    name = alias;
                else if(name.equals(""))
                    name = key.substring(0,7);

                if(!isBlocked)
                    friendList.add(new Friend(online,name,status,note, key));
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
        db.update(Constants.TABLE_FRIENDS, values, Constants.COLUMN_NAME_ISONLINE + "='1'",  null);
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

    public void deleteChat(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        db.delete(Constants.TABLE_CHAT_LOGS, Constants.COLUMN_NAME_KEY + "='" + key + "'", null);
        db.close();
    }

    public void deleteMessage(int messageId)
    {
        System.out.println("%%"+messageId);
        SQLiteDatabase db = this.getReadableDatabase();
        db.delete(Constants.TABLE_CHAT_LOGS, "_id" + "='" + messageId + "'", null);
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
        Log.d("DB", selectQuery);
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(2);
                String note = cursor.getString(4);
                String alias = cursor.getString(5);

                if(name.equals(""))
                    name = key.substring(0,7);

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
        String query = "UPDATE " + Constants.TABLE_FRIENDS + " SET " + Constants.COLUMN_NAME_ISBLOCKED + "='TRUE' WHERE " + Constants.COLUMN_NAME_KEY + "='" + key + "'";
        db.execSQL(query);
        db.close();
    }

    public void unblockUser(String key) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + Constants.TABLE_FRIENDS + " SET " + Constants.COLUMN_NAME_ISBLOCKED + "='FALSE' WHERE " + Constants.COLUMN_NAME_KEY + "='" + key + "'";
        db.execSQL(query);
        db.close();
    }
}
