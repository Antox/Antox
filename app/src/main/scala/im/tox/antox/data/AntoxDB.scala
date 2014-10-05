package im.tox.antox.data

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.preference.PreferenceManager
import android.util.Log
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import java.util.HashSet
import java.util.TimeZone
import im.tox.antox.utils.Constants
import im.tox.antox.utils.Friend
import im.tox.antox.utils.FriendRequest
import im.tox.antox.utils.Message
import im.tox.antox.utils.Tuple
import im.tox.antox.utils.UserStatus
import im.tox.jtoxcore.ToxUserStatus
import AntoxDB._
import scala.collection.mutable.ArrayBuffer
//remove if not needed
import scala.collection.JavaConversions._

object AntoxDB {

  private class DatabaseHelper(context: Context, activeDatabase: String) extends SQLiteOpenHelper(context,
    activeDatabase, null, Constants.DATABASE_VERSION) {

    var CREATE_TABLE_FRIENDS: String = "CREATE TABLE IF NOT EXISTS friends" + " (tox_key text primary key, " +
      "username text, " +
      "status text, " +
      "note text, " +
      "alias text, " +
      "isonline boolean, " +
      "isblocked boolean);"

    var CREATE_TABLE_MESSAGES: String = "CREATE TABLE IF NOT EXISTS messages" + " ( _id integer primary key , " +
      "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
      "message_id integer, " +
      "tox_key text, " +
      "message text, " +
      "has_been_received boolean, " +
      "has_been_read boolean, " +
      "successfully_sent boolean, " +
      "size integer, " +
      "type int, " +
      "FOREIGN KEY(tox_key) REFERENCES friends(tox_key))"

    var CREATE_TABLE_FRIEND_REQUESTS: String = "CREATE TABLE IF NOT EXISTS friend_requests" + " ( _id integer primary key, " +
      "tox_key text, " +
      "message text)"

    def onCreate(mDb: SQLiteDatabase) {
      mDb.execSQL(CREATE_TABLE_FRIENDS)
      mDb.execSQL(CREATE_TABLE_FRIEND_REQUESTS)
      mDb.execSQL(CREATE_TABLE_MESSAGES)
    }

    override def onUpgrade(mDb: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
      mDb.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_FRIENDS)
      mDb.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_CHAT_LOGS)
      mDb.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_FRIEND_REQUEST)
      onCreate(mDb)
    }
  }
}

class AntoxDB(ctx: Context) {

  private var mDbHelper: DatabaseHelper = _

  private var mDb: SQLiteDatabase = _

  val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)

  private var activeDatabase: String = preferences.getString("active_account", "")

  def open(writeable: Boolean): AntoxDB = {
    mDbHelper = new DatabaseHelper(ctx, activeDatabase)
    mDb = if (writeable) mDbHelper.getWritableDatabase else mDbHelper.getReadableDatabase
    this
  }

  def close() {
    mDbHelper.close()
  }

  private def isColumnInTable(mDb: SQLiteDatabase, table: String, column: String): Boolean = {
    try {
      val cursor = mDb.rawQuery("SELECT * FROM " + table + " LIMIT 0", null)
      if (cursor.getColumnIndex(column) == -1) {
        false
      } else {
        true
      }
    } catch {
      case e: Exception => false
    }
  }

  def addFriend(key: String,
    message: String,
    alias: String,
    username: String) {
    this.open(true)
    val parsedUsername = if (username.contains("@")) {
      username.substring(0, username.indexOf("@"))
    } else if (username == null || username.length == 0) {
      key.substring(0, 7)
    } else {
      username
    }
    val values = new ContentValues()
    values.put(Constants.COLUMN_NAME_KEY, key)
    values.put(Constants.COLUMN_NAME_STATUS, "0")
    values.put(Constants.COLUMN_NAME_NOTE, message)
    values.put(Constants.COLUMN_NAME_USERNAME, username)
    values.put(Constants.COLUMN_NAME_ISONLINE, false)
    values.put(Constants.COLUMN_NAME_ALIAS, alias)
    values.put(Constants.COLUMN_NAME_ISBLOCKED, false)
    mDb.insert(Constants.TABLE_FRIENDS, null, values)
    this.close()
  }

  def addFileTransfer(key: String,
    path: String,
    fileNumber: Int,
    size: Int,
    sending: Boolean): Long = {
    this.open(true)
    val values = new ContentValues()
    values.put(Constants.COLUMN_NAME_KEY, key)
    values.put(Constants.COLUMN_NAME_MESSAGE, path)
    values.put(Constants.COLUMN_NAME_MESSAGE_ID, fileNumber: java.lang.Integer)
    values.put(Constants.COLUMN_NAME_HAS_BEEN_RECEIVED, false)
    values.put(Constants.COLUMN_NAME_HAS_BEEN_READ, false)
    values.put(Constants.COLUMN_NAME_SUCCESSFULLY_SENT, false)
    if (sending) {
      values.put("type", Constants.MESSAGE_TYPE_FILE_TRANSFER: java.lang.Integer)
    } else {
      values.put("type", Constants.MESSAGE_TYPE_FILE_TRANSFER_FRIEND: java.lang.Integer)
    }
    values.put("size", size: java.lang.Integer)
    val id = mDb.insert(Constants.TABLE_CHAT_LOGS, null, values)
    this.close()
    id
  }

  def fileTransferStarted(key: String, fileNumber: Int) {
    this.open(false)
    val query = "UPDATE messages SET " + Constants.COLUMN_NAME_SUCCESSFULLY_SENT +
      " = 1 WHERE (type == 3 OR type == 4) AND message_id == " +
      fileNumber +
      " AND tox_key = '" +
      key +
      "'"
    mDb.execSQL(query)
    this.close()
  }

  def addFriendRequest(key: String, message: String) {
    this.open(true)
    val values = new ContentValues()
    values.put(Constants.COLUMN_NAME_KEY, key)
    values.put(Constants.COLUMN_NAME_MESSAGE, message)
    mDb.insert(Constants.TABLE_FRIEND_REQUEST, null, values)
    this.close()
  }

  def addMessage(message_id: Int,
    key: String,
    message: String,
    has_been_received: Boolean,
    has_been_read: Boolean,
    successfully_sent: Boolean,
    `type`: Int) {
    this.open(true)
    val values = new ContentValues()
    values.put(Constants.COLUMN_NAME_MESSAGE_ID, message_id: java.lang.Integer)
    values.put(Constants.COLUMN_NAME_KEY, key)
    values.put(Constants.COLUMN_NAME_MESSAGE, message)
    values.put(Constants.COLUMN_NAME_HAS_BEEN_RECEIVED, has_been_received)
    values.put(Constants.COLUMN_NAME_HAS_BEEN_READ, has_been_read)
    values.put(Constants.COLUMN_NAME_SUCCESSFULLY_SENT, successfully_sent)
    values.put("type", `type`: java.lang.Integer)
    mDb.insert(Constants.TABLE_CHAT_LOGS, null, values)
    this.close()
  }

  def getUnreadCounts(): Map[String, Integer] = {
    this.open(false)
    val map = scala.collection.mutable.Map.empty[String, Integer]
    val selectQuery = "SELECT friends.tox_key, COUNT(messages._id) " + "FROM messages " +
      "JOIN friends ON friends.tox_key = messages.tox_key " +
      "WHERE messages.has_been_read == 0 AND (messages.type == 2 OR messages.type == 4)" +
      "GROUP BY friends.tox_key"
    val cursor = mDb.rawQuery(selectQuery, null)
    if (cursor.moveToFirst()) {
      do {
        val key = cursor.getString(0)
        val count = cursor.getInt(1).intValue
        map.put(key, count)
      } while (cursor.moveToNext());
    }
    cursor.close()
    this.close()
    map.toMap
  }

  def getFilePath(key: String, fileNumber: Int): String = {
    this.open(false)
    var path = ""
    val selectQuery = "SELECT message FROM messages WHERE tox_key = '" + key +
      "' AND (type == 3 OR type == 4) AND message_id == " +
      fileNumber
    val cursor = mDb.rawQuery(selectQuery, null)
    Log.d("getFilePath count: ", java.lang.Integer.toString(cursor.getCount) + " filenumber: " +
      fileNumber)
    if (cursor.moveToFirst()) {
      path = cursor.getString(0)
    }
    cursor.close()
    this.close()
    path
  }

  def getFileId(key: String, fileNumber: Int): Int = {
    this.open(false)
    var id = -1
    val selectQuery = "SELECT _id FROM messages WHERE tox_key = '" + key +
      "' AND (type == 3 OR type == 4) AND message_id == " +
      fileNumber
    val cursor = mDb.rawQuery(selectQuery, null)
    if (cursor.moveToFirst()) {
      id = cursor.getInt(0)
    }
    cursor.close()
    this.close()
    id
  }

  def clearFileNumbers() {
    this.open(false)
    val query = "UPDATE messages SET message_id = -1 WHERE (type == 3 OR type == 4)"
    mDb.execSQL(query)
    this.close()
  }

  def clearFileNumber(key: String, fileNumber: Int) {
    this.open(false)
    val query = "UPDATE messages SET message_id = -1 WHERE (type == 3 OR type == 4) AND message_id == " +
      fileNumber +
      " AND tox_key = '" +
      key +
      "'"
    mDb.execSQL(query)
    this.close()
  }

  def fileFinished(key: String, fileNumber: Int) {
    Log.d("AntoxDB", "fileFinished")
    this.open(false)
    val query = "UPDATE messages SET " + Constants.COLUMN_NAME_HAS_BEEN_RECEIVED +
      "=1, message_id = -1 WHERE (type == 3 OR type == 4) AND message_id == " +
      fileNumber +
      " AND tox_key = '" +
      key +
      "'"
    mDb.execSQL(query)
    this.close()
  }

  def getLastMessages(): Map[String, (String, Timestamp)] = {
    this.open(false)
    val map = scala.collection.mutable.Map.empty[String, (String, Timestamp)]
    val selectQuery = "SELECT tox_key, message, timestamp FROM messages WHERE _id IN (" +
      "SELECT MAX(_id) " +
      "FROM messages WHERE (type == 1 OR type == 2) " +
      "GROUP BY tox_key)"
    val cursor = mDb.rawQuery(selectQuery, null)
    if (cursor.moveToFirst()) {
      do {
        val key = cursor.getString(0)
        val message = cursor.getString(1)
        val timestamp = Timestamp.valueOf(cursor.getString(2))
        map.put(key, (message, timestamp))
      } while (cursor.moveToNext());
    }
    cursor.close()
    this.close()
    map.toMap
  }

  def getMessageList(key: String, actionMessages: Boolean): ArrayList[Message] = {
    this.open(false)
    val messageList = new ArrayList[Message]()
    var selectQuery: String = null
    if (key == "") {
      selectQuery = "SELECT * FROM " + Constants.TABLE_CHAT_LOGS + " ORDER BY " +
        Constants.COLUMN_NAME_TIMESTAMP +
        " DESC"
    } else {
      var act: String = null
      act = if (actionMessages) "" else "AND (type == 1 OR type == 2 OR type == 3 OR type == 4) "
      selectQuery = "SELECT * FROM " + Constants.TABLE_CHAT_LOGS + " WHERE " +
        Constants.COLUMN_NAME_KEY +
        " = '" +
        key +
        "' " +
        act +
        "ORDER BY " +
        Constants.COLUMN_NAME_TIMESTAMP +
        " ASC"
    }
    val cursor = mDb.rawQuery(selectQuery, null)
    if (cursor.moveToFirst()) {
      do {
        val id = cursor.getInt(0)
        val time = Timestamp.valueOf(cursor.getString(1))
        val message_id = cursor.getInt(2)
        val k = cursor.getString(3)
        val m = cursor.getString(4)
        val received = cursor.getInt(5) > 0
        val read = cursor.getInt(6) > 0
        val sent = cursor.getInt(7) > 0
        val size = cursor.getInt(8)
        val `type` = cursor.getInt(9)
        messageList.add(new Message(id, message_id, k, m, received, read, sent, time, size, `type`))
      } while (cursor.moveToNext());
    }
    cursor.close()
    this.close()
    messageList
  }

  def getMessageIds(key: String, actionMessages: Boolean): HashSet[Integer] = {
    this.open(false)
    var selectQuery: String = null
    val idSet = new HashSet[Integer]()
    if (key == null || key == "") {
      selectQuery = "SELECT * FROM " + Constants.TABLE_CHAT_LOGS + " ORDER BY " +
        Constants.COLUMN_NAME_TIMESTAMP +
        " DESC"
    } else {
      var act: String = null
      act = if (actionMessages) "" else "AND (type == 1 OR type == 2 OR type == 3 OR type == 4) "
      selectQuery = "SELECT * FROM " + Constants.TABLE_CHAT_LOGS + " WHERE " +
        Constants.COLUMN_NAME_KEY +
        " = '" +
        key +
        "' " +
        act +
        "ORDER BY " +
        Constants.COLUMN_NAME_TIMESTAMP +
        " ASC"
    }
    val cursor = mDb.rawQuery(selectQuery, null)
    if (cursor.moveToFirst()) {
      do {
        val id = cursor.getInt(0)
        idSet.add(id)
      } while (cursor.moveToNext());
    }
    cursor.close()
    this.close()
    idSet
  }

  def getMessageCursor(key: String, actionMessages: Boolean): Cursor = {
    this.open(false)
    var selectQuery: String = null
    if (key == null || key == "") {
      selectQuery = "SELECT * FROM " + Constants.TABLE_CHAT_LOGS + " ORDER BY " +
        Constants.COLUMN_NAME_TIMESTAMP +
        " DESC"
    } else {
      var act: String = null
      act = if (actionMessages) "" else "AND (type == 1 OR type == 2 OR type == 3 OR type == 4) "
      selectQuery = "SELECT * FROM " + Constants.TABLE_CHAT_LOGS + " WHERE " +
        Constants.COLUMN_NAME_KEY +
        " = '" +
        key +
        "' " +
        act +
        "ORDER BY " +
        Constants.COLUMN_NAME_TIMESTAMP +
        " ASC"
    }
    val cursor = mDb.rawQuery(selectQuery, null)
    cursor
  }

  def getRecentCursor(): Cursor = {
    this.open(false)
    val selectQuery = "SELECT f.tox_key, f.username, f.isonline, f.status, m1.timestamp, m1.message, COUNT(m2.tox_key) as unreadCount, m1._id " +
      "FROM " +
      Constants.TABLE_FRIENDS +
      " f " +
      "INNER JOIN " +
      Constants.TABLE_CHAT_LOGS +
      " m1 ON (f.tox_key = m1.tox_key) " +
      "LEFT OUTER JOIN (SELECT tox_key FROM " +
      Constants.TABLE_CHAT_LOGS +
      " WHERE ((type = 2 OR type = 4) AND has_been_read = 0)) " +
      "m2 ON (f.tox_key = m2.tox_key) " +
      "WHERE m1._id = (SELECT MAX(_id) FROM " +
      Constants.TABLE_CHAT_LOGS +
      " WHERE (tox_key = f.tox_key AND (type = 1 OR type = 2))) " +
      "GROUP BY f.tox_key " +
      "ORDER BY m1._id DESC"
    val cursor = mDb.rawQuery(selectQuery, null)
    cursor
  }

  def getFriendRequestsList(): ArrayList[FriendRequest] = {
    this.open(false)
    val friendRequests = new ArrayList[FriendRequest]()
    val projection = Array(Constants.COLUMN_NAME_KEY, Constants.COLUMN_NAME_MESSAGE)
    val cursor = mDb.query(Constants.TABLE_FRIEND_REQUEST, projection, null, null, null, null, null)
    if (cursor.moveToFirst()) {
      do {
        val key = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_KEY))
        val message = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_MESSAGE))
        friendRequests.add(new FriendRequest(key, message))
      } while (cursor.moveToNext());
    }
    cursor.close()
    this.close()
    friendRequests
  }

  def getUnsentMessageList(): Array[Message] = {
    this.open(false)
    val messageList = new ArrayList[Message]()
    val selectQuery = "SELECT * FROM " + Constants.TABLE_CHAT_LOGS + " WHERE " +
      Constants.COLUMN_NAME_SUCCESSFULLY_SENT +
      "=0 AND type == 1 ORDER BY " +
      Constants.COLUMN_NAME_TIMESTAMP +
      " ASC"
    val cursor = mDb.rawQuery(selectQuery, null)
    if (cursor.moveToFirst()) {
      do {
        val id = cursor.getInt(0)
        val time = Timestamp.valueOf(cursor.getString(1))
        val m_id = cursor.getInt(2)
        Log.d("UNSENT MESAGE ID: ", "" + m_id)
        val k = cursor.getString(3)
        val m = cursor.getString(4)
        val received = cursor.getInt(5) > 0
        val read = cursor.getInt(6) > 0
        val sent = cursor.getInt(7) > 0
        val size = cursor.getInt(8)
        val `type` = cursor.getInt(9)
        messageList.add(new Message(id, m_id, k, m, received, read, sent, time, size, `type`))
      } while (cursor.moveToNext());
    }
    cursor.close()
    this.close()
    messageList.toArray(new Array[Message](messageList.size))
  }

  def updateUnsentMessage(message_id: Integer, id: Integer) {
    this.open(true)
    val values = new ContentValues()
    values.put(Constants.COLUMN_NAME_SUCCESSFULLY_SENT, "1")
    values.put("type", 1: java.lang.Integer)
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
    val date = new Date()
    values.put(Constants.COLUMN_NAME_TIMESTAMP, dateFormat.format(date))
    values.put(Constants.COLUMN_NAME_MESSAGE_ID, message_id)
    mDb.update(Constants.TABLE_CHAT_LOGS, values, "_id" + "=" + id + " AND " +
      Constants.COLUMN_NAME_SUCCESSFULLY_SENT +
      "=0", null)
    this.close()
  }

  def setMessageReceived(receipt: Int): String = {
    this.open(true)
    val query = "UPDATE " + Constants.TABLE_CHAT_LOGS + " SET " + Constants.COLUMN_NAME_HAS_BEEN_RECEIVED +
      "=1 WHERE " +
      Constants.COLUMN_NAME_MESSAGE_ID +
      "=" +
      receipt +
      " AND " +
      Constants.COLUMN_NAME_SUCCESSFULLY_SENT +
      "=1 AND type =1"
    mDb.execSQL(query)
    val selectQuery = "SELECT * FROM " + Constants.TABLE_CHAT_LOGS + " WHERE " +
      Constants.COLUMN_NAME_MESSAGE_ID +
      "=" +
      receipt +
      " AND " +
      Constants.COLUMN_NAME_SUCCESSFULLY_SENT +
      "=1 AND type=1"
    val cursor = mDb.rawQuery(selectQuery, null)
    var k = ""
    if (cursor.moveToFirst()) {
      k = cursor.getString(3)
    }
    cursor.close()
    this.close()
    k
  }

  def markIncomingMessagesRead(key: String) {
    this.open(true)
    val query = "UPDATE " + Constants.TABLE_CHAT_LOGS + " SET " + Constants.COLUMN_NAME_HAS_BEEN_READ +
      "=1 WHERE " +
      Constants.COLUMN_NAME_KEY +
      "='" +
      key +
      "' AND (type == 2 OR type == 4)"
    mDb.execSQL(query)
    this.close()
    Log.d("", "marked incoming messages as read")
  }

  def deleteMessage(id: Int) {
    this.open(true)
    val query = "DELETE FROM " + Constants.TABLE_CHAT_LOGS + " WHERE _id == " +
      id
    mDb.execSQL(query)
    this.close()
    Log.d("", "Deleted message")
  }

  def getFriendList(): Array[Friend] = {
    this.open(false)
    val friendList = new ArrayBuffer[Friend]()
    val selectQuery = "SELECT  * FROM " + Constants.TABLE_FRIENDS
    val cursor = mDb.rawQuery(selectQuery, null)
    if (cursor.moveToFirst()) {
      do {
        var name = cursor.getString(1)
        val key = cursor.getString(0)
        val status = cursor.getString(2)
        val note = cursor.getString(3)
        var alias = cursor.getString(4)
        val isOnline = cursor.getInt(5) != 0
        val isBlocked = cursor.getInt(6) > 0
        if (alias == null) alias = ""
        if (alias != "") name = alias else if (name == "") name = key.substring(0, 7)
        if (!isBlocked) (friendList += (new Friend(isOnline, name, status, note, key, alias)))
      } while (cursor.moveToNext());
    }
    cursor.close()
    this.close()
    friendList.toArray
  }

  def doesFriendExist(key: String): Boolean = {
    this.open(false)
    val mCount = mDb.rawQuery("SELECT count(*) FROM " + Constants.TABLE_FRIENDS + " WHERE " +
      Constants.COLUMN_NAME_KEY +
      "='" +
      key +
      "'", null)
    mCount.moveToFirst()
    val count = mCount.getInt(0)
    if (count > 0) {
      mCount.close()
      this.close()
      return true
    }
    mCount.close()
    this.close()
    false
  }

  def setAllOffline() {
    this.open(false)
    val values = new ContentValues()
    values.put(Constants.COLUMN_NAME_ISONLINE, "0")
    mDb.update(Constants.TABLE_FRIENDS, values, Constants.COLUMN_NAME_ISONLINE + "='1'", null)
    this.close()
  }

  def deleteFriend(key: String) {
    this.open(false)
    mDb.delete(Constants.TABLE_FRIENDS, Constants.COLUMN_NAME_KEY + "='" + key + "'", null)
    this.close()
  }

  def deleteFriendRequest(key: String) {
    this.open(false)
    mDb.delete(Constants.TABLE_FRIEND_REQUEST, Constants.COLUMN_NAME_KEY + "='" + key + "'", null)
    this.close()
  }

  def getFriendRequestMessage(key: String): String = {
    this.open(false)
    val selectQuery = "SELECT message FROM " + Constants.TABLE_FRIEND_REQUEST +
      " WHERE tox_key='" +
      key +
      "'"
    val cursor = mDb.rawQuery(selectQuery, null)
    var message = ""
    if (cursor.moveToFirst()) {
      message = cursor.getString(0)
    }
    cursor.close()
    this.close()
    message
  }

  def deleteChat(key: String) {
    this.open(false)
    mDb.delete(Constants.TABLE_CHAT_LOGS, Constants.COLUMN_NAME_KEY + "='" + key + "'", null)
    this.close()
  }

  def updateFriendName(key: String, newName: String) {
    this.open(false)
    val values = new ContentValues()
    values.put(Constants.COLUMN_NAME_USERNAME, newName)
    mDb.update(Constants.TABLE_FRIENDS, values, Constants.COLUMN_NAME_KEY + "='" + key + "'", null)
    this.close()
  }

  def updateStatusMessage(key: String, newMessage: String) {
    this.open(false)
    val values = new ContentValues()
    values.put(Constants.COLUMN_NAME_NOTE, newMessage)
    mDb.update(Constants.TABLE_FRIENDS, values, Constants.COLUMN_NAME_KEY + "='" + key + "'", null)
    this.close()
  }

  def updateUserStatus(key: String, status: ToxUserStatus) {
    this.open(false)
    val values = new ContentValues()
    val tmp = UserStatus.getStringFromToxUserStatus(status)
    values.put(Constants.COLUMN_NAME_STATUS, tmp)
    mDb.update(Constants.TABLE_FRIENDS, values, Constants.COLUMN_NAME_KEY + "='" + key + "'", null)
    this.close()
  }

  def updateUserOnline(key: String, online: Boolean) {
    this.open(false)
    val values = new ContentValues()
    values.put(Constants.COLUMN_NAME_ISONLINE, online)
    mDb.update(Constants.TABLE_FRIENDS, values, Constants.COLUMN_NAME_KEY + "='" + key + "'", null)
    this.close()
  }

  def getFriendDetails(key: String): Array[String] = {
    var details = Array[String](null, null, null)
    this.open(false)
    val selectQuery = "SELECT * FROM " + Constants.TABLE_FRIENDS + " WHERE " +
      Constants.COLUMN_NAME_KEY +
      "='" +
      key +
      "'"
    val cursor = mDb.rawQuery(selectQuery, null)
    if (cursor.moveToFirst()) {
      do {
        var name = cursor.getString(1)
        val note = cursor.getString(3)
        val alias = cursor.getString(4)
        if (name == null) name = ""
        if (name == "") name = key.substring(0, 7)
        details = Array(name, alias, note)
      } while (cursor.moveToNext());
    }
    cursor.close()
    this.close()
    details
  }

  def updateAlias(alias: String, key: String) {
    this.open(true)
    val query = "UPDATE " + Constants.TABLE_FRIENDS + " SET " + Constants.COLUMN_NAME_ALIAS +
      "='" +
      alias +
      "' WHERE " +
      Constants.COLUMN_NAME_KEY +
      "='" +
      key +
      "'"
    mDb.execSQL(query)
    this.close()
  }

  def isFriendBlocked(key: String): Boolean = {
    var isBlocked = false
    this.open(false)
    val selectQuery = "SELECT isBlocked FROM " + Constants.TABLE_FRIENDS + " WHERE " +
      Constants.COLUMN_NAME_KEY +
      "='" +
      key +
      "'"
    val cursor = mDb.rawQuery(selectQuery, null)
    if (cursor.moveToFirst()) {
      isBlocked = cursor.getInt(0) > 0
    }
    cursor.close()
    this.close()
    isBlocked
  }
}
