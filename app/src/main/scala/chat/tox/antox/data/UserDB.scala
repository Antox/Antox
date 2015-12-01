
package chat.tox.antox.data

import android.content.{ContentValues, Context}
import android.database.Cursor
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import android.preference.PreferenceManager
import chat.tox.antox.R
import chat.tox.antox.data.UserDB.DatabaseHelper
import chat.tox.antox.toxme.ToxMeName
import chat.tox.antox.utils.DatabaseConstants._
import chat.tox.antox.utils.{AntoxLog, BriteScalaDatabase, DatabaseUtil}
import chat.tox.antox.wrapper.{ToxAddress, UserInfo}
import com.squareup.sqlbrite.SqlBrite
import im.tox.tox4j.core.data.{ToxStatusMessage, ToxNickname}
import org.scaloid.common.LoggerTag
import rx.lang.scala.Observable

import scala.collection.mutable.ArrayBuffer

object UserDB {

  private val TAG = LoggerTag(getClass.getSimpleName)

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
      AntoxLog.info(s"Upgrading UserDB from version $oldVersion to $newVersion", TAG)

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
    editor.putString("nickname", new String(activeUserDetails.nickname.value))
    editor.putString("password", activeUserDetails.password)
    editor.putString("status", activeUserDetails.status)
    editor.putString("status_message", new String(activeUserDetails.statusMessage.value))
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
    val query = s"""SELECT count(*)
                   |FROM $TABLE_USERS
                   |WHERE $COLUMN_NAME_PROFILE_NAME='$username'""".stripMargin

    val exists = mDb.query(query).use { cursor =>
      cursor.moveToFirst() && cursor.getInt(0) > 0
    }

    exists
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
        val toxMeName = new ToxMeName(cursor.getString(COLUMN_NAME_PROFILE_NAME), if (domain.isEmpty) None else Some(domain))

        Some(new UserInfo(
          toxMeName = toxMeName,
          password = cursor.getString(COLUMN_NAME_PASSWORD),
          nickname = ToxNickname.unsafeFromValue(cursor.getString(COLUMN_NAME_NICKNAME).getBytes),
          status = cursor.getString(COLUMN_NAME_STATUS),
          statusMessage = ToxStatusMessage.unsafeFromValue(cursor.getString(COLUMN_NAME_STATUS_MESSAGE).getBytes),
          loggingEnabled = cursor.getBoolean(COLUMN_NAME_LOGGING_ENABLED),
          avatarName = cursor.getString(COLUMN_NAME_AVATAR)))
      } else {
        None
      }

    userInfo
  }

  def getActiveUserDetails: UserInfo =
    getUserDetails(getActiveUser).get //fail fast

  def getUserDetails(username: String): Option[UserInfo] = {
    val query = userDetailsQuery(username)

    val userInfo = mDb.query(query).use { cursor =>
      userInfoFromCursor(cursor)
    }

    userInfo
  }

  def activeUserDetailsObservable(): Observable[UserInfo] =
    userDetailsObservable(getActiveUser)

  def userDetailsObservable(username: String): Observable[UserInfo] = {
    val query = userDetailsQuery(username)

    mDb.createQuery(TABLE_USERS, query).map(closedCursor => {
      val userInfo = closedCursor.use { cursor =>
        userInfoFromCursor(cursor)
      }

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
    mDb.query(s"SELECT count(*) FROM $TABLE_USERS").use { cursor =>
      cursor.moveToFirst()
      val count = cursor.getInt(0)

      count
    }
  }

  def getAllProfiles: ArrayBuffer[String] = {
    val profiles = new ArrayBuffer[String]()
    val query = s"SELECT $COLUMN_NAME_PROFILE_NAME FROM $TABLE_USERS"
    mDb.query(query).use { cursor =>
      if (cursor.moveToFirst()) {
        do {
          profiles += cursor.getString(0)
        } while (cursor.moveToNext())
      }
    }

    profiles
  }
}
