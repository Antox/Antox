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
import com.squareup.sqlbrite.{BriteDatabase, SqlBrite}
import im.tox.antox.data.AntoxDB._
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils._
import im.tox.antox.utils.DbConstants._
import im.tox.antox.wrapper.ContactType.ContactType
import im.tox.antox.wrapper.FileKind.AVATAR
import im.tox.antox.wrapper.MessageType.MessageType
import im.tox.antox.wrapper._
import im.tox.tox4j.core.enums.ToxUserStatus
import im.tox.tox4j.impl.jni.ToxCryptoImpl
import rx.lang.scala.JavaConversions._
import rx.lang.scala.Observable

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object AntoxDB {

  val sqlBrite = SqlBrite.create()

  private class DatabaseHelper(context: Context, activeDatabase: String) extends SQLiteOpenHelper(context,
    activeDatabase, null, DATABASE_VERSION) {

    var CREATE_TABLE_CONTACTS: String = "CREATE TABLE IF NOT EXISTS contacts" + " (tox_key text primary key, " +
      "username text, " +
      "status text, " +
      "note text, " +
      "alias text, " +
      "isonline boolean, " +
      "isblocked boolean, " +
      "avatar text, " +
      "received_avatar boolean," +
      "ignored boolean, " +
      "favorite boolean, " +
      "contact_type int);"

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
      "FOREIGN KEY(tox_key) REFERENCES contacts(tox_key))"

    var CREATE_TABLE_FRIEND_REQUESTS: String = "CREATE TABLE IF NOT EXISTS friend_requests" + " ( _id integer primary key, " +
      "tox_key text, " +
      "message text)"

    var CREATE_TABLE_GROUP_INVITES: String = "CREATE TABLE IF NOT EXISTS group_invites" + " ( _id integer primary key, " +
      "tox_key text, " +
      "group_inviter text, " +
      "group_data BLOB)"

    def onCreate(mDb: SQLiteDatabase) {
      mDb.execSQL(CREATE_TABLE_CONTACTS)
      mDb.execSQL(CREATE_TABLE_FRIEND_REQUESTS)
      mDb.execSQL(CREATE_TABLE_GROUP_INVITES)
      mDb.execSQL(CREATE_TABLE_MESSAGES)
    }

    override def onUpgrade(mDb: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
      mDb.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS)
      mDb.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES)
      mDb.execSQL("DROP TABLE IF EXISTS " + TABLE_FRIEND_REQUESTS)
      onCreate(mDb)
    }

    override def onDowngrade(mDb: SQLiteDatabase, oldVersion: Int, newVersion: Int): Unit = {
      mDb.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS)
      mDb.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES)
      mDb.execSQL("DROP TABLE IF EXISTS " + TABLE_FRIEND_REQUESTS)
    }
  }
}

class AntoxDB(ctx: Context) {

  private var mDbHelper: DatabaseHelper = _

  private var mDb: BriteScalaDatabase = _

  val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)

  private val activeDatabase: String = preferences.getString("active_account", "")

  private def open(writeable: Boolean): AntoxDB = {
    mDbHelper = new DatabaseHelper(ctx, activeDatabase)
    mDb = new BriteScalaDatabase(sqlBrite.wrapDatabaseHelper(mDbHelper))
    this
  }

  def close() {
    mDbHelper.close()
  }

  def synchroniseWithTox(tox: ToxCore): Unit = {
    for (friendNumber <- tox.getFriendList) {
      val friendKey = tox.getFriendKey(friendNumber)
      if (!doesContactExist(friendKey)) {
        addFriend(friendKey, "", "", "")
      }
    }

    for (groupNumber <- tox.getGroupList) {
      val groupKey = tox.getGroupKey(groupNumber)
      if (!doesContactExist(groupKey)) {
        addGroup(groupKey, "", "")
      }
    }
  }

  def addContact(key: ToxKey,
    statusMessage: String,
    alias: String,
    username: String,
    contactType: ContactType) {
    this.open(writeable = true)
    val parsedUsername = if (username.contains("@")) {
      username.substring(0, username.indexOf("@"))
    } else if (username.length == 0) {
      UIUtils.trimId(key)
    } else {
      username
    }

    val values = new ContentValues()
    values.put(COLUMN_NAME_KEY, key.toString)
    values.put(COLUMN_NAME_STATUS, "0")
    values.put(COLUMN_NAME_NOTE, statusMessage)
    values.put(COLUMN_NAME_USERNAME, username)
    values.put(COLUMN_NAME_ISONLINE, false)
    values.put(COLUMN_NAME_ALIAS, alias)
    values.put(COLUMN_NAME_ISBLOCKED, false)
    values.put(COLUMN_NAME_AVATAR, key.toString)
    values.put(COLUMN_NAME_RECEIVED_AVATAR, false)
    values.put(COLUMN_NAME_IGNORED, false)
    values.put(COLUMN_NAME_FAVORITE, false)
    values.put(COLUMN_NAME_CONTACT_TYPE, contactType.id: java.lang.Integer)
    mDb.insert(TABLE_CONTACTS, values)
    this.close()
  }

  def addFriend(key: ToxKey, name: String, statusMessage: String, alias: String): Unit = {
    addContact(key, statusMessage, alias, name, ContactType.FRIEND)
  }

  def addGroup(key: ToxKey, name: String, topic: String): Unit = {
    addContact(key, topic, "", name, ContactType.GROUP)
  }

  def addFileTransfer(key: ToxKey,
    path: String,
    fileNumber: Int,
    fileKind: Int,
    size: Int,
    sending: Boolean): Long = {
    this.open(writeable = true)
    val values = new ContentValues()
    values.put(COLUMN_NAME_KEY, key.toString)
    values.put(COLUMN_NAME_MESSAGE, path)
    values.put(COLUMN_NAME_MESSAGE_ID, fileNumber: java.lang.Integer)
    values.put(COLUMN_NAME_HAS_BEEN_RECEIVED, false)
    values.put(COLUMN_NAME_HAS_BEEN_READ, false)
    values.put(COLUMN_NAME_SUCCESSFULLY_SENT, false)
    if (sending) {
      values.put("type", MessageType.FILE_TRANSFER.id: java.lang.Integer)
    } else {
      values.put("type", MessageType.FILE_TRANSFER_FRIEND.id: java.lang.Integer)
    }
    values.put(COLUMN_NAME_FILE_KIND, fileKind: java.lang.Integer)
    values.put("size", size: java.lang.Integer)
    val id = mDb.insert(TABLE_MESSAGES, values)
    this.close()
    id
  }

  def fileTransferStarted(key: ToxKey, fileNumber: Int) {
    this.open(writeable = false)
    val where =
      s"""WHERE (type == ${MessageType.FILE_TRANSFER}
         |OR type == ${MessageType.FILE_TRANSFER_FRIEND})
         |AND message_id == $fileNumber
         |AND tox_key = '$key'""".stripMargin

    mDb.update(TABLE_MESSAGES, contentValue(COLUMN_NAME_SUCCESSFULLY_SENT, TRUE), where)
    this.close()
  }

  def addFriendRequest(key: ToxKey, message: String) {
    this.open(writeable = true)
    val values = new ContentValues()
    values.put(COLUMN_NAME_KEY, key.toString)
    values.put(COLUMN_NAME_MESSAGE, message)
    mDb.insert(TABLE_FRIEND_REQUESTS, values)
    this.close()
  }

  def addGroupInvite(key: ToxKey, inviter: String, data: Array[Byte]) {
    this.open(writeable = true)
    val values = new ContentValues()
    values.put(COLUMN_NAME_KEY, key.toString)
    values.put(COLUMN_NAME_GROUP_INVITER, inviter)
    values.put(COLUMN_NAME_GROUP_DATA, data)
    mDb.insert(TABLE_GROUP_INVITES, values)
    this.close()
  }

  def addMessage(message_id: Int,
    key: ToxKey,
    sender_name: String,
    message: String,
    has_been_received: Boolean,
    has_been_read: Boolean,
    successfully_sent: Boolean,
    `type`: MessageType) {
    this.open(writeable = true)
    val values = new ContentValues()
    values.put(COLUMN_NAME_MESSAGE_ID, message_id: java.lang.Integer)
    values.put(COLUMN_NAME_KEY, key.toString)
    values.put(COLUMN_NAME_SENDER_NAME, sender_name)
    values.put(COLUMN_NAME_MESSAGE, message)
    values.put(COLUMN_NAME_HAS_BEEN_RECEIVED, has_been_received)
    values.put(COLUMN_NAME_HAS_BEEN_READ, has_been_read)
    values.put(COLUMN_NAME_SUCCESSFULLY_SENT, successfully_sent)
    values.put("type", `type`.id: java.lang.Integer)
    values.put(COLUMN_NAME_FILE_KIND, -1.asInstanceOf[java.lang.Integer])
    mDb.insert(TABLE_MESSAGES, values)
    this.close()
  }

  val unreadCounts: Observable[Map[ToxKey, Integer]] = {
    this.open(writeable = false)
    val map = scala.collection.mutable.Map.empty[ToxKey, Integer]
    val selectQuery =
      s"""SELECT $TABLE_CONTACTS.$COLUMN_NAME_KEY,
        |COUNT($TABLE_MESSAGES._id) FROM $TABLE_MESSAGES JOIN $TABLE_CONTACTS
        |ON $TABLE_CONTACTS.tox_key = $TABLE_MESSAGES.tox_key
        |WHERE $TABLE_MESSAGES.$COLUMN_NAME_HAS_BEEN_READ == $FALSE
        |AND ${createSqlEqualsCondition(COLUMN_NAME_TYPE, MessageType.values -- MessageType.selfValues, TABLE_MESSAGES)}
        |AND ${createSqlEqualsCondition(COLUMN_NAME_FILE_KIND, FileKind.values.filter(_.visible), TABLE_MESSAGES)} GROUP BY contacts.tox_key""".stripMargin

    mDb.createQuery(TABLE_MESSAGES, selectQuery).map(query => {
      val cursor = query.run()
      if (cursor.moveToFirst()) {
        do {
          val key = new ToxKey(cursor.getString(0))
          val count = cursor.getInt(1).intValue
          map.put(key, count)
        } while (cursor.moveToNext())
      }

      cursor.close()
      this.close()
      map.toMap
    })
  }

/*  def getFilePath(key: ToxKey, fileNumber: Int): String = {
    this.open(writeable = false)
    var path = ""
    val selectQuery = "SELECT message FROM messages WHERE tox_key = '" + key +
      "' AND (type == 3 OR type == 4) AND message_id == " +
      fileNumber
    val cursor = mDb.query(selectQuery)
    Log.d("getFilePath count: ", java.lang.Integer.toString(cursor.getCount) + " filenumber: " +
      fileNumber)
    if (cursor.moveToFirst()) {
      path = cursor.getString(0)
    }
    cursor.close()
    this.close()
    path
  } */

  def getFileId(key: ToxKey, fileNumber: Int): Int = {
    this.open(writeable = false)
    var id = -1
    val selectQuery =
      s"""SELECT _id FROM $TABLE_MESSAGES
         |WHERE tox_key = '$key'
         |AND ${createSqlEqualsCondition(COLUMN_NAME_TYPE, MessageType.transferValues, TABLE_MESSAGES)}
         |AND $COLUMN_NAME_MESSAGE_ID == $fileNumber""".stripMargin

    val cursor = mDb.query(selectQuery)
    if (cursor.moveToFirst()) {
      id = cursor.getInt(0)
    }
    cursor.close()
    this.close()
    id
  }

  def clearFileNumbers() {
    this.open(writeable = false)
    val where = createSqlEqualsCondition(COLUMN_NAME_TYPE, MessageType.transferValues)
    mDb.update(TABLE_MESSAGES, contentValue(COLUMN_NAME_MESSAGE_ID, -1), where)
    this.close()
  }

  def clearFileNumber(key: ToxKey, fileNumber: Int) {
    this.open(writeable = false)
    val where = createSqlEqualsCondition(COLUMN_NAME_TYPE, MessageType.transferValues) +
      s" AND $COLUMN_NAME_MESSAGE_ID == $fileNumber AND $COLUMN_NAME_KEY = '$key'"

    mDb.update(TABLE_MESSAGES, contentValue(COLUMN_NAME_MESSAGE_ID, -1), where)
    this.close()
  }

  def fileFinished(key: ToxKey, fileNumber: Int) {
    Log.d("AntoxDB", "fileFinished")
    this.open(writeable = false)
    val where = createSqlEqualsCondition(COLUMN_NAME_TYPE, MessageType.transferValues) +
      s" AND $COLUMN_NAME_MESSAGE_ID == $fileNumber AND $COLUMN_NAME_KEY = '$key'"

    val values = new ContentValues()
    values.put(COLUMN_NAME_HAS_BEEN_RECEIVED, TRUE.asInstanceOf[java.lang.Integer])
    values.put(COLUMN_NAME_MESSAGE_ID, -1.asInstanceOf[java.lang.Integer])
    mDb.update(TABLE_MESSAGES, values, where)
    this.close()
  }

  val lastMessages: Observable[Map[ToxKey, (String, Timestamp)]] = {
    this.open(writeable = false)
    val map = scala.collection.mutable.Map.empty[ToxKey, (String, Timestamp)]
    val selectQuery =
      s"""SELECT $COLUMN_NAME_KEY, $COLUMN_NAME_MESSAGE, $COLUMN_NAME_TIMESTAMP
         |FROM $TABLE_MESSAGES WHERE _id
         |IN (SELECT MAX(_id) FROM $TABLE_MESSAGES
         |WHERE ${createSqlEqualsCondition(COLUMN_NAME_TYPE, MessageType.values -- MessageType.transferValues)}
         |GROUP BY $COLUMN_NAME_KEY)""".stripMargin

    mDb.createQuery(TABLE_MESSAGES, selectQuery).map(query => {
      val cursor = query.run()
      if (cursor.moveToFirst()) {
        do {
          val key = new ToxKey(cursor.getString(0))
          val message = cursor.getString(1)
          val timestamp = Timestamp.valueOf(cursor.getString(2))
          map.put(key, (message, timestamp))
        } while (cursor.moveToNext())
      }
      cursor.close()
      this.close()
      map.toMap
    })
  }

  def messageListObservable(key: Option[ToxKey], actionMessages: Boolean): Observable[Seq[Message]] = {
    this.open(writeable = false)
    val selectQuery: String = getMessageQuery(key, actionMessages)

    mDb.createQuery(TABLE_MESSAGES, selectQuery).map(query => {
      messageListFromCursor(query.run())
    })
  }

  def getMessageList(key: Option[ToxKey], actionMessages: Boolean): Seq[Message] = {
    this.open(writeable = false)
    val selectQuery: String = getMessageQuery(key, actionMessages)

    messageListFromCursor(mDb.query(selectQuery))
  }

  private def getMessageQuery(key: Option[ToxKey], actionMessages: Boolean): String = {
    key match {
      case Some(toxKey) =>
        var act: String = null
        act = getQueryTypes(actionMessages)

        "SELECT * FROM " + TABLE_MESSAGES + " WHERE " +
          COLUMN_NAME_KEY +
          " = '" +
          toxKey +
          "' " +
          act +
          "ORDER BY " +
          COLUMN_NAME_TIMESTAMP +
          " ASC"

      case None =>
        "SELECT * FROM " + TABLE_MESSAGES + " ORDER BY " +
          COLUMN_NAME_TIMESTAMP +
          " DESC"
    }
  }

  private def messageListFromCursor(cursor: Cursor): Seq[Message] = {
    val messageList = new ArrayBuffer[Message]()
    if (cursor.moveToFirst()) {
      do {
        val id = cursor.getInt(0)
        val time = Timestamp.valueOf(cursor.getString(1))
        val message_id = cursor.getInt(2)
        val key = new ToxKey(cursor.getString(3))
        val sender_name = cursor.getString(4)
        val message = cursor.getString(5)
        val received = cursor.getInt(6) > 0
        val read = cursor.getInt(7) > 0
        val sent = cursor.getInt(8) > 0
        val size = cursor.getInt(9)
        val `type` = cursor.getInt(10)
        val fileKind = FileKind.fromToxFileKind(cursor.getInt(11))
        if (fileKind == FileKind.INVALID || fileKind.visible) {
          messageList += new Message(id, message_id, key, sender_name, message, received, read, sent,
            time, size, MessageType(`type`), fileKind)
        }
      } while (cursor.moveToNext())
    }
    cursor.close()
    this.close()
    messageList
  }

  def getQueryTypes(actionMessages: Boolean): String = {
    val condition = createSqlEqualsCondition(COLUMN_NAME_TYPE, MessageType.values.filterNot(_ == MessageType.ACTION).toSeq, TABLE_MESSAGES)
    if (actionMessages) "" else s"AND $condition"
  }

  def getMessageIds(key: Option[ToxKey], actionMessages: Boolean): mutable.Set[Integer] = {
    this.open(writeable = false)
    val idSet = new mutable.HashSet[Integer]()
    val selectQuery = key match {
      case Some(toxKey) =>
        val types: String = getQueryTypes(actionMessages)

        "SELECT * FROM " + TABLE_MESSAGES + " WHERE " +
          COLUMN_NAME_KEY +
          " = '" +
          toxKey +
          "' " +
          types +
          "ORDER BY " +
          COLUMN_NAME_TIMESTAMP +
          " ASC"
      case None =>
        "SELECT * FROM " + TABLE_MESSAGES + " ORDER BY " +
          COLUMN_NAME_TIMESTAMP +
          " DESC"
    }
    val cursor = mDb.query(selectQuery)
    if (cursor.moveToFirst()) {
      do {
        val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NAME_KEY))
        val fileKind = FileKind.fromToxFileKind(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NAME_FILE_KIND)))

        if (fileKind == FileKind.INVALID || fileKind.visible) idSet.add(id)
      } while (cursor.moveToNext())
    }
    cursor.close()
    this.close()
    idSet
  }

 /* def getMessageCursor(key: Option[ToxKey], actionMessages: Boolean): Cursor = {
    this.open(writeable = false)
    val selectQuery: String =
    key match {
      case Some(toxKey) =>
        val types: String = getQueryTypes(actionMessages)
        "SELECT * FROM " + TABLE_MESSAGES + " WHERE " +
          COLUMN_NAME_KEY +
          " = '" +
          toxKey +
          "' " +
          types +
          "ORDER BY " +
          COLUMN_NAME_TIMESTAMP +
          " ASC"
      case None =>
        "SELECT * FROM " + TABLE_MESSAGES + " ORDER BY " +
          COLUMN_NAME_TIMESTAMP +
          " DESC"
    }

    val cursor = mDb.rawQuery(selectQuery)
    cursor
  } */

  def friendRequests: Observable[Seq[FriendRequest]] = {
    this.open(writeable = false)
    val selectQuery = s"SELECT $COLUMN_NAME_KEY, $COLUMN_NAME_MESSAGE FROM $TABLE_FRIEND_REQUESTS"
    mDb.createQuery(TABLE_FRIEND_REQUESTS, selectQuery).map(query => {
      val cursor = query.run()
      val friendRequests = new ArrayBuffer[FriendRequest]()
      if (cursor.moveToFirst()) {
        do {
          val key = new ToxKey(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME_KEY)))
          val message = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME_MESSAGE))
          friendRequests += new FriendRequest(key, message)
        } while (cursor.moveToNext())
      }
      cursor.close()
      this.close()
      friendRequests
    })
  }

  def groupInvites: Observable[Seq[GroupInvite]] = {
    this.open(writeable = false)
    val selectQuery = s"SELECT $COLUMN_NAME_KEY, $COLUMN_NAME_GROUP_INVITER, $COLUMN_NAME_GROUP_DATA FROM $TABLE_GROUP_INVITES"

    mDb.createQuery(TABLE_GROUP_INVITES, selectQuery).map(query => {
      val cursor = query.run()
      val groupInvites = new ArrayBuffer[GroupInvite]()
      if (cursor.moveToFirst()) {
        do {
          val groupKey = new ToxKey(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME_KEY)))
          val inviter = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME_GROUP_INVITER))
          val data = cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_NAME_GROUP_DATA))
          groupInvites += new GroupInvite(groupKey, inviter, data)
        } while (cursor.moveToNext())
      }
      cursor.close()
      this.close()
      groupInvites
    })
  }

  def getUnsentMessageList: Array[Message] = {
    this.open(writeable = false)
    val messageList = new util.ArrayList[Message]()
    val selectQuery = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " +
      COLUMN_NAME_SUCCESSFULLY_SENT +
      "=0 AND type == 1 ORDER BY " +
      COLUMN_NAME_TIMESTAMP +
      " ASC"
    val cursor = mDb.query(selectQuery)
    if (cursor.moveToFirst()) {
      do {
        val id = cursor.getInt(0)
        val time = Timestamp.valueOf(cursor.getString(1))
        val m_id = cursor.getInt(2)
        Log.d("UNSENT MESAGE ID: ", "" + m_id)
        val key = new ToxKey(cursor.getString(3))
        val sender_name = cursor.getString(4)
        val message = cursor.getString(5)
        val received = cursor.getInt(6) > 0
        val read = cursor.getInt(7) > 0
        val sent = cursor.getInt(8) > 0
        val size = cursor.getInt(9)
        val `type` = cursor.getInt(10)
        val fileKind = cursor.getInt(11)
        messageList.add(new Message(id, m_id, key, sender_name, message, received, read, sent, time, size,
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
    values.put(COLUMN_NAME_SUCCESSFULLY_SENT, "1")
    values.put("type", 1: java.lang.Integer)
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
    val date = new Date()
    values.put(COLUMN_NAME_TIMESTAMP, dateFormat.format(date))
    values.put(COLUMN_NAME_MESSAGE_ID, message_id)
    mDb.update(TABLE_MESSAGES, values, s"_id = $id AND $COLUMN_NAME_SUCCESSFULLY_SENT = 0")
    this.close()
  }

  def setMessageReceived(receipt: Int): Unit = {
    this.open(writeable = true)
    val where = s"$COLUMN_NAME_MESSAGE_ID = $receipt AND $COLUMN_NAME_SUCCESSFULLY_SENT = $TRUE AND type = ${MessageType.OWN}"

    mDb.update(TABLE_MESSAGES, contentValue(COLUMN_NAME_HAS_BEEN_RECEIVED, TRUE), where)
  }

  def markIncomingMessagesRead(key: ToxKey) {
    this.open(writeable = true)
    val where =
      s"$COLUMN_NAME_KEY ='$key' AND ${createSqlEqualsCondition(COLUMN_NAME_TYPE, MessageType.values -- MessageType.selfValues)}"
    mDb.update(TABLE_MESSAGES, contentValue(COLUMN_NAME_HAS_BEEN_READ, TRUE), where)
    this.close()
    Log.d("", "marked incoming messages as read")
  }

  def deleteMessage(id: Int) {
    this.open(writeable = true)
    val where = s"_id == $id"
    mDb.delete(TABLE_MESSAGES, where)
    this.close()
    Log.d("", "Deleted message")
  }

  val friendList: Observable[Seq[FriendInfo]] = {
    this.open(writeable = false)
    val friendList = new ArrayBuffer[FriendInfo]()
    val selectQuery =
      s"SELECT  * FROM $TABLE_CONTACTS WHERE $COLUMN_NAME_CONTACT_TYPE == ${ContactType.FRIEND.id}"
    mDb.createQuery(TABLE_CONTACTS, selectQuery).map(query => {
      val cursor = query.run()
      if (cursor.moveToFirst()) {
        do {
          val friendInfo = getFriendInfoFromCursor(cursor)
          if (!friendInfo.blocked) friendList += friendInfo
        } while (cursor.moveToNext())
      }
      cursor.close()
      this.close()
      friendList
    })
  }

  val groupList: Observable[Seq[GroupInfo]] = {
    this.open(writeable = false)
    val selectQuery = "SELECT  * FROM " + TABLE_CONTACTS +
      " WHERE " + COLUMN_NAME_CONTACT_TYPE + " == " + ContactType.GROUP.id

    mDb.createQuery(TABLE_CONTACTS, selectQuery).map(query => {
      val cursor = query.run()
      val groupList = new ArrayBuffer[GroupInfo]()
      if (cursor.moveToFirst()) {
        do {
          val groupInfo = getGroupInfoFromCursor(cursor)
          if (!groupInfo.blocked) groupList += groupInfo
        } while (cursor.moveToNext())
      }

      cursor.close()
      this.close()
      groupList
    })
  }

  val friendInfoList = friendList
    .combineLatestWith(lastMessages)((fl, lm) => (fl, lm))
    .combineLatestWith(unreadCounts)((tup, uc) => {
    tup match {
      case (fl, lm) =>
        fl.map(f => {
          val lastMessageTup: Option[(String, Timestamp)] = lm.get(f.key)
          val unreadCount: Option[Integer] = uc.get(f.key)
          (lastMessageTup, unreadCount) match {
            case (Some((lastMessage, lastMessageTimestamp)), Some(unreadCount)) =>
              f.copy(lastMessage = lastMessage, lastMessageTimestamp = lastMessageTimestamp, unreadCount = unreadCount)
            case (Some((lastMessage, lastMessageTimestamp)), None) =>
              f.copy(lastMessage = lastMessage, lastMessageTimestamp = lastMessageTimestamp, unreadCount = 0)
            case _ =>
              f.copy(lastMessage = "", lastMessageTimestamp = TimestampUtils.emptyTimestamp(), unreadCount = 0)
          }
        })
    }
  })

  val groupInfoList = groupList
    .combineLatestWith(lastMessages)((gl, lm) => (gl, lm))
    .combineLatestWith(unreadCounts)((tup, uc) => {
    tup match {
      case (gl, lm) =>
        gl.map(g => {
          val lastMessageTup: Option[(String, Timestamp)] = lm.get(g.key)
          val unreadCount: Option[Integer] = uc.get(g.key)
          (lastMessageTup, uc) match {
            case (Some((lastMessage, lastMessageTimestamp)), _) =>
              g.copy(lastMessage = lastMessage, lastMessageTimestamp = lastMessageTimestamp, unreadCount = 0)
            case _ =>
              g.copy(lastMessage = "", lastMessageTimestamp = TimestampUtils.emptyTimestamp(), unreadCount = 0)
          }
        })
    }
  })

  //this is bad FIXME
  val contactListElements = friendInfoList
    .combineLatestWith(friendRequests)((friendInfos, friendRequests) => (friendInfos, friendRequests)) //combine friendinfolist and friend requests and return them in a tuple
    .combineLatestWith(groupInvites)((a, gil) => (a._1, a._2, gil)) //return friendinfolist, friendrequests (a) and groupinvites (gi) in a tuple
    .combineLatestWith(groupInfoList)((a, gil) => (a._1, a._2, a._3, gil)) //return friendinfolist, friendrequests and groupinvites (a), and groupInfoList (gl) in a tuple

  def doesContactExist(key: ToxKey): Boolean = {
    this.open(writeable = false)
    val mCount = mDb.query("SELECT count(*) FROM " + TABLE_CONTACTS + " WHERE " +
      COLUMN_NAME_KEY +
      "='" +
      key +
      "'")
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
    this.open(writeable = false)
    val values = new ContentValues()
    values.put(COLUMN_NAME_ISONLINE, "0")
    mDb.update(TABLE_CONTACTS, values, COLUMN_NAME_ISONLINE + "='1'")
    values.clear()
    this.close()
  }

  private def deleteWithKey(key: ToxKey, tableName: String): Unit = {
    this.open(writeable = false)
    mDb.delete(tableName, COLUMN_NAME_KEY + "='" + key + "'")
    this.close()
  }

  def deleteContact(key: ToxKey): Unit = deleteWithKey(key, TABLE_CONTACTS)
  def deleteFriendRequest(key: ToxKey): Unit = deleteWithKey(key, TABLE_FRIEND_REQUESTS)
  def deleteGroupInvite(key: ToxKey): Unit = deleteWithKey(key, TABLE_GROUP_INVITES)
  def deleteChatLogs(key: ToxKey): Unit = deleteWithKey(key, TABLE_MESSAGES)

  def getFriendRequestMessage(key: ToxKey): String = {
    this.open(writeable = false)
    val selectQuery = "SELECT message FROM " + TABLE_FRIEND_REQUESTS +
      " WHERE tox_key='" +
      key +
      "'"
    val cursor = mDb.query(selectQuery)
    var message = ""
    if (cursor.moveToFirst()) {
      message = cursor.getString(0)
    }
    cursor.close()
    this.close()
    message
  }


  def updateColumnWithKey(table: String, key: ToxKey, columnName: String, value: String): Unit = {
    this.open(writeable = false)
    val values = new ContentValues()
    values.put(columnName, value)
    mDb.update(table, values, COLUMN_NAME_KEY + "='" + key + "'")
    this.close()
  }

  def updateColumnWithKey(table: String, key: ToxKey, columnName: String, value: Boolean): Unit = {
    this.open(writeable = false)
    val values = new ContentValues()
    values.put(columnName, value)
    mDb.update(table, values, COLUMN_NAME_KEY + "='" + key + "'")
    this.close()
  }

  def updateContactName(key: ToxKey, newName: String) =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_USERNAME, newName)

  def updateContactStatusMessage(key: ToxKey, newMessage: String) =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_NOTE, newMessage)

  def updateContactStatus(key: ToxKey, status: ToxUserStatus) =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_STATUS, UserStatus.getStringFromToxUserStatus(status))

  def updateContactOnline(key: ToxKey, online: Boolean) =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_ISONLINE, online)

  def updateFriendAvatar(key: ToxKey, avatar: String) =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_AVATAR, avatar)

  def setAllFriendReceivedAvatar(receivedAvatar: Boolean) {
    this.open(writeable = false)
    val values = new ContentValues()
    values.put(COLUMN_NAME_RECEIVED_AVATAR, receivedAvatar)
    mDb.update(TABLE_CONTACTS, values, null)
    this.close()
  }

  def updateContactReceivedAvatar(key: ToxKey, receivedAvatar: Boolean) =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_RECEIVED_AVATAR, receivedAvatar)

  def updateContactFavorite(key: ToxKey, favorite: Boolean) =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_FAVORITE, favorite)

  def getContactDetails(key: ToxKey): Array[String] = {
    var details = Array[String](null, null, null)
    this.open(writeable = false)
    val selectQuery = "SELECT * FROM " + TABLE_CONTACTS + " WHERE " +
      COLUMN_NAME_KEY +
      "='" +
      key +
      "'"
    val cursor = mDb.query(selectQuery)
    if (cursor.moveToFirst()) {
      do {
        var name = cursor.getString(1)
        val note = cursor.getString(3)
        val alias = cursor.getString(4)
        if (name == null) name = ""
        if (name == "") name = UIUtils.trimId(key)
        details = Array(name, alias, note)
      } while (cursor.moveToNext())
    }
    cursor.close()
    this.close()
    details
  }

  def getContactNameOrAlias(key: ToxKey): String = {
    val contactDetails = getContactDetails(key)
    if (contactDetails(1) == "") contactDetails(0) else contactDetails(1)
  }

  def getContactStatusMessage(key: ToxKey): String = {
    getContactInfo(key).statusMessage
  }


  def getContactInfo(key: ToxKey): ContactInfo = {
    this.open(writeable = true)

    val query =
      "SELECT * " +
        " FROM " + TABLE_CONTACTS +
        " WHERE " + COLUMN_NAME_KEY +
        " = '" + key + "'"

    val cursor = mDb.query(query)
    var contactInfo: ContactInfo = null
    if (cursor.moveToFirst()) {
      contactInfo = ContactType(cursor.getInt(11)) match {
        case ContactType.FRIEND | ContactType.NONE =>
          getFriendInfo(key)
        case ContactType.GROUP =>
          getGroupInfo(key)
      }
    }
    cursor.close()
    this.close()

    contactInfo
  }

  def getFriendInfo(key: ToxKey): FriendInfo = {
    this.open(writeable = true)

    val query =
      "SELECT * " +
        " FROM " + TABLE_CONTACTS +
        " WHERE " + COLUMN_NAME_KEY +
        " = '" + key + "'"

    val cursor = mDb.query(query)
    var friendInfo: FriendInfo = null
    if (cursor.moveToFirst()) {
      friendInfo = getFriendInfoFromCursor(cursor)
    }
    cursor.close()
    this.close()

    friendInfo
  }

  def getGroupInfo(key: ToxKey): GroupInfo = {
    this.open(writeable = true)

    val query =
      "SELECT * " +
        " FROM " + TABLE_CONTACTS +
        " WHERE " + COLUMN_NAME_KEY +
        " = '" + key + "'"

    val cursor = mDb.query(query)
    var groupInfo: GroupInfo = null
    if (cursor.moveToFirst()) {
       groupInfo = getGroupInfoFromCursor(cursor)
    }
    cursor.close()
    this.close()

    groupInfo
  }

  private def getFriendInfoFromCursor(cursor: Cursor): FriendInfo = {
    var name = cursor.getString(1)
    val key = new ToxKey(cursor.getString(0))
    val status = cursor.getString(2)
    val note = cursor.getString(3)
    var alias = cursor.getString(4)
    val online = cursor.getInt(5) != 0
    val blocked = cursor.getInt(6) > 0
    val avatar = cursor.getString(7)
    val receievedAvatar = cursor.getInt(8) > 0
    val ignored = cursor.getInt(9) > 0
    val favorite = cursor.getInt(10) > 0

    if (alias == null) alias = ""
    if (alias != "") name = alias else if (name == "") name = UIUtils.trimId(key)
    val file = AVATAR.getAvatarFile(avatar, ctx)

    new FriendInfo(online, name, status, note, key, file, receievedAvatar, blocked, ignored, favorite, alias)
  }

  private def getGroupInfoFromCursor(cursor: Cursor): GroupInfo = {
    var name = cursor.getString(1)
    val key = new ToxKey(cursor.getString(0))
    val status = cursor.getString(2)
    val topic = cursor.getString(3)
    var alias = cursor.getString(4)
    val connected = cursor.getInt(5) != 0
    val blocked = cursor.getInt(6) > 0
    val avatar = cursor.getString(7)
    val receivedAvatar = cursor.getInt(8) > 0
    val ignored = cursor.getInt(9) > 0
    val favorite = cursor.getInt(10) > 0

    if (alias == null) alias = ""
    if (alias != "") name = alias else if (name == "") name = UIUtils.trimId(key)
    val file = AVATAR.getAvatarFile(avatar, ctx)

    new GroupInfo(key, connected, name, topic, blocked, ignored, favorite, alias)
  }

  def updateAlias(alias: String, key: ToxKey) {
    this.open(writeable = true)
    val where =
      s"$COLUMN_NAME_KEY ='$key'"
    mDb.update(TABLE_CONTACTS, contentValue(COLUMN_NAME_ALIAS, alias), where)
    this.close()
  }

  def isContactBlocked(key: ToxKey): Boolean = {
    var isBlocked = false
    this.open(writeable = false)
    val selectQuery = "SELECT isBlocked FROM " + TABLE_CONTACTS + " WHERE " +
      COLUMN_NAME_KEY +
      "='" +
      key +
      "'"
    val cursor = mDb.query(selectQuery)
    if (cursor.moveToFirst()) {
      isBlocked = cursor.getInt(0) > 0
    }
    cursor.close()
    this.close()
    isBlocked
  }
}
