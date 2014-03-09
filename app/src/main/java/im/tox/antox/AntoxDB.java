package im.tox.antox;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

import im.tox.jtoxcore.ToxUserStatus;

/**
 * Created by Aagam Shah on 7/3/14.
 */
public class AntoxDB extends SQLiteOpenHelper {

    public String CREATE_TABLE_FRIENDS = "CREATE TABLE IF NOT EXISTS friends " +
            "( _id integer primary key , key text, username text, status text,note text, isonline boolean)";

    public String CREATE_TABLE_FRIEND_REQUEST = "CREATE TABLE IF NOT EXISTS friend_request " +
            "( _id integer primary key, key text, message text)";

    public String DROP_TABLE_FRIENDS = "DROP TABLE IF EXISTS friends";
    public String DROP_TABLE_FRIEND_REQUEST = "DROP TABLE IF EXISTS friend_request";

    public AntoxDB(Context ctx) {
        super(ctx, Constants.DATABASE_NAME, null, Constants.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_FRIENDS);
        db.execSQL(CREATE_TABLE_FRIEND_REQUEST);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i2) {
        db.execSQL(DROP_TABLE_FRIENDS);
        db.execSQL(DROP_TABLE_FRIEND_REQUEST);
        db.execSQL(CREATE_TABLE_FRIENDS);
        db.execSQL(CREATE_TABLE_FRIEND_REQUEST);

    }

    //Adding friend using his key.
    // Currently we are not able to fetch Note,username so keep it null.
    //So storing the received message as his/her personal note.

    public void addFriend(String key, String message) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_NAME_KEY, key);
        values.put(Constants.COLUMN_NAME_STATUS, "0");
        values.put(Constants.COLUMN_NAME_NOTE, message);
        values.put(Constants.COLUMN_NAME_USERNAME, "");
        values.put(Constants.COLUMN_NAME_ISONLINE, false);
        db.insert(Constants.TABLE_FRIENDS, null, values);
        db.close();
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
                String online = (String) cursor.getString(5);

                if(name.equals(""))
                    name = key.substring(0,7);

                if(online == "online")
                    friendList.add(new Friend(R.drawable.ic_status_online,name,status,note, key));
                else
                    friendList.add(new Friend(R.drawable.ic_status_offline,name,status,note, key));
            } while (cursor.moveToNext());
        }

        cursor.close();

        return friendList;
    }

    public void deleteFriend(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        db.delete(Constants.TABLE_FRIENDS, Constants.COLUMN_NAME_KEY + "='" + key + "'", null);
        db.close();
    }

    public void updateFriendName(String key, String newName) {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_NAME_USERNAME, newName);
        db.update(Constants.TABLE_FRIENDS, values, Constants.COLUMN_NAME_KEY + "='" + key + "'", null);
    }

    public void updateStatusMessage(String key, String newMessage) {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_NAME_NOTE, newMessage);
        db.update(Constants.TABLE_FRIENDS, values, Constants.COLUMN_NAME_KEY + "='" + key + "'", null);
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
    }

    public void updateUserOnline(String key, boolean online) {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put(Constants.COLUMN_NAME_ISONLINE, online);
        db.update(Constants.TABLE_FRIENDS, values, Constants.COLUMN_NAME_KEY + "='" + key + "'", null);
    }
}
