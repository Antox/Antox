package im.tox.antox.data

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util
import java.util.{Date, TimeZone}

import android.content.{ContentValues, Context}
import android.database.Cursor
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import android.preference.PreferenceManager
import android.util.Log
import im.tox.antox.data.AntoxDB._
import im.tox.antox.utils._
import im.tox.antox.wrapper.FileKind.AVATAR
import im.tox.antox.wrapper.MessageType.MessageType
import im.tox.antox.wrapper._
import im.tox.tox4j.core.enums.ToxStatus

import scala.collection.mutable.ArrayBuffer

object AntoxDB {

  private class DatabaseHelper(context: Context, activeDatabase: String) extends SQLiteOpenHelper(context,
    activeDatabase, null, Constants.DATABASE_VERSION) {

    var CREATE_TABLE_FRIENDS: String = "CREATE TABLE IF NOT EXISTS friends" + " (tox_key text primary key, " +
      "username text, " +
      "status text, " +
      "note text, " +
      "alias text, " +
      "isonline boolean, " +
      "isblocked boolean, " +
      "avatar text, " +
      "received_avatar boolean);"

    var CREATE_TABLE_GROUP: String = "CREATE TABLE IF NOT EXISTS groups" + " (tox_key text primary key," +
      "name text, " +
      "topic text, " +
      "alias text, " +
      "isconnected boolean, " +
      "ignored boolean, " +
      "isblocked boolean);"

    var CREATE_TABLE_MESSAGES: String = "CREATE TABLE IF NOT EXISTS messages" + " ( _id integer primary key , " +
      "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
      "message_id integer, " +
      "tox_key text, " +
      "sender_name text, " +
      "message text, " +
      "has_been_received boolean, " +
      "has_been_read boolean, " +
      "successfully_sent boolean, " +
      "size integer, " +
      "type int, " +
      "file_kind int, " + //-1 if not a file transfer
      "FOREIGN KEY(tox_key) REFERENCES friends(tox_key))"

    var CREATE_TABLE_FRIEND_REQUESTS: String = "CREATE TABLE IF NOT EXISTS friend_requests" + " ( _id integer primary key, " +
      "tox_key text, " +
      "message text)"

    var CREATE_TABLE_GROUP_INVITES: String = "CREATE TABLE IF NOT EXISTS group_invites" + " ( _id integer primary key, " +
      "tox_key text, " +
      "group_inviter text, " +
      "group_data BLOB)"

    def onCreate(mDb: SQLiteDatabase) {
      mDb.execSQL(CREATE_TABLE_FRIENDS)
      mDb.execSQL(CREATE_TABLE_GROUP)
      mDb.execSQL(CREATE_TABLE_FRIEND_REQUESTS)
      mDb.execSQL(CREATE_TABLE_GROUP_INVITES)
      mDb.execSQL(CREATE_TABLE_MESSAGES)
    }

    override def onUpgrade(mDb: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
      mDb.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_FRIENDS)
      mDb.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_GROUPS)
      mDb.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_CHAT_LOGS)
      mDb.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_FRIEND_REQUESTS)
      onCreate(mDb)
    }

    override def onDowngrade(mDb: SQLiteDatabase, oldVersion: Int, newVersion: Int): Unit = {
      mDb.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_FRIENDS)
      mDb.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_CHAT_LOGS)
      mDb.execSQL("DROP TABLE IF EXISTS " + Constants.TABLE_FRIEND_REQUESTS)
    }
  }
}

class AntoxDB(ctx: Context) {

  private var mDbHelper: DatabaseHelper = _

  private var mDb: SQLiteDatabase = _

  val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)

  private val activeDatabase: String = preferences.getString("active_account", "")

  //TODO: should be private? ask astonex.
  def open(writeable: Boolean): AntoxDB = {
    mDbHelper = new DatabaseHelper(ctx, activeDatabase)
    mDb = if (writeable) mDbHelper.getWritableDatabase else mDbHelper.getReadableDatabase
    this
  }

  def close() {
    mDbHelper.close()
  }

  def addFriend(key: String,
    statusMessage: String,
    alias: String,
    username: String) {
    this.open(writeable = true)
    val parsedUsername = if (username.contains("@")) {
      username.substring(0, username.indexOf("@"))
    } else if (username.length == 0) {
      UIUtils.trimIDForDisplay(key)
    } else {
      username
    }
    val values = new ContentValues()
    values.put(Constants.COLUMN_NAME_KEY, key)
    values.put(Constants.COLUMN_NAME_STATUS, "0")
    values.put(Constants.COLUMN_NAME_NOTE, statusMessage)
    values.put(Constants.COLUMN_NAME_USERNAME, username)
    values.put(Constants.COLUMN_NAME_ISONLINE, false)
    values.put(Constants.COLUMN_NAME_ALIAS, alias)
    values.put(Constants.COLUMN_NAME_ISBLOCKED, false)
    values.put(Constants.COLUMN_NAME_AVATAR, key)
    values.put(Constants.COLUMN_NAME_RECEIVED_AVATAR, false)
    mDb.insert(Constants.TABLE_FRIENDS, null, values)
    this.close()
  }

  def addGroup(key: String, name: String, topic: String): Unit = {
    this.open(writeable = true)
    val values = new ContentValues()
    values.put(Constants.COLUMN_NAME_KEY, key)
    values.put(Constants.COLUMN_NAME_NAME, name)
    values.put(Constants.COLUMN_NAME_TOPIC, topic)
    values.put(Constants.COLUMN_NAME_ALIAS, "")
    values.put(Constants.COLUMN_NAME_IGNORED, false)
    values.put(Constants.COLUMN_NAME_ISBLOCKED, false)
    mDb.insert(Constants.TABLE_GROUPS, null, values)
    this.close()
  }

  def addFileTransfer(key: String,
    path: String,
    fileNumber: Int,
    fileKind: Int,
    size: Int,
    sending: Boolean): Long = {
    this.open(writeable = true)
    val values = new ContentValues()
    values.put(Constants.COLUMN_NAME_KEY, key)
    values.put(Constants.COLUMN_NAME_MESSAGE, path)
    values.put(Constants.COLUMN_NAME_MESSAGE_ID, fileNumber: java.lang.Integer)
    values.put(Constants.COLUMN_NAME_HAS_BEEN_RECEIVED, false)
    values.put(Constants.COLUMN_NAME_HAS_BEEN_READ, false)
    values.put(Constants.COLUMN_NAME_SUCCESSFULLY_SENT, false)
    if (sending) {
      values.put("type", MessageType.FILE_TRANSFER.id: java.lang.Integer)
    } else {
      values.put("type", MessageType.FILE_TRANSFER_FRIEND.id: java.lang.Integer)
    }
    values.put(Constants.COLUMN_NAME_FILE_KIND, fileKind: java.lang.Integer)
    values.put("size", size: java.lang.Integer)
    val id = mDb.insert(Constants.TABLE_CHAT_LOGS, null, values)
    this.close()
    id
  }

  def fileTransferStarted(key: String, fileNumber: Int) {
    this.open(writeable = false)
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
    this.open(writeable = true)
    val values = new ContentValues()
    values.put(Constants.COLUMN_NAME_KEY, key)
    values.put(Constants.COLUMN_NAME_MESSAGE, message)
    mDb.insert(Constants.TABLE_FRIEND_REQUESTS, null, values)
    this.close()
  }

  def addGroupInvite(key: String, inviter: String, data: Array[Byte]) {
    this.open(writeable = true)
    val values = new ContentValues()
    values.put(Constants.COLUMN_NAME_KEY, key)
    values.put(Constants.COLUMN_NAME_GROUP_INVITER, inviter)
    values.put(Constants.COLUMN_NAME_GROUP_DATA, data)
    mDb.insert(Constants.TABLE_GROUP_INVITES, null, values)
    this.close()
  }

  def addMessage(message_id: Int,
    key: String,
    sender_name: String,
    message: String,
    has_been_received: Boolean,
    has_been_read: Boolean,
    successfully_sent: Boolean,
    `type`: MessageType) {
    this.open(writeable = true)
    val values = new ContentValues()
    values.put(Constants.COLUMN_NAME_MESSAGE_ID, message_id: java.lang.Integer)
    values.put(Constants.COLUMN_NAME_KEY, key)
    values.put(Constants.COLUMN_NAME_SENDER_NAME, sender_name)
    values.put(Constants.COLUMN_NAME_MESSAGE, message)
    values.put(Constants.COLUMN_NAME_HAS_BEEN_RECEIVED, has_been_received)
    values.put(Constants.COLUMN_NAME_HAS_BEEN_READ, has_been_read)
    values.put(Constants.COLUMN_NAME_SUCCESSFULLY_SENT, successfully_sent)
    values.put("type", `type`.id: java.lang.Integer)
    values.put(Constants.COLUMN_NAME_FILE_KIND, -1.asInstanceOf[java.lang.Integer])
    mDb.insert(Constants.TABLE_CHAT_LOGS, null, values)
    this.close()
  }

  def getUnreadCounts: Map[String, Integer] = {
    this.open(writeable = false)
    val map = scala.collection.mutable.Map.empty[String, Integer]
    val selectQuery = "SELECT friends.tox_key, COUNT(messages._id) " + "FROM messages " +
      "JOIN friends ON friends.tox_key = messages.tox_key " +
      "WHERE messages.has_been_read == 0 " +
      "AND (messages.type == " + MessageType.FRIEND.id +
      " OR messages.type == " + MessageType.FILE_TRANSFER_FRIEND.id + ") " +
      "AND (messages.file_kind == " + FileKind.INVALID.kindId +
      " OR messages.file_kind == " + FileKind.DATA.kindId + ") " +
      "GROUP BY friends.tox_key"

    //TODO: fix this when I understand sql
    val groupQuery = "SELECT groups.tox_key, COUNT(messages._id) " + "FROM messages " +
      "JOIN groups ON groups.tox_key = messages.tox_key " +
      "WHERE messages.has_been_read == 0 " +
      "AND (messages.type == " + MessageType.GROUP_PEER.id + ")" +
      "GROUP BY groups.tox_key"
    val cursor = mDb.rawQuery(selectQuery, null)
    val groupCursor = mDb.rawQuery(groupQuery, null)
    if (cursor.moveToFirst()) {
      do {
        val key = cursor.getString(0)
        val count = cursor.getInt(1).intValue
        map.put(key, count)
      } while (cursor.moveToNext())
    }

    if (groupCursor.moveToFirst()) {
      do {
        val key = groupCursor.getString(0)
        val count = groupCursor.getInt(1).intValue
        map.put(key, count)
      } while (groupCursor.moveToNext())
    }

    cursor.close()
    groupCursor.close()
    this.close()
    map.toMap
  }

  def getFilePath(key: String, fileNumber: Int): String = {
    this.open(writeable = false)
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
    this.open(writeable = false)
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
    this.open(writeable = false)
    val query = "UPDATE messages SET message_id = -1 WHERE (type == 3 OR type == 4)"
    mDb.execSQL(query)
    this.close()
  }

  def clearFileNumber(key: String, fileNumber: Int) {
    this.open(writeable = false)
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
    this.open(writeable = false)
    val query = "UPDATE messages SET " + Constants.COLUMN_NAME_HAS_BEEN_RECEIVED +
      "=1, message_id = -1 WHERE (type == 3 OR type == 4) AND message_id == " +
      fileNumber +
      " AND tox_key = '" +
      key +
      "'"
    mDb.execSQL(query)
    this.close()
  }

  def getLastMessages: Map[String, (String, Timestamp)] = {
    this.open(writeable = false)
    val map = scala.collection.mutable.Map.empty[String, (String, Timestamp)]
    val selectQuery = "SELECT tox_key, message, timestamp FROM messages WHERE _id IN (" +
      "SELECT MAX(_id) " +
      "FROM messages WHERE (type == " + MessageType.OWN.id +
      " OR type == " + MessageType.FRIEND.id +
      " OR type == " + MessageType.ACTION.id +
      " OR type == " + MessageType.GROUP_OWN.id +
      " OR type == " + MessageType.GROUP_ACTION.id +
      " OR type == " + MessageType.GROUP_PEER.id + ") " +
      "GROUP BY tox_key)"
    val cursor = mDb.rawQuery(selectQuery, null)
    if (cursor.moveToFirst()) {
      do {
        val key = cursor.getString(0)
        val message = cursor.getString(1)
        val timestamp = Timestamp.valueOf(cursor.getString(2))
        map.put(key, (message, timestamp))
      } while (cursor.moveToNext())
    }
    cursor.close()
    this.close()
    map.toMap
  }

  def getMessageList(key: String, actionMessages: Boolean): util.ArrayList[Message] = {
    this.open(writeable = false)
    val messageList = new util.ArrayList[Message]()
    var selectQuery: String = null
    if (key == "") {
      selectQuery = "SELECT * FROM " + Constants.TABLE_CHAT_LOGS + " ORDER BY " +
        Constants.COLUMN_NAME_TIMESTAMP +
        " DESC"
    } else {
      var act: String = null
      act = getQueryTypes(actionMessages)
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
        val sender_name = cursor.getString(4)
        val m = cursor.getString(5)
        val received = cursor.getInt(6) > 0
        val read = cursor.getInt(7) > 0
        val sent = cursor.getInt(8) > 0
        val size = cursor.getInt(9)
        val `type` = cursor.getInt(10)
        val fileKind = FileKind.fromToxFileKind(cursor.getInt(11))
        if (fileKind == FileKind.INVALID || fileKind.visible) {
          messageList.add(new Message(id, message_id, k, sender_name, m, received, read, sent,
          time, size,
          MessageType(`type`), fileKind))
        }
      } while (cursor.moveToNext())
    }
    cursor.close()
    this.close()
    messageList
  }

  def getQueryTypes(actionMessages: Boolean): String = {
    if (actionMessages) "" else "AND (type == 1 OR type == 2 OR type == 3 OR type == 4 OR type == 6 OR type == 7) "
  }

  def getMessageIds(key: String, actionMessages: Boolean): util.HashSet[Integer] = {
    this.open(writeable = false)
    var selectQuery: String = null
    val idSet = new util.HashSet[Integer]()
    if (key == null || key == "") {
      selectQuery = "SELECT * FROM " + Constants.TABLE_CHAT_LOGS + " ORDER BY " +
        Constants.COLUMN_NAME_TIMESTAMP +
        " DESC"
    } else {
      var act: String = null
      act = getQueryTypes(actionMessages)
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
        val fileKind = FileKind.fromToxFileKind(cursor.getInt(11))

        if (fileKind == FileKind.INVALID || fileKind.visible) idSet.add(id)
      } while (cursor.moveToNext())
    }
    cursor.close()
    this.close()
    idSet
  }

  def getMessageCursor(key: String, actionMessages: Boolean): Cursor = {
    this.open(writeable = false)
    var selectQuery: String = null
    if (key == null || key == "") {
      selectQuery = "SELECT * FROM " + Constants.TABLE_CHAT_LOGS + " ORDER BY " +
        Constants.COLUMN_NAME_TIMESTAMP +
        " DESC"
    } else {
      var act: String = null
      act = getQueryTypes(actionMessages)
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

  def getFriendRequestsList: util.ArrayList[FriendRequest] = {
    this.open(writeable = false)
    val friendRequests = new util.ArrayList[FriendRequest]()
    val projection = Array(Constants.COLUMN_NAME_KEY, Constants.COLUMN_NAME_MESSAGE)
    val cursor = mDb.query(Constants.TABLE_FRIEND_REQUESTS, projection, null, null, null, null, null)
    if (cursor.moveToFirst()) {
      do {
        val key = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_KEY))
        val message = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_MESSAGE))
        friendRequests.add(new FriendRequest(key, message))
      } while (cursor.moveToNext())
    }
    cursor.close()
    this.close()
    friendRequests
  }

  def getGroupInvitesList: util.ArrayList[GroupInvite] = {
    this.open(writeable = false)
    val groupInvites = new util.ArrayList[GroupInvite]()
    val projection = Array(Constants.COLUMN_NAME_KEY, Constants.COLUMN_NAME_GROUP_INVITER,
      Constants.COLUMN_NAME_GROUP_DATA)
    val cursor = mDb.query(Constants.TABLE_GROUP_INVITES, projection, null, null, null, null, null)
    if (cursor.moveToFirst()) {
      do {
        val groupId = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_KEY))
        val inviter = cursor.getString(cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_GROUP_INVITER))
        val data = cursor.getBlob(cursor.getColumnIndexOrThrow(Constants.COLUMN_NAME_GROUP_DATA))
        groupInvites.add(new GroupInvite(groupId, inviter, data))
      } while (cursor.moveToNext())
    }
    cursor.close()
    this.close()
    groupInvites
  }

  def getUnsentMessageList: Array[Message] = {
    this.open(writeable = false)
    val messageList = new util.ArrayList[Message]()
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
        val sender_name = cursor.getString(4)
        val m = cursor.getString(5)
        val received = cursor.getInt(6) > 0
        val read = cursor.getInt(7) > 0
        val sent = cursor.getInt(8) > 0
        val size = cursor.getInt(9)
        val `type` = cursor.getInt(10)
        val fileKind = cursor.getInt(11)
        messageList.add(new Message(id, m_id, k, sender_name, m, received, read, sent, time, size,
          MessageType(`type`), FileKind.fromToxFileKind(fileKind)))
      } while (cursor.moveToNext())
    }
    cursor.close()
    this.close()
    messageList.toArray(new Array[Message](messageList.size))
  }

  def updateUnsentMessage(message_id: Integer, id: Integer) {
    this.open(writeable = true)
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
    this.open(writeable = true)
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
    this.open(writeable = true)
    val query = "UPDATE " + Constants.TABLE_CHAT_LOGS + " SET " + Constants.COLUMN_NAME_HAS_BEEN_READ +
      "=1 WHERE " +
      Constants.COLUMN_NAME_KEY +
      "='" +
      key +
      "' AND (type == 2 OR type == 4 OR type == " + MessageType.GROUP_PEER.id + ")"
    mDb.execSQL(query)
    this.close()
    Log.d("", "marked incoming messages as read")
  }

  def deleteMessage(id: Int) {
    this.open(writeable = true)
    val query = "DELETE FROM " + Constants.TABLE_CHAT_LOGS + " WHERE _id == " +
      id
    mDb.execSQL(query)
    this.close()
    Log.d("", "Deleted message")
  }

  def getFriendList: Array[FriendInfo] = {
    this.open(writeable = false)
    val friendList = new ArrayBuffer[FriendInfo]()
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
        val avatar = cursor.getString(7)
        val receievedAvatar = cursor.getInt(8) > 0
        if (alias == null) alias = ""
        if (alias != "") name = alias else if (name == "") name = UIUtils.trimIDForDisplay(key)
        val file = AVATAR.getAvatarFile(avatar, ctx)
        if (!isBlocked) friendList += new FriendInfo(isOnline, name, status, note, key, file, receievedAvatar, alias)
      } while (cursor.moveToNext())
    }
    cursor.close()
    this.close()
    friendList.toArray
  }
  
  def getGroupList: Array[GroupInfo] = {
    this.open(writeable = false)
    val groupList = new ArrayBuffer[GroupInfo]()
    val selectQuery = "SELECT  * FROM " + Constants.TABLE_GROUPS
    val cursor = mDb.rawQuery(selectQuery, null)
    if (cursor.moveToFirst()) {
      do {
        val key = cursor.getString(0)
        val name = cursor.getString(1)
        val topic = cursor.getString(2)
        val alias = cursor.getString(3)
        val isConnected = cursor.getInt(4) != 0
        val ignored = cursor.getInt(5) > 0
        val isBlocked = cursor.getInt(6) > 0
        groupList += new GroupInfo(key, isConnected, name, topic, alias)
      } while (cursor.moveToNext())
    }
    cursor.close()
    this.close()
    groupList.toArray
  }

  private def doesExist(key: String, tableName: String): Boolean = {
    this.open(writeable = false)
    val mCount = mDb.rawQuery("SELECT count(*) FROM " + tableName + " WHERE " +
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

  def doesFriendExist(key: String): Boolean = {
    doesExist(key, Constants.TABLE_FRIENDS)
  }

  def doesGroupExist(key: String): Boolean = {
    doesExist(key, Constants.TABLE_GROUPS)
  }

  def setAllOffline() {
    this.open(writeable = false)
    val values = new ContentValues()
    values.put(Constants.COLUMN_NAME_ISONLINE, "0")
    mDb.update(Constants.TABLE_FRIENDS, values, Constants.COLUMN_NAME_ISONLINE + "='1'", null)
    values.clear()
    values.put(Constants.COLUMN_NAME_ISCONNECTED, "0")
    mDb.update(Constants.TABLE_GROUPS, values, Constants.COLUMN_NAME_ISCONNECTED + "='1'", null)
    this.close()
  }

  private def deleteWithKey(key: String, tableName: String): Unit = {
    this.open(writeable = false)
    mDb.delete(tableName, Constants.COLUMN_NAME_KEY + "='" + key + "'", null)
    this.close()
  }

  def deleteFriend(key: String): Unit = deleteWithKey(key, Constants.TABLE_FRIENDS)
  def deleteFriendRequest(key: String): Unit = deleteWithKey(key, Constants.TABLE_FRIEND_REQUESTS)
  def deleteGroup(key: String): Unit = deleteWithKey(key, Constants.TABLE_GROUPS)
  def deleteGroupInvite(key: String): Unit = deleteWithKey(key, Constants.TABLE_GROUP_INVITES)

  def getFriendRequestMessage(key: String): String = {
    this.open(writeable = false)
    val selectQuery = "SELECT message FROM " + Constants.TABLE_FRIEND_REQUESTS +
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
    deleteWithKey(key, Constants.TABLE_CHAT_LOGS)
  }

  def updateColumnWithKey(table: String, key: String, columnName: String, value: String): Unit = {
    this.open(writeable = false)
    val values = new ContentValues()
    values.put(columnName, value)
    mDb.update(table, values, Constants.COLUMN_NAME_KEY + "='" + key + "'", null)
    this.close()
  }

  def updateColumnWithKey(table: String, key: String, columnName: String, value: Boolean): Unit = {
    this.open(writeable = false)
    val values = new ContentValues()
    values.put(columnName, value)
    mDb.update(table, values, Constants.COLUMN_NAME_KEY + "='" + key + "'", null)
    this.close()
  }

  def updateFriendName(key: String, newName: String) =
    updateColumnWithKey(Constants.TABLE_FRIENDS, key, Constants.COLUMN_NAME_USERNAME, newName)

  def updateGroupName(key: String, newName: String) =
    updateColumnWithKey(Constants.TABLE_GROUPS, key, Constants.COLUMN_NAME_NAME, newName)

  def updateStatusMessage(key: String, newMessage: String) =
    updateColumnWithKey(Constants.TABLE_FRIENDS, key, Constants.COLUMN_NAME_NOTE, newMessage)

  def updateGroupTopic(key: String, newTopic: String) =
    updateColumnWithKey(Constants.TABLE_GROUPS, key, Constants.COLUMN_NAME_TOPIC, newTopic)

  def updateUserStatus(key: String, status: ToxStatus) =
    updateColumnWithKey(Constants.TABLE_FRIENDS, key, Constants.COLUMN_NAME_STATUS, UserStatus.getStringFromToxUserStatus(status))

  def updateUserOnline(key: String, online: Boolean) =
    updateColumnWithKey(Constants.TABLE_FRIENDS, key, Constants.COLUMN_NAME_ISONLINE, online)

  def updateGroupConnected(key: String, connected: Boolean) =
    updateColumnWithKey(Constants.TABLE_GROUPS, key, Constants.COLUMN_NAME_ISCONNECTED, connected)


  def updateFriendAvatar(key: String, avatar: String) {
    this.open(writeable = false)
    val values = new ContentValues()
    values.put(Constants.COLUMN_NAME_AVATAR, avatar)
    mDb.update(Constants.TABLE_FRIENDS, values, Constants.COLUMN_NAME_KEY + "='" + key + "'", null)
    this.close()
  }

  def setAllFriendReceivedAvatar(receivedAvatar: Boolean) {
    this.open(writeable = false)
    val values = new ContentValues()
    values.put(Constants.COLUMN_NAME_RECEIVED_AVATAR, receivedAvatar)
    mDb.update(Constants.TABLE_FRIENDS, values, null, null)
    this.close()
  }

  def updateFriendReceivedAvatar(key: String, receivedAvatar: Boolean) {
    this.open(writeable = false)
    val values = new ContentValues()
    values.put(Constants.COLUMN_NAME_RECEIVED_AVATAR, receivedAvatar)
    mDb.update(Constants.TABLE_FRIENDS, values, Constants.COLUMN_NAME_KEY + "='" + key + "'", null)
    this.close()
  }

  def getFriendDetails(key: String): Array[String] = {
    var details = Array[String](null, null, null)
    this.open(writeable = false)
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
        if (name == "") name = UIUtils.trimIDForDisplay(key)
        details = Array(name, alias, note)
      } while (cursor.moveToNext())
    }
    cursor.close()
    this.close()
    details
  }

  def getFriendNameOrAlias(key: String): String = {
    val friendDetails = getFriendDetails(key)
    if (friendDetails(1) == "") friendDetails(0) else friendDetails(1)
  }

  def getGroupDetails(key: String): (String, String, String) = {
    var name: String = null
    var alias: String = null
    var topic: String = null
    this.open(writeable = false)
    val selectQuery = "SELECT * FROM " + Constants.TABLE_GROUPS + " WHERE " +
      Constants.COLUMN_NAME_KEY +
      "='" +
      key +
      "'"
    val cursor = mDb.rawQuery(selectQuery, null)
    if (cursor.moveToFirst()) {
      do {
        name = cursor.getString(1)
        alias = cursor.getString(3)
        topic = cursor.getString(2)
        if (name == null) name = ""
        if (name == "") name = UIUtils.trimIDForDisplay(key)
      } while (cursor.moveToNext())
    }
    cursor.close()
    this.close()
    (name, alias, topic)
  }

  def getGroupNameOrAlias(key: String): String = {
    val groupDetails = getGroupDetails(key)
    if (groupDetails._2 == "") groupDetails._1 else groupDetails._2
  }

  def getFriendStatusMessage(key: String): String = {
    this.open(writeable = true)

    val query =
      "SELECT " + Constants.COLUMN_NAME_NOTE +
      " FROM " + Constants.TABLE_FRIENDS +
      " WHERE " + Constants.COLUMN_NAME_KEY +
      " = '" + key + "'"

    var friendNote = ""
    val cursor = mDb.rawQuery(query, null)
    if (cursor.moveToFirst()) {
      friendNote = cursor.getString(0)
    }
    cursor.close()

    this.close()

    friendNote
  }

  def updateAlias(alias: String, key: String) {
    this.open(writeable = true)
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
    this.open(writeable = false)
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
