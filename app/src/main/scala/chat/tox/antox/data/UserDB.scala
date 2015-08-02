
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

  private var mDbHelper: DatabaseHelper = _

  private var mDb: BriteScalaDatabase = _

  val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)

  mDbHelper = new DatabaseHelper(ctx)
  mDb = new BriteScalaDatabase(AntoxDB.sqlBrite.wrapDatabaseHelper(mDbHelper))

  def close() {
    mDbHelper.close()
  }

  def addUser(username: String, password: String) {
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

  def userDetailsQuery(username: String): String =
    s"""SELECT *
       |FROM $TABLE_USERS
       |WHERE $COLUMN_NAME_USERNAME='$username'""".stripMargin

  def userInfoFromCursor(cursor: Cursor): UserInfo = {
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

  def getUserDetails(username: String): UserInfo = {
    val query = userDetailsQuery(username)

    val cursor = mDb.query(query)
    val userInfo: UserInfo = userInfoFromCursor(cursor)
    cursor.close()
    userInfo
  }

  def userDetailsObservable(username: String): Observable[UserInfo] = {
    val query = userDetailsQuery(username)

    mDb.createQuery(TABLE_USERS, query).map(query => {
      val cursor = query.run()
      val userInfo: UserInfo = userInfoFromCursor(cursor)

      cursor.close()
      userInfo
    })
  }

  def updateUserDetail(username: String, detail: String, newDetail: String) {
    val whereClause = s"$COLUMN_NAME_USERNAME='$username'"
    mDb.update(TABLE_USERS, contentValue(detail, newDetail), whereClause)
  }

  def updateUserDetail(username: String, detail: String, newDetail: Boolean) {
    val whereClause = s"$COLUMN_NAME_USERNAME='$username'"
    mDb.update(TABLE_USERS, contentValue(detail, if (newDetail) TRUE else FALSE), whereClause)
  }

  def doUsersExist(): Boolean = {
    val cursor = mDb.query(s"SELECT count(*) FROM $TABLE_USERS")
    cursor.moveToFirst()
    val count = cursor.getInt(0)
    cursor.close()
    count > 0
  }

  def getAllProfiles: util.ArrayList[String] = {
    val profiles = new util.ArrayList[String]()
    val query = s"SELECT $COLUMN_NAME_USERNAME FROM $TABLE_USERS"
    val cursor = mDb.query(query)
    if (cursor.moveToFirst()) {
      do {
        profiles.add(cursor.getString(0))
      } while (cursor.moveToNext())
    }
    cursor.close()
    profiles
  }
}
