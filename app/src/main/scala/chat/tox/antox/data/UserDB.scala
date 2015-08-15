
package chat.tox.antox.data

import java.util

import android.content.{ContentValues, Context}
import android.database.Cursor
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import android.preference.PreferenceManager
import android.util.Log
import chat.tox.antox.R
import chat.tox.antox.data.UserDB.DatabaseHelper
import chat.tox.antox.utils.DatabaseConstants._
import chat.tox.antox.utils.{BriteScalaDatabase, DatabaseUtil}
import chat.tox.antox.wrapper.UserInfo
import com.squareup.sqlbrite.SqlBrite
import rx.lang.scala.Observable

import scala.collection.mutable.ArrayBuffer

object UserDB {

  val databaseName = "userdb"
  val sqlBrite = SqlBrite.create()

  class DatabaseHelper(context: Context) extends SQLiteOpenHelper(context, databaseName, null, USER_DATABASE_VERSION) {
    private val CREATE_TABLE_USERS: String =
      s"""CREATE TABLE IF NOT EXISTS $TABLE_USERS ( _id integer primary key ,
         |$COLUMN_NAME_USERNAME text,
         |$COLUMN_NAME_PASSWORD text,
         |$COLUMN_NAME_NICKNAME text,
         |$COLUMN_NAME_STATUS text,
         |$COLUMN_NAME_STATUS_MESSAGE text,
         |$COLUMN_NAME_AVATAR text,
         |$COLUMN_NAME_LOGGING_ENABLED boolean);""".stripMargin

    override def onCreate(db: SQLiteDatabase) {
      db.execSQL(CREATE_TABLE_USERS)
    }

    override def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int): Unit = {
      Log.w("UserDB", "Upgrading UserDB from version " + oldVersion + " to " + newVersion)

      for (currVersion <- oldVersion to newVersion) {
        currVersion match {
          case 1 =>
            if (!DatabaseUtil.isColumnInTable(db, TABLE_USERS, COLUMN_NAME_AVATAR))
              db.execSQL(s"ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_NAME_AVATAR text")
          case 2 =>
            db.execSQL(s"ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_NAME_LOGGING_ENABLED integer")
            db.execSQL(s"UPDATE $TABLE_USERS SET $COLUMN_NAME_LOGGING_ENABLED = $TRUE")
          case _ =>
        }
      }
    }

  }
}

class UserDB(ctx: Context) {

  case class NotLoggedInException(message: String = "Invalid request. No active user found.") extends RuntimeException

  private var mDbHelper: DatabaseHelper = _

  private var mDb: BriteScalaDatabase = _

  val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)

  var activeUser: Option[String] = None
  def getActiveUser = activeUser.getOrElse(throw new NotLoggedInException())

  mDbHelper = new DatabaseHelper(ctx)
  mDb = new BriteScalaDatabase(AntoxDB.sqlBrite.wrapDatabaseHelper(mDbHelper))

  def close() {
    mDbHelper.close()
  }

  def login(username: String): Unit = {
    activeUser = Some(username)
    val activeUserDetails = getActiveUserDetails

    val editor = preferences.edit()
    editor.putString("active_account", username)
    editor.putString("nickname", activeUserDetails.nickname)
    editor.putString("password", activeUserDetails.password)
    editor.putString("status", activeUserDetails.status)
    editor.putString("status_message", activeUserDetails.statusMessage)
    editor.putString("avatar", activeUserDetails.avatarName)
    editor.putBoolean("logging_enabled", activeUserDetails.loggingEnabled)
    editor.commit()
  }

  def loggedIn = activeUser.isDefined

  def logout(): Unit = {
    activeUser = None
  }

  def addUser(username: String, toxID: String, password: String) {
    val values = new ContentValues()
    values.put(COLUMN_NAME_USERNAME, username)
    values.put(COLUMN_NAME_PASSWORD, password)
    values.put(COLUMN_NAME_NICKNAME, username)
    values.put(COLUMN_NAME_STATUS, "online")
    val defaultStatusMessage = ctx.getResources.getString(R.string.pref_default_status_message)
    values.put(COLUMN_NAME_STATUS_MESSAGE, defaultStatusMessage)
    values.put(COLUMN_NAME_AVATAR, "")
    values.put(COLUMN_NAME_LOGGING_ENABLED, true)
    mDb.insert(TABLE_USERS, values)

    val editor = preferences.edit()
    editor.putString("tox_id", toxID)
    editor.putBoolean("logging_enabled", true)
    editor.putBoolean("autostart", true)
    editor.commit()
  }

  def doesUserExist(username: String): Boolean = {
    val cursor = mDb.query(
      s"""SELECT count(*)
         |FROM $TABLE_USERS
         |WHERE $COLUMN_NAME_USERNAME='$username'""".stripMargin)

    cursor.moveToFirst()
    val count = cursor.getInt(0)
    cursor.close()
    count > 0
  }

  private def userDetailsQuery(username: String): String =
    s"""SELECT *
       |FROM $TABLE_USERS
       |WHERE $COLUMN_NAME_USERNAME='$username'""".stripMargin

  private def userInfoFromCursor(cursor: Cursor): UserInfo = {
    var userInfo: UserInfo = null
    if (cursor.moveToFirst()) {
      userInfo = new UserInfo(
        username = cursor.getString(1),
        password = cursor.getString(2),
        nickname = cursor.getString(3),
        status = cursor.getString(4),
        statusMessage = cursor.getString(5),
        loggingEnabled = cursor.getInt(7) > 0,
        avatarName = cursor.getString(6))
    }
    userInfo
  }

  def getActiveUserDetails: UserInfo =
    getUserDetails(getActiveUser)

  def getUserDetails(username: String): UserInfo = {
    val query = userDetailsQuery(username)

    val cursor = mDb.query(query)
    val userInfo: UserInfo = userInfoFromCursor(cursor)
    cursor.close()
    userInfo
  }

  def activeUserDetailsObservable(): Observable[UserInfo] =
    userDetailsObservable(getActiveUser)

  def userDetailsObservable(username: String): Observable[UserInfo] = {
    val query = userDetailsQuery(username)

    mDb.createQuery(TABLE_USERS, query).map(query => {
      val cursor = query.run()
      val userInfo: UserInfo = userInfoFromCursor(cursor)

      cursor.close()
      userInfo
    })
  }

  def updateActiveUserDetail(detail: String, newDetail: String) = {
    if (!preferences.getString(detail, "").equals(newDetail)) {
      preferences.edit().putString(detail, newDetail).apply()
    }
    updateUserDetail(getActiveUser, detail, newDetail)
  }

  def updateActiveUserDetail(detail: String, newDetail: Boolean) = {
    if (!preferences.getBoolean(detail, false) == newDetail) {
      preferences.edit().putBoolean(detail, newDetail).apply()
    }
    updateUserDetail(getActiveUser, detail, newDetail)
  }

  private def updateUserDetail(username: String, detail: String, newDetail: String) {
    val whereClause = s"$COLUMN_NAME_USERNAME='$username'"
    mDb.update(TABLE_USERS, contentValue(detail, newDetail), whereClause)
  }

  def updateUserDetail(username: String, detail: String, newDetail: Boolean) {
    val whereClause = s"$COLUMN_NAME_USERNAME='$username'"
    mDb.update(TABLE_USERS, contentValue(detail, if (newDetail) TRUE else FALSE), whereClause)
  }

  def numUsers(): Int = {
    val cursor = mDb.query(s"SELECT count(*) FROM $TABLE_USERS")
    cursor.moveToFirst()
    val count = cursor.getInt(0)
    cursor.close()
    count
  }

  def getAllProfiles: ArrayBuffer[String] = {
    val profiles = new ArrayBuffer[String]()
    val query = s"SELECT $COLUMN_NAME_USERNAME FROM $TABLE_USERS"
    val cursor = mDb.query(query)
    if (cursor.moveToFirst()) {
      do {
        profiles += cursor.getString(0)
      } while (cursor.moveToNext())
    }
    cursor.close()
    profiles
  }
}
