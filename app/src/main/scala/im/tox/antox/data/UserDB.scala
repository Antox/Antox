
package im.tox.antox.data

import java.util

import android.content.{ContentValues, Context}
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import im.tox.antox.wrapper.{FileKind, UserInfo}

//remove if not needed

class UserDB(ctx: Context) extends SQLiteOpenHelper(ctx, "userdb", null, 1) {

  private val CREATE_TABLE_USERS: String = "CREATE TABLE IF NOT EXISTS users" + " ( _id integer primary key , " +
    "username text," +
    "password text," +
    "nickname text," +
    "status text," +
    "status_message text," +
    "avatar text);"

  override def onCreate(db: SQLiteDatabase) {
    db.execSQL(CREATE_TABLE_USERS)
  }

  override def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
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
        cursor.getString(6))
    }
    cursor.close()
    db.close()
    userInfo
  }

  def updateUserDetail(username: String, detail: String, newDetail: String) {
    val db = this.getReadableDatabase
    val query = "UPDATE users SET " + detail + "='" + newDetail + "' WHERE username='" +
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
    profiles
  }
}
