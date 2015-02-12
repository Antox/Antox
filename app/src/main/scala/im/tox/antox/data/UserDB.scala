
package im.tox.antox.data

import java.util
import java.util.ArrayList

import android.content.{ContentValues, Context}
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
//remove if not needed

class UserDB(ctx: Context) extends SQLiteOpenHelper(ctx, "userdb", null, 1) {

  private var CREATE_TABLE_USERS: String = "CREATE TABLE IF NOT EXISTS users" + " ( _id integer primary key , " +
    "username text," +
    "password text," +
    "nickname text," +
    "status text," +
    "status_message text);"

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

  def getUserDetails(username: String): Array[String] = {
    val details = Array.ofDim[String](4)
    val db = this.getReadableDatabase
    val query = "SELECT * FROM users WHERE username='" + username + "'"
    val cursor = db.rawQuery(query, null)
    if (cursor.moveToFirst()) {
      //WHY WOULD ANY SANE MAN DO THIS
      details(0) = cursor.getString(3) //nickname
      details(1) = cursor.getString(2) //password
      details(2) = cursor.getString(4) //status
      details(3) = cursor.getString(5) //status message
    }
    cursor.close()
    db.close()
    details
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
