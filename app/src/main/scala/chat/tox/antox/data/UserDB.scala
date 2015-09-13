
package chat.tox.antox.data

import android.content.{ContentValues, Context}
import android.database.Cursor
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import android.preference.PreferenceManager
import android.util.Log
import chat.tox.antox.R
import chat.tox.antox.data.UserDB.DatabaseHelper
import chat.tox.antox.toxme.ToxMeName
import chat.tox.antox.utils.DatabaseConstants._
import chat.tox.antox.utils.{BriteScalaDatabase, DatabaseUtil}
import chat.tox.antox.wrapper.{ToxAddress, UserInfo}
import com.squareup.sqlbrite.SqlBrite
import rx.lang.scala.Observable

import scala.collection.mutable.ArrayBuffer

object UserDB {

  val databaseName = "userdb"
  val sqlBrite = SqlBrite.create()

  class DatabaseHelper(context: Context) extends SQLiteOpenHelper(context, databaseName, null, USER_DATABASE_VERSION) {
    private val CREATE_TABLE_USERS: String =
      s"""CREATE TABLE IF NOT EXISTS $TABLE_USERS ( _id integer primary key ,
         |$COLUMN_NAME_PROFILE_NAME text,
         |$COLUMN_NAME_PASSWORD text,
         |$COLUMN_NAME_NICKNAME text,
         |$COLUMN_NAME_STATUS text,
         |$COLUMN_NAME_STATUS_MESSAGE text,
         |$COLUMN_NAME_AVATAR text,
         |$COLUMN_NAME_LOGGING_ENABLED boolean,
         |$COLUMN_NAME_TOXME_DOMAIN text);""".stripMargin


    override def onCreate(db: SQLiteDatabase) {
      db.execSQL(CREATE_TABLE_USERS)
    }

    override def onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int): Unit = {
      Log.w("UserDB", "Upgrading UserDB from version " + oldVersion + " to " + newVersion)

      for (currVersion <- oldVersion to newVersion) {
        currVersion match {
          case 1 =>
            if (!DatabaseUtil.isColumnInTable(db, TABLE_USERS, COLUMN_NAME_AVATAR)) {
              db.execSQL(s"ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_NAME_AVATAR text")
            }
          case 2 =>
            db.execSQL(s"ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_NAME_LOGGING_ENABLED integer")
            db.execSQL(s"UPDATE $TABLE_USERS SET $COLUMN_NAME_LOGGING_ENABLED = $TRUE")
          case 4 =>
            db.execSQL(s"ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_NAME_TOXME_DOMAIN text")
            db.execSQL(s"UPDATE $TABLE_USERS SET $COLUMN_NAME_TOXME_DOMAIN = 'toxme.io' ")
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

  def activeUser: Option[String] = {
    val user = preferences.getString("active_account", "")
    user match {
      case "" =>
        None
      case _ =>
        Some(user)
    }
  }

  def getActiveUser: String = activeUser.getOrElse(throw new NotLoggedInException())

  mDbHelper = new DatabaseHelper(ctx)
  mDb = new BriteScalaDatabase(AntoxDB.sqlBrite.wrapDatabaseHelper(mDbHelper))

  def close() {
    mDbHelper.close()
  }

  def login(username: String): Unit = {
    preferences.edit().putString("active_account", username).commit()

    val editor = preferences.edit()
    val activeUserDetails = getActiveUserDetails
    editor.putString("nickname", activeUserDetails.nickname)
    editor.putString("password", activeUserDetails.password)
    editor.putString("status", activeUserDetails.status)
    editor.putString("status_message", activeUserDetails.statusMessage)
    editor.putString("avatar", activeUserDetails.avatarName)
    editor.putBoolean("logging_enabled", activeUserDetails.loggingEnabled)
    activeUserDetails.toxMeName.domain match {
      case Some(domain) =>
        editor.putString("toxme_info", activeUserDetails.toxMeName.username + "@" + domain)
      case None =>
        editor.putString("toxme_info", "")
    }

    editor.commit()
  }

  def loggedIn: Boolean = activeUser.isDefined

  def logout(): Unit = {
    preferences.edit().putString("active_account", "").commit()
  }

  def addUser(toxMeName: ToxMeName, toxId: ToxAddress, password: String) {
    val values = new ContentValues()
    values.put(COLUMN_NAME_PROFILE_NAME, toxMeName.username)
    values.put(COLUMN_NAME_PASSWORD, password)
    values.put(COLUMN_NAME_NICKNAME, toxMeName.username)
    values.put(COLUMN_NAME_STATUS, "online")
    val defaultStatusMessage = ctx.getResources.getString(R.string.pref_default_status_message)
    values.put(COLUMN_NAME_STATUS_MESSAGE, defaultStatusMessage)
    values.put(COLUMN_NAME_AVATAR, "")
    values.put(COLUMN_NAME_LOGGING_ENABLED, true)
    values.put(COLUMN_NAME_TOXME_DOMAIN, toxMeName.domain.getOrElse(""))
    mDb.insert(TABLE_USERS, values)

    val editor = preferences.edit()
    editor.putString("tox_id", toxId.toString)
    editor.putBoolean("logging_enabled", true)
    editor.putBoolean("autostart", true)
    editor.commit()
  }

  def doesUserExist(username: String): Boolean = {
    val cursor = mDb.query(
      s"""SELECT count(*)
         |FROM $TABLE_USERS
         |WHERE $COLUMN_NAME_PROFILE_NAME='$username'""".stripMargin)

    cursor.moveToFirst()
    val count = cursor.getInt(0)
    cursor.close()
    count > 0
  }

  def deleteActiveUser(): Unit = {
    val profileName = getActiveUserDetails.profileName
    logout()
    val where = s"$COLUMN_NAME_PROFILE_NAME == ?"
    mDb.delete(TABLE_USERS, where, profileName)
    ctx.deleteDatabase(profileName)
  }

  private def userDetailsQuery(username: String): String =
    s"""SELECT *
       |FROM $TABLE_USERS
       |WHERE $COLUMN_NAME_PROFILE_NAME='$username'""".stripMargin

  private def userInfoFromCursor(cursor: Cursor): Option[UserInfo] = {
    val userInfo: Option[UserInfo] =
      if (cursor.moveToFirst()) {
        val domain = cursor.getString(COLUMN_NAME_TOXME_DOMAIN)
        Some(new UserInfo(
          toxMeName = new ToxMeName(
            cursor.getString(COLUMN_NAME_PROFILE_NAME), if (domain.isEmpty) None else Some(domain)),
          password = cursor.getString(COLUMN_NAME_PASSWORD),
          nickname = cursor.getString(COLUMN_NAME_NICKNAME),
          status = cursor.getString(COLUMN_NAME_STATUS),
          statusMessage = cursor.getString(COLUMN_NAME_STATUS_MESSAGE),
          loggingEnabled = cursor.getBoolean(COLUMN_NAME_LOGGING_ENABLED),
          avatarName = cursor.getString(COLUMN_NAME_AVATAR)))
      } else {
        None
      }

    userInfo
  }

  def getActiveUserDetails: UserInfo =
    getUserDetails(getActiveUser).get //fail fast (

  def getUserDetails(username: String): Option[UserInfo] = {
    val query = userDetailsQuery(username)

    val cursor = mDb.query(query)
    val userInfo = userInfoFromCursor(cursor)
    cursor.close()
    userInfo
  }

  def activeUserDetailsObservable(): Observable[UserInfo] =
    userDetailsObservable(getActiveUser)

  def userDetailsObservable(username: String): Observable[UserInfo] = {
    val query = userDetailsQuery(username)

    mDb.createQuery(TABLE_USERS, query).map(query => {
      val cursor = query.run()
      val userInfo = userInfoFromCursor(cursor)
      cursor.close()
      userInfo
    }).filter(_.isDefined).map(_.get)
  }

  def updateActiveUserDetail(detail: String, newDetail: String): Unit = {
    if (!preferences.getString(detail, "").equals(newDetail)) {
      preferences.edit().putString(detail, newDetail).apply()
    }
    updateUserDetail(getActiveUser, detail, newDetail)
  }

  def updateActiveUserDetail(detail: String, newDetail: Boolean): Unit = {
    if (!preferences.getBoolean(detail, false) == newDetail) {
      preferences.edit().putBoolean(detail, newDetail).apply()
    }
    updateUserDetail(getActiveUser, detail, newDetail)
  }

  private def updateUserDetail(username: String, detail: String, newDetail: String) {
    val whereClause = s"$COLUMN_NAME_PROFILE_NAME='$username'"
    mDb.update(TABLE_USERS, contentValue(detail, newDetail), whereClause)
  }

  def updateUserDetail(username: String, detail: String, newDetail: Boolean) {
    val whereClause = s"$COLUMN_NAME_PROFILE_NAME='$username'"
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
    val query = s"SELECT $COLUMN_NAME_PROFILE_NAME FROM $TABLE_USERS"
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
