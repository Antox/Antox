
package im.tox.antox.data

import java.util

import android.content.{ContentValues, Context}
import android.database.DatabaseUtils
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import android.util.Log
import im.tox.antox.utils.{Constants, DatabaseUtil}
import im.tox.antox.wrapper.UserInfo

class UserDB(ctx: Context) extends SQLiteOpenHelper(ctx, "userdb", null, Constants.USER_DATABASE_VERSION) {

  private val CREATE_TABLE_USERS: String = "CREATE TABLE IF NOT EXISTS users" + " ( _id integer primary key , " +
    "username text," +
    "password text," +
    "nickname text," +
    "status text," +
    "status_message text," +
    "avatar text," +
    "logging_enabled boolean);"

  override def onCreate(db: SQLiteDatabase) {
    db.execSQL(CREATE_TABLE_USERS)
  }

  override def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int): Unit = {
    Log.w("UserDB", "Upgrading UserDB from version " + oldVersion + " to " + newVersion)

    for (currVersion <- oldVersion to newVersion) {
      currVersion match {
        case 1 =>
          if (!DatabaseUtil.isColumnInTable(db, "users", "avatar"))
            db.execSQL("ALTER TABLE users ADD COLUMN avatar text")
        case 2 =>
          db.execSQL("ALTER TABLE users ADD COLUMN logging_enabled integer")
          db.execSQL("UPDATE users SET logging_enabled = 1")
        case _ =>
      }
    }
  }

  def addUser(username: String, password: String) {
    val db = this.getWritableDatabase
    val values = new ContentValues()
    values.put("username", username)
    values.put("password", password)
    values.put("nickname", username)
    values.put("status", "online")
    values.put("status_message", "Hey! I'm using Antox")
    values.put("avatar", "")
    values.put("logging_enabled", true)
    db.insert("users", null, values)
    db.close()
  }

  def doesUserExist(username: String): Boolean = {
    val db = this.getReadableDatabase
    val cursor = db.rawQuery("SELECT count(*) FROM users WHERE username='" + username +
      "'", null)
    cursor.moveToFirst()
    val count = cursor.getInt(0)
    cursor.close()
    db.close()
    count > 0
  }

  def getUserDetails(username: String): UserInfo = {
    val db = this.getReadableDatabase
    val query = "SELECT * FROM users WHERE username='" + username + "'"
    val cursor = db.rawQuery(query, null)
    var userInfo: UserInfo = null
    if (cursor.moveToFirst()) {
      userInfo = new UserInfo(username,
        cursor.getString(2),
        cursor.getString(3),
        cursor.getString(4),
        cursor.getString(5),
        cursor.getInt(7) > 0,
        cursor.getString(6))
    }
    cursor.close()
    db.close()
    userInfo
  }

  def updateUserDetail(username: String, detail: String, newDetail: String) {
    val db = this.getReadableDatabase
    val query = "UPDATE users SET " + detail + "=" + DatabaseUtils.sqlEscapeString(newDetail) + " WHERE username='" +
      username +
      "'"
    db.execSQL(query)
    db.close()
  }

  def updateUserDetail(username: String, detail: String, newDetail: Boolean) {
    val db = this.getReadableDatabase
    val query = "UPDATE users SET " + detail + "=" + (if (newDetail) 1 else 0) + " WHERE username='" +
      username +
      "'"
    db.execSQL(query)
    db.close()
  }

  def doUsersExist(): Boolean = {
    val db = this.getReadableDatabase
    val cursor = db.rawQuery("SELECT count(*) FROM users", null)
    cursor.moveToFirst()
    val count = cursor.getInt(0)
    cursor.close()
    db.close()
    count > 0
  }

  def getAllProfiles: util.ArrayList[String] = {
    val profiles = new util.ArrayList[String]()
    val sqLiteDatabase = this.getReadableDatabase
    val query = "SELECT username FROM users"
    val cursor = sqLiteDatabase.rawQuery(query, null)
    if (cursor.moveToFirst()) {
      do {
        profiles.add(cursor.getString(0))
      } while (cursor.moveToNext())
    }
    cursor.close()
    profiles
  }
}
