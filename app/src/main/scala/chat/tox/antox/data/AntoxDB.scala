package chat.tox.antox.data

import java.sql.Timestamp

import android.content.{ContentValues, Context}
import android.database.Cursor
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import android.preference.PreferenceManager
import android.util.Log
import chat.tox.antox.data.AntoxDB.DatabaseHelper
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils.DatabaseConstants._
import chat.tox.antox.utils._
import chat.tox.antox.wrapper.ContactType.ContactType
import chat.tox.antox.wrapper.FileKind.AVATAR
import chat.tox.antox.wrapper.MessageType.MessageType
import chat.tox.antox.wrapper.{ToxCore, _}
import com.squareup.sqlbrite.SqlBrite
import im.tox.tox4j.core.enums.{ToxMessageType, ToxUserStatus}
import org.scaloid.common.LoggerTag
import rx.lang.scala.Observable

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object AntoxDB {

  private val TAG = LoggerTag(getClass.getSimpleName)

  val sqlBrite = SqlBrite.create()

  class DatabaseHelper(context: Context, activeDatabase: String) extends SQLiteOpenHelper(context,
    activeDatabase, null, DATABASE_VERSION) {

    var CREATE_TABLE_CONTACTS: String =
      s"""CREATE TABLE IF NOT EXISTS $TABLE_CONTACTS ($COLUMN_NAME_KEY text primary key,
         |$COLUMN_NAME_NAME text,
         |$COLUMN_NAME_STATUS text,
         |$COLUMN_NAME_NOTE text,
         |$COLUMN_NAME_ALIAS text,
         |$COLUMN_NAME_ISONLINE boolean,
         |$COLUMN_NAME_ISBLOCKED boolean,
         |$COLUMN_NAME_AVATAR text,
         |$COLUMN_NAME_RECEIVED_AVATAR boolean,
         |$COLUMN_NAME_IGNORED boolean,
         |$COLUMN_NAME_FAVORITE boolean,
         |$COLUMN_NAME_CONTACT_TYPE int,
         |$COLUMN_NAME_UNSENT_MESSAGE text);""".stripMargin

    var CREATE_TABLE_MESSAGES: String =
      s"""CREATE TABLE IF NOT EXISTS $TABLE_MESSAGES (
         |$COLUMN_NAME_ID integer primary key ,
         |$COLUMN_NAME_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP,
         |$COLUMN_NAME_MESSAGE_ID integer,
         |$COLUMN_NAME_KEY text,
         |$COLUMN_NAME_SENDER_KEY text,
         |$COLUMN_NAME_SENDER_NAME text,
         |$COLUMN_NAME_MESSAGE text,
         |$COLUMN_NAME_HAS_BEEN_RECEIVED boolean,
         |$COLUMN_NAME_HAS_BEEN_READ boolean,
         |$COLUMN_NAME_SUCCESSFULLY_SENT boolean,
         |$COLUMN_NAME_SIZE integer,
         |$COLUMN_NAME_TYPE int,
         |$COLUMN_NAME_FILE_KIND int,
         |FOREIGN KEY($COLUMN_NAME_KEY) REFERENCES $TABLE_CONTACTS($COLUMN_NAME_KEY))""".stripMargin

    var CREATE_TABLE_FRIEND_REQUESTS: String =
      s"""CREATE TABLE IF NOT EXISTS $TABLE_FRIEND_REQUESTS ( _id integer primary key,
         |$COLUMN_NAME_KEY text,
         |$COLUMN_NAME_MESSAGE text)""".stripMargin

    var CREATE_TABLE_GROUP_INVITES: String =
      s"""CREATE TABLE IF NOT EXISTS $TABLE_GROUP_INVITES ( _id integer primary key,
         |$COLUMN_NAME_KEY text,
         |$COLUMN_NAME_GROUP_INVITER text,
         |$COLUMN_NAME_GROUP_DATA BLOB)""".stripMargin

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

class AntoxDB(ctx: Context, activeDatabase: String, selfKey: ToxKey) {

  private var mDbHelper: DatabaseHelper = _

  private var mDb: BriteScalaDatabase = _

  val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)

  mDbHelper = new DatabaseHelper(ctx, activeDatabase)
  mDb = new BriteScalaDatabase(AntoxDB.sqlBrite.wrapDatabaseHelper(mDbHelper))

  def close() {
    mDbHelper.close()
  }

  def synchroniseWithTox(tox: ToxCore): Unit = {
    for (friendKey <- tox.getFriendList) {
      if (!doesContactExist(friendKey)) {
        addFriend(friendKey, "", "", "")
      }
    }

    for (groupKey <- tox.getGroupList) {
      if (!doesContactExist(groupKey)) {
        addGroup(groupKey, "", "")
      }
    }
  }

  def addContact(key: ToxKey,
                 username: String,
                 alias: String,
                 statusMessage: String,
                 contactType: ContactType) {
    val values = new ContentValues()
    values.put(COLUMN_NAME_KEY, key.toString)
    values.put(COLUMN_NAME_STATUS, "0")
    values.put(COLUMN_NAME_NOTE, statusMessage)
    values.put(COLUMN_NAME_NAME, username)
    values.put(COLUMN_NAME_ISONLINE, false)
    values.put(COLUMN_NAME_ALIAS, alias)
    values.put(COLUMN_NAME_ISBLOCKED, false)
    values.put(COLUMN_NAME_AVATAR, key.toString)
    values.put(COLUMN_NAME_RECEIVED_AVATAR, false)
    values.put(COLUMN_NAME_IGNORED, false)
    values.put(COLUMN_NAME_FAVORITE, false)
    values.put(COLUMN_NAME_CONTACT_TYPE, contactType.id: java.lang.Integer)
    mDb.insert(TABLE_CONTACTS, values)
  }

  def addFriend(key: FriendKey, name: String, alias: String, statusMessage: String): Unit = {
    AntoxLog.debug(s"Adding friend $key to database", AntoxDB.TAG)
    addContact(key, name, alias, statusMessage, ContactType.FRIEND)
  }

  def addGroup(key: GroupKey, name: String, topic: String): Unit = {
    AntoxLog.debug(s"Adding group $key to database", AntoxDB.TAG)
    addContact(key, name, "", topic, ContactType.GROUP)
  }

  def addFileTransfer(key: ToxKey,
                      senderKey: ToxKey,
                      senderName: String,
                      path: String,
                      hasBeenRead: Boolean,
                      fileNumber: Int,
                      fileKind: Int,
                      size: Int): Long = {

    val values = new ContentValues()
    values.put(COLUMN_NAME_KEY, key.toString)
    values.put(COLUMN_NAME_SENDER_KEY, senderKey.toString)
    values.put(COLUMN_NAME_SENDER_NAME, senderName)
    values.put(COLUMN_NAME_MESSAGE, path)
    values.put(COLUMN_NAME_MESSAGE_ID, fileNumber: java.lang.Integer)
    values.put(COLUMN_NAME_HAS_BEEN_RECEIVED, false)
    values.put(COLUMN_NAME_HAS_BEEN_READ, hasBeenRead)
    values.put(COLUMN_NAME_SUCCESSFULLY_SENT, false)
    values.put(COLUMN_NAME_TYPE, MessageType.FILE_TRANSFER.id: java.lang.Integer)
    values.put(COLUMN_NAME_FILE_KIND, fileKind: java.lang.Integer)
    values.put(COLUMN_NAME_SIZE, size: java.lang.Integer)
    val id = mDb.insert(TABLE_MESSAGES, values)
    id
  }

  def fileTransferStarted(key: ToxKey, fileNumber: Int) {
    val where =
      s"""type == ${MessageType.FILE_TRANSFER}
         |AND message_id == $fileNumber
         |AND tox_key = '$key'""".stripMargin

    mDb.update(TABLE_MESSAGES, contentValue(COLUMN_NAME_SUCCESSFULLY_SENT, TRUE), where)
  }

  def addFriendRequest(key: ToxKey, message: String) {
    val values = new ContentValues()
    values.put(COLUMN_NAME_KEY, key.toString)
    values.put(COLUMN_NAME_MESSAGE, message)
    mDb.insert(TABLE_FRIEND_REQUESTS, values)
  }

  def addGroupInvite(key: ToxKey, inviter: String, data: Array[Byte]) {
    val values = new ContentValues()
    values.put(COLUMN_NAME_KEY, key.toString)
    values.put(COLUMN_NAME_GROUP_INVITER, inviter)
    values.put(COLUMN_NAME_GROUP_DATA, data)
    mDb.insert(TABLE_GROUP_INVITES, values)
  }

  def addMessage(messageId: Int,
                 key: ToxKey,
                 senderKey: ToxKey,
                 senderName: String,
                 message: String,
                 hasBeenReceived: Boolean,
                 hasBeenRead: Boolean,
                 successfullySent: Boolean,
                 `type`: ToxMessageType) {

    val values = new ContentValues()
    values.put(COLUMN_NAME_MESSAGE_ID, messageId: java.lang.Integer)
    values.put(COLUMN_NAME_KEY, key.toString)
    values.put(COLUMN_NAME_SENDER_KEY, senderKey.toString)
    values.put(COLUMN_NAME_SENDER_NAME, senderName)
    values.put(COLUMN_NAME_MESSAGE, message)
    values.put(COLUMN_NAME_HAS_BEEN_RECEIVED, hasBeenReceived)
    values.put(COLUMN_NAME_HAS_BEEN_READ, hasBeenRead)
    values.put(COLUMN_NAME_SUCCESSFULLY_SENT, successfullySent)
    values.put(COLUMN_NAME_TYPE, MessageType.fromToxMessageType(`type`).id: java.lang.Integer)
    values.put(COLUMN_NAME_FILE_KIND, -1.asInstanceOf[java.lang.Integer])
    mDb.insert(TABLE_MESSAGES, values)
  }

  val unreadCounts: Observable[Map[ToxKey, Int]] = {
    val selectQuery =
      s"""SELECT $TABLE_CONTACTS.$COLUMN_NAME_KEY, COUNT($TABLE_MESSAGES._id)
        |FROM $TABLE_MESSAGES
        |JOIN $TABLE_CONTACTS ON $TABLE_CONTACTS.tox_key = $TABLE_MESSAGES.tox_key
        |WHERE $TABLE_MESSAGES.$COLUMN_NAME_HAS_BEEN_READ == $FALSE
        |AND $COLUMN_NAME_SENDER_KEY != '${selfKey.toString}'
        |AND ${createSqlEqualsCondition(COLUMN_NAME_FILE_KIND, FileKind.values.filter(_.visible).map(_.kindId), TABLE_MESSAGES)} GROUP BY contacts.tox_key""".stripMargin

    mDb.createQuery(TABLE_MESSAGES, selectQuery).map(query => {
      val map = scala.collection.mutable.Map.empty[ToxKey, Int]
      val cursor = query.run()
      if (cursor.moveToFirst()) {
        do {
          val key = new ToxPublicKey(cursor.getString(0))
          val count = cursor.getInt(1).intValue
          map.put(key, count)
        } while (cursor.moveToNext())
      }

      cursor.close()
      map.toMap
    })
  }

  def getUnreadCounts: Map[ToxKey, Int] =
    unreadCounts.toBlocking.first

  def getFileId(key: ToxKey, fileNumber: Int): Int = {
    var id = -1
    val selectQuery =
      s"""SELECT _id
         |FROM $TABLE_MESSAGES
         |WHERE $COLUMN_NAME_KEY = '$key'
         |AND ${createSqlEqualsCondition(COLUMN_NAME_TYPE, MessageType.transferValues.map(_.id), TABLE_MESSAGES)}
         |AND $COLUMN_NAME_MESSAGE_ID == $fileNumber""".stripMargin

    val cursor = mDb.query(selectQuery)
    if (cursor.moveToFirst()) {
      id = cursor.getInt(0)
    }
    cursor.close()
    id
  }

  def clearFileNumbers() {
    val where = createSqlEqualsCondition(COLUMN_NAME_TYPE, MessageType.transferValues.map(_.id))
    mDb.update(TABLE_MESSAGES, contentValue(COLUMN_NAME_MESSAGE_ID, -1), where)
  }

  def clearFileNumber(key: ToxKey, fileNumber: Int) {
    val where = createSqlEqualsCondition(COLUMN_NAME_TYPE, MessageType.transferValues.map(_.id)) +
      s" AND $COLUMN_NAME_MESSAGE_ID == $fileNumber AND $COLUMN_NAME_KEY = '$key'"

    mDb.update(TABLE_MESSAGES, contentValue(COLUMN_NAME_MESSAGE_ID, -1), where)
  }

  def fileTransferFinished(key: ToxKey, fileNumber: Int) {
    AntoxLog.debug("fileFinished", AntoxDB.TAG)
    val where = createSqlEqualsCondition(COLUMN_NAME_TYPE, MessageType.transferValues.map(_.id)) +
        s" AND $COLUMN_NAME_MESSAGE_ID == $fileNumber AND $COLUMN_NAME_KEY = '$key'"

    val values = new ContentValues()
    values.put(COLUMN_NAME_HAS_BEEN_RECEIVED, TRUE.asInstanceOf[java.lang.Integer])
    mDb.update(TABLE_MESSAGES, values, where)
  }

  val lastMessages: Observable[Seq[Message]] = {
    val selectQuery =
      s"""SELECT *
         |FROM $TABLE_MESSAGES
         |WHERE _id
         |IN (SELECT MAX(_id) FROM $TABLE_MESSAGES
         |GROUP BY $COLUMN_NAME_KEY)""".stripMargin

    mDb.createQuery(TABLE_MESSAGES, selectQuery).map(query => {
      val map = scala.collection.mutable.Map.empty[ToxKey, (String, Timestamp)]
      val cursor = query.run()
      val messages = messageListFromCursor(cursor).filter(messageVisible)

      cursor.close()
      messages
    })
  }

  def messageVisible(message: Message): Boolean =
    message.fileKind == FileKind.INVALID || message.fileKind.visible

  def messageListObservable(key: Option[ToxKey]): Observable[ArrayBuffer[Message]] = {
    val selectQuery: String = getMessageQuery(key)

    mDb.createQuery(TABLE_MESSAGES, selectQuery).map(query => {
      messageListFromCursor(query.run())
        .filter(messageVisible)
    })
  }

  def getMessageList(key: Option[ToxKey]): ArrayBuffer[Message] = {
    val selectQuery: String = getMessageQuery(key)

    messageListFromCursor(mDb.query(selectQuery))
      .filter(messageVisible)
  }

  private def getMessageQuery(key: Option[ToxKey]): String = {
    key match {
      case Some(toxKey) =>
        s"""SELECT *
           |FROM $TABLE_MESSAGES
           |WHERE $COLUMN_NAME_KEY = '$toxKey'
           |ORDER BY $COLUMN_NAME_TIMESTAMP ASC""".stripMargin

      case None =>
        s"SELECT * FROM $TABLE_MESSAGES ORDER BY $COLUMN_NAME_TIMESTAMP DESC"
    }
  }

  private def keyFromString(key: String): ToxKey = {
    val contactTypeQuery =
      s"""SELECT $COLUMN_NAME_CONTACT_TYPE
         |FROM $TABLE_CONTACTS
         |WHERE $COLUMN_NAME_KEY = '$key'""".stripMargin

    val cursor = mDb.query(contactTypeQuery)

    if (cursor.moveToFirst()) {
      ContactType(cursor.getInt(COLUMN_NAME_CONTACT_TYPE)) match {
        case ContactType.FRIEND =>
          FriendKey(key)
        case ContactType.GROUP =>
          GroupKey(key)
        case ContactType.PEER =>
          PeerKey(key)
        case _ =>
          ToxPublicKey(key)
      }
    } else {
      if (selfKey.key == key) {
        SelfKey(key)
      } else {
        ToxPublicKey(key)
      }
    }
  }

  private def messageListFromCursor(cursor: Cursor): ArrayBuffer[Message] = {
    val messageList = new ArrayBuffer[Message]()
    if (cursor.moveToFirst()) {
      do {
        val id = cursor.getInt(COLUMN_NAME_ID)
        val time = Timestamp.valueOf(cursor.getString(COLUMN_NAME_TIMESTAMP))
        val messageId = cursor.getInt(COLUMN_NAME_MESSAGE_ID)
        val senderKey = keyFromString(cursor.getString(COLUMN_NAME_SENDER_KEY))
        val senderName = cursor.getString(COLUMN_NAME_SENDER_NAME)
        val message = cursor.getString(COLUMN_NAME_MESSAGE)
        val received = cursor.getBoolean(COLUMN_NAME_HAS_BEEN_RECEIVED)
        val read = cursor.getBoolean(COLUMN_NAME_HAS_BEEN_READ)
        val sent = cursor.getBoolean(COLUMN_NAME_SUCCESSFULLY_SENT)
        val size = cursor.getInt(COLUMN_NAME_SIZE)
        val `type` = MessageType(cursor.getInt(COLUMN_NAME_TYPE))
        val key = keyFromString(cursor.getString(COLUMN_NAME_KEY))
        val fileKind = FileKind.fromToxFileKind(cursor.getInt(COLUMN_NAME_FILE_KIND))
        messageList += new Message(id, messageId, key, senderKey, senderName, message, received, read, sent,
          time, size, `type`, fileKind)
      } while (cursor.moveToNext())
    }
    cursor.close()
    messageList
  }

  def getMessageIds(key: Option[ToxKey]): mutable.Set[Integer] = {
    val idSet = new mutable.HashSet[Integer]()
    val selectQuery = getMessageQuery(key)
    val cursor = mDb.query(selectQuery)
    if (cursor.moveToFirst()) {
      do {
        val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NAME_KEY))
        val fileKind = FileKind.fromToxFileKind(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NAME_FILE_KIND)))

        if (fileKind == FileKind.INVALID || fileKind.visible) idSet.add(id)
      } while (cursor.moveToNext())
    }
    cursor.close()
    idSet
  }

  def friendRequests: Observable[Seq[FriendRequest]] = {
    val selectQuery = s"SELECT $COLUMN_NAME_KEY, $COLUMN_NAME_MESSAGE FROM $TABLE_FRIEND_REQUESTS"
    mDb.createQuery(TABLE_FRIEND_REQUESTS, selectQuery).map(query => {
      val cursor = query.run()
      val friendRequests = new ArrayBuffer[FriendRequest]()
      if (cursor.moveToFirst()) {
        do {
          val key = new FriendKey(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME_KEY)))
          val message = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME_MESSAGE))
          friendRequests += new FriendRequest(key, message)
        } while (cursor.moveToNext())
      }
      cursor.close()
      friendRequests
    })
  }

  def groupInvites: Observable[Seq[GroupInvite]] = {
    val selectQuery = s"SELECT $COLUMN_NAME_KEY, $COLUMN_NAME_GROUP_INVITER, $COLUMN_NAME_GROUP_DATA FROM $TABLE_GROUP_INVITES"

    mDb.createQuery(TABLE_GROUP_INVITES, selectQuery).map(query => {
      val cursor = query.run()
      val groupInvites = new ArrayBuffer[GroupInvite]()
      if (cursor.moveToFirst()) {
        do {
          val groupKey = new GroupKey(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME_KEY)))
          val inviter = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME_GROUP_INVITER))
          val data = cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_NAME_GROUP_DATA))
          groupInvites += new GroupInvite(groupKey, inviter, data)
        } while (cursor.moveToNext())
      }
      cursor.close()
      groupInvites
    })
  }

  def getUnsentMessageList: Array[Message] = {
    val selectQuery =
      s"""SELECT *
         |FROM $TABLE_MESSAGES
         |WHERE $COLUMN_NAME_SUCCESSFULLY_SENT =$FALSE
         |AND ${createSqlEqualsCondition(COLUMN_NAME_TYPE, (MessageType.values -- MessageType.transferValues).map(_.id))}
         |AND $COLUMN_NAME_SENDER_KEY == '${selfKey.toString}'
         |ORDER BY $COLUMN_NAME_TIMESTAMP ASC""".stripMargin

    val cursor = mDb.query(selectQuery)

    val messageList = messageListFromCursor(cursor)
    cursor.close()
    messageList.toArray
  }

  def updateUnsentMessage(messageId: Int, id: Int) {
    val values = new ContentValues()
    values.put(COLUMN_NAME_SUCCESSFULLY_SENT, TRUE.toString)
    values.put(COLUMN_NAME_MESSAGE_ID, messageId: java.lang.Integer)
    mDb.update(TABLE_MESSAGES, values, s"_id = $id AND $COLUMN_NAME_SUCCESSFULLY_SENT = $FALSE")
  }

  def setMessageReceived(receipt: Int): Unit = {
    val where = s"$COLUMN_NAME_MESSAGE_ID = $receipt AND $COLUMN_NAME_SUCCESSFULLY_SENT = $TRUE AND type = ${MessageType.MESSAGE.id}"

    mDb.update(TABLE_MESSAGES, contentValue(COLUMN_NAME_HAS_BEEN_RECEIVED, TRUE), where)
  }

  def markIncomingMessagesRead(key: ToxKey) {
    val where = ""
      //s"$COLUMN_NAME_KEY ='$key' AND ${createSqlEqualsCondition(COLUMN_NAME_TYPE, (MessageType.values -- MessageType.selfValues).map(_.id))}"
    mDb.update(TABLE_MESSAGES, contentValue(COLUMN_NAME_HAS_BEEN_READ, TRUE), where)
    AntoxLog.debug("Marked incoming messages as read", AntoxDB.TAG)
  }

  def deleteMessage(id: Int) {
    val where = s"_id == $id"
    mDb.delete(TABLE_MESSAGES, where)
    AntoxLog.debug(s"Deleted message: $id", AntoxDB.TAG)
  }

  private val friendList: Observable[Seq[FriendInfo]] = {
    val selectQuery =
      s"SELECT * FROM $TABLE_CONTACTS WHERE $COLUMN_NAME_CONTACT_TYPE == ${ContactType.FRIEND.id}"

    mDb.createQuery(TABLE_CONTACTS, selectQuery).map(query => {
      val friendList = new ArrayBuffer[FriendInfo]()
      val cursor = query.run()
      if (cursor.moveToFirst()) {
        do {
          val friendInfo = getFriendInfoFromCursor(cursor)
          if (!friendInfo.blocked) friendList += friendInfo
        } while (cursor.moveToNext())
      }
      cursor.close()
      friendList
    })
  }

  private val groupList: Observable[Seq[GroupInfo]] = {
    val selectQuery =
      s"""SELECT *
         |FROM $TABLE_CONTACTS
         |WHERE $COLUMN_NAME_CONTACT_TYPE == ${ContactType.GROUP.id}""".stripMargin

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
      groupList
    })
  }

  val friendInfoList = friendList
    .combineLatestWith(lastMessages)((fl, lm) => (fl, lm))
    .combineLatestWith(unreadCounts)((tup, unreadCountList) => {
    tup match {
      case (fl, lm) =>
        fl.map(f => {
          val maybeUnreadCount: Option[Int] = unreadCountList.get(f.key)
          f.copy(lastMessage = lm.find(_.key == f.key), unreadCount = maybeUnreadCount.getOrElse(0))
        })
    }
  })

  val groupInfoList = groupList
    .combineLatestWith(lastMessages)((gl, lm) => (gl, lm))
    .combineLatestWith(unreadCounts)((tup, uc) => {
    tup match {
      case (gl, lm) =>
        gl.map(g => {
          val unreadCount: Option[Int] = uc.get(g.key)
          g.copy(lastMessage = lm.find(_.key == g.key), unreadCount = unreadCount.getOrElse(0))
        })
    }
  })

  //this is bad FIXME
  val contactListElements = friendInfoList
    .combineLatestWith(friendRequests)((friendInfos, friendRequests) => (friendInfos, friendRequests)) //combine friendinfolist and friend requests and return them in a tuple
    .combineLatestWith(groupInvites)((a, gil) => (a._1, a._2, gil)) //return friendinfolist, friendrequests (a) and groupinvites (gi) in a tuple
    .combineLatestWith(groupInfoList)((a, gil) => (a._1, a._2, a._3, gil)) //return friendinfolist, friendrequests and groupinvites (a), and groupInfoList (gl) in a tuple

  def doesContactExist(key: ToxKey): Boolean = {
    val mCount = mDb.query(s"SELECT count(*) FROM $TABLE_CONTACTS WHERE $COLUMN_NAME_KEY ='$key'")
    mCount.moveToFirst()
    val count = mCount.getInt(0)
    if (count > 0) {
      mCount.close()
      return true
    }
    mCount.close()
    false
  }

  def setAllOffline() {
    val values = new ContentValues()
    values.put(COLUMN_NAME_ISONLINE, FALSE.toString)
    mDb.update(TABLE_CONTACTS, values, whereClause = s"$COLUMN_NAME_ISONLINE ='$TRUE'")
    values.clear()
  }

  private def deleteWithKey(key: ToxKey, tableName: String): Unit = {
    mDb.delete(tableName, s"$COLUMN_NAME_KEY ='$key'")
  }

  def deleteContact(key: ToxKey): Unit = deleteWithKey(key, TABLE_CONTACTS)
  def deleteFriendRequest(key: ToxKey): Unit = deleteWithKey(key, TABLE_FRIEND_REQUESTS)
  def deleteGroupInvite(key: ToxKey): Unit = deleteWithKey(key, TABLE_GROUP_INVITES)
  def deleteChatLogs(key: ToxKey): Unit = deleteWithKey(key, TABLE_MESSAGES)

  def getFriendRequestMessage(key: ToxKey): String = {
    val selectQuery = s"SELECT message FROM $TABLE_FRIEND_REQUESTS WHERE tox_key='$key'"
    val cursor = mDb.query(selectQuery)
    var message = ""
    if (cursor.moveToFirst()) {
      message = cursor.getString(0)
    }
    cursor.close()
    message
  }


  def updateColumnWithKey(table: String, key: ToxKey, columnName: String, value: String): Unit = {
    val values = new ContentValues()
    values.put(columnName, value)
    mDb.update(table, values, s"$COLUMN_NAME_KEY ='$key'")
  }

  def updateColumnWithKey(table: String, key: ToxKey, columnName: String, value: Boolean): Unit = {
    val values = new ContentValues()
    values.put(columnName, value)
    mDb.update(table, values, s"$COLUMN_NAME_KEY ='$key'")
  }

  def updateContactName(key: ToxKey, newName: String): Unit =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_NAME, newName)

  def updateContactStatusMessage(key: ToxKey, newMessage: String): Unit =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_NOTE, newMessage)

  def updateContactStatus(key: ToxKey, status: ToxUserStatus): Unit =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_STATUS, UserStatus.getStringFromToxUserStatus(status))

  def updateContactOnline(key: ToxKey, online: Boolean): Unit =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_ISONLINE, online)

  def updateFriendAvatar(key: ToxKey, avatar: Option[String]): Unit =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_AVATAR, avatar.getOrElse(""))

  def setAllFriendReceivedAvatar(receivedAvatar: Boolean): Unit = {
    val values = new ContentValues()
    values.put(COLUMN_NAME_RECEIVED_AVATAR, receivedAvatar)
    mDb.update(TABLE_CONTACTS, values, null)
  }

  def updateContactReceivedAvatar(key: ToxKey, receivedAvatar: Boolean): Unit =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_RECEIVED_AVATAR, receivedAvatar)

  def updateContactFavorite(key: ToxKey, favorite: Boolean): Unit =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_FAVORITE, favorite)

  def updateContactUnsentMessage(key: ToxKey, unsentMessage: String): Unit =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_UNSENT_MESSAGE, unsentMessage)

  def getContactUnsentMessage(key: ToxKey): String = {
    val query = s"SELECT $COLUMN_NAME_UNSENT_MESSAGE FROM $TABLE_CONTACTS WHERE $COLUMN_NAME_KEY = '$key'"

    val cursor = mDb.query(query)
    var unsentMessage: String = ""
    if (cursor.moveToFirst()) {
      unsentMessage = cursor.getString(0)
    }
    cursor.close()

    unsentMessage
  }

  def getFriendInfo(key: FriendKey): FriendInfo = {
    val query =
      s"SELECT * FROM $TABLE_CONTACTS WHERE $COLUMN_NAME_KEY = '$key'"

    val cursor = mDb.query(query)
    var friendInfo: FriendInfo = null
    if (cursor.moveToFirst()) {
      friendInfo = getFriendInfoFromCursor(cursor)
    }
    cursor.close()

    friendInfo
  }

  def getGroupInfo(key: ToxKey): GroupInfo = {
    val query =
      s"SELECT * FROM $TABLE_CONTACTS WHERE $COLUMN_NAME_KEY = '$key'"

    val cursor = mDb.query(query)
    var groupInfo: GroupInfo = null
    if (cursor.moveToFirst()) {
       groupInfo = getGroupInfoFromCursor(cursor)
    }
    cursor.close()

    groupInfo
  }

  private def getFriendInfoFromCursor(cursor: Cursor): FriendInfo = {
    var name = cursor.getString(COLUMN_NAME_NAME)
    val key = new FriendKey(cursor.getString(COLUMN_NAME_KEY))
    val status = cursor.getString(COLUMN_NAME_STATUS)
    val statusMessage = cursor.getString(COLUMN_NAME_NOTE)
    var alias = cursor.getString(COLUMN_NAME_ALIAS)
    val online = cursor.getBoolean(COLUMN_NAME_ISONLINE)
    val blocked = cursor.getBoolean(COLUMN_NAME_ISBLOCKED)
    val avatar = cursor.getString(COLUMN_NAME_AVATAR)
    val receievedAvatar = cursor.getBoolean(COLUMN_NAME_RECEIVED_AVATAR)
    val ignored = cursor.getBoolean(COLUMN_NAME_IGNORED)
    val favorite = cursor.getBoolean(COLUMN_NAME_FAVORITE)

    if (alias == null) alias = ""
    if (name == "") name = UiUtils.trimId(key)
    val file = AVATAR.getAvatarFile(avatar, ctx)

    new FriendInfo(online, name, alias, status, statusMessage, key, file, receievedAvatar, blocked, ignored, favorite)
  }

  private def getGroupInfoFromCursor(cursor: Cursor): GroupInfo = {
    var name = cursor.getString(COLUMN_NAME_NAME)
    val key = new GroupKey(cursor.getString(COLUMN_NAME_KEY))
    val status = cursor.getString(COLUMN_NAME_STATUS)
    val topic = cursor.getString(COLUMN_NAME_NOTE)
    var alias = cursor.getString(COLUMN_NAME_ALIAS)
    val connected = cursor.getBoolean(COLUMN_NAME_ISONLINE)
    val blocked = cursor.getBoolean(COLUMN_NAME_ISBLOCKED)
    val avatar = cursor.getString(COLUMN_NAME_AVATAR)
    val receievedAvatar = cursor.getBoolean(COLUMN_NAME_RECEIVED_AVATAR)
    val ignored = cursor.getBoolean(COLUMN_NAME_IGNORED)
    val favorite = cursor.getBoolean(COLUMN_NAME_FAVORITE)

    if (alias == null) alias = ""
    if (name == "") name = UiUtils.trimId(key)
    val file = AVATAR.getAvatarFile(avatar, ctx)

    new GroupInfo(key, connected, name, alias, topic, blocked, ignored, favorite)
  }

  def updateAlias(alias: String, key: ToxKey) {
    val where =
      s"$COLUMN_NAME_KEY ='$key'"
    mDb.update(TABLE_CONTACTS, contentValue(COLUMN_NAME_ALIAS, alias), where)
  }

  def isContactBlocked(key: ToxKey): Boolean = {
    var isBlocked = false
    val selectQuery = s"SELECT isBlocked FROM $TABLE_CONTACTS WHERE $COLUMN_NAME_KEY ='$key'"
    val cursor = mDb.query(selectQuery)
    if (cursor.moveToFirst()) {
      isBlocked = cursor.getInt(0) > 0
    }
    cursor.close()
    isBlocked
  }
}
