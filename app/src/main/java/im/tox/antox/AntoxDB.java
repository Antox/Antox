package im.tox.antox;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Aagam Shah on 7/3/14.
 */
public class AntoxDB extends SQLiteOpenHelper {

    public String CREATE_TABLE_FRIENDS = "CREATE TABLE IF NOT EXISTS friends " +
            "( _id integer primary autoincrement, key text, username text, status text,note text)";

    public String CREATE_TABLE_FRIEND_REQUEST = "CREATE TABLE IF NOT EXISTS friend_request " +
            "( _id integer primary autoincrement, key text, message text)";

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
}
