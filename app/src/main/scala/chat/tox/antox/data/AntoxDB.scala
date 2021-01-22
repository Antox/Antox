package chat.tox.antox.data

import java.sql.Timestamp

import android.content.{ContentValues, Context}
import android.database.Cursor
import android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper}
import android.preference.PreferenceManager
import chat.tox.antox.data.AntoxDB.DatabaseHelper
import chat.tox.antox.utils.DatabaseConstants.RowOrder.RowOrder
import chat.tox.antox.utils.DatabaseConstants._
import chat.tox.antox.utils.StringExtensions.RichString
import chat.tox.antox.utils._
import chat.tox.antox.wrapper.ContactType.ContactType
import chat.tox.antox.wrapper.FileKind.AVATAR
import chat.tox.antox.wrapper.MessageType.MessageType
import chat.tox.antox.wrapper.{ToxCore, _}
import com.squareup.sqlbrite.SqlBrite
import im.tox.tox4j.core.data.{ToxNickname, ToxStatusMessage}
import im.tox.tox4j.core.enums.{ToxMessageType, ToxUserStatus}
import org.scaloid.common.LoggerTag
import rx.lang.scala.Observable
import rx.schedulers.Schedulers

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object AntoxDB {

  private val TAG = LoggerTag(getClass.getSimpleName)

  val sqlBrite = SqlBrite.create()

  class DatabaseHelper(context: Context, activeDatabase: String) extends SQLiteOpenHelper(context,
    activeDatabase, null, DATABASE_VERSION) {

    val CREATE_TABLE_CONTACTS: String =
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

    val CREATE_TABLE_MESSAGES: String =
      s"""CREATE TABLE IF NOT EXISTS $TABLE_MESSAGES (
          |$COLUMN_NAME_ID integer primary key,
          |$COLUMN_NAME_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP,
          |$COLUMN_NAME_MESSAGE_ID integer,
          |$COLUMN_NAME_KEY text,
          |$COLUMN_NAME_SENDER_KEY text,
          |$COLUMN_NAME_SENDER_NAME text ,
          |$COLUMN_NAME_MESSAGE text,
          |$COLUMN_NAME_HAS_BEEN_RECEIVED boolean,
          |$COLUMN_NAME_HAS_BEEN_READ boolean,
          |$COLUMN_NAME_SUCCESSFULLY_SENT boolean,
          |$COLUMN_NAME_SIZE integer,
          |$COLUMN_NAME_TYPE int,
          |$COLUMN_NAME_FILE_KIND int,
          |$COLUMN_NAME_CALL_EVENT_KIND int,
          |FOREIGN KEY($COLUMN_NAME_KEY) REFERENCES $TABLE_CONTACTS($COLUMN_NAME_KEY))""".stripMargin

    val CREATE_TABLE_FRIEND_REQUESTS: String =
      s"""CREATE TABLE IF NOT EXISTS $TABLE_FRIEND_REQUESTS ( _id integer primary key,
          |$COLUMN_NAME_KEY text,
          |$COLUMN_NAME_MESSAGE text)""".stripMargin

    val CREATE_TABLE_GROUP_INVITES: String =
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

class AntoxDB(ctx: Context, activeDatabase: String, selfKey: SelfKey) {

  private var mDbHelper: DatabaseHelper = _

  private var mDb: BriteScalaDatabase = _

  val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)

  mDbHelper = new DatabaseHelper(ctx, activeDatabase)
  // old style ----
  // mDb = new BriteScalaDatabase(AntoxDB.sqlBrite.wrapDatabaseHelper(mDbHelper))
  // old style ----
  mDb = new BriteScalaDatabase(AntoxDB.sqlBrite.wrapDatabaseHelper(mDbHelper, Schedulers.io()))

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

  def addContact(key: ContactKey,
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

  def addToMessagesTable(messageId: Int,
                         key: ContactKey,
                         senderKey: ToxKey,
                         senderName: ToxNickname,
                         message: String,
                         hasBeenReceived: Boolean,
                         hasBeenRead: Boolean,
                         successfullySent: Boolean,
                         size: Int,
                         messageType: MessageType,
                         fileKind: FileKind = FileKind.INVALID,
                         callEventKind: CallEventKind = CallEventKind.Invalid): Long = {

    val values = new ContentValues()
    values.put(COLUMN_NAME_MESSAGE_ID, messageId: java.lang.Integer)
    values.put(COLUMN_NAME_KEY, key.toString)
    values.put(COLUMN_NAME_SENDER_KEY, senderKey.toString)
    values.put(COLUMN_NAME_SENDER_NAME, new String(senderName.value))
    values.put(COLUMN_NAME_MESSAGE, message)
    values.put(COLUMN_NAME_HAS_BEEN_RECEIVED, hasBeenReceived)
    values.put(COLUMN_NAME_HAS_BEEN_READ, hasBeenRead)
    values.put(COLUMN_NAME_SUCCESSFULLY_SENT, successfullySent)
    values.put(COLUMN_NAME_SIZE, size: java.lang.Integer)
    values.put(COLUMN_NAME_TYPE, messageType.id: java.lang.Integer)
    values.put(COLUMN_NAME_FILE_KIND, fileKind.kindId: java.lang.Integer)
    values.put(COLUMN_NAME_CALL_EVENT_KIND, callEventKind.kindId: java.lang.Integer)
    val id = mDb.insert(TABLE_MESSAGES, values)
    id
  }

  def addFileTransfer(fileNumber: Int,
                      key: ContactKey,
                      senderKey: ToxKey,
                      senderName: ToxNickname,
                      path: String,
                      hasBeenRead: Boolean,
                      size: Int,
                      fileKind: FileKind): Long = {

    addToMessagesTable(
      fileNumber,
      key,
      senderKey,
      senderName,
      path,
      hasBeenReceived = false,
      hasBeenRead = hasBeenRead,
      successfullySent = false,
      size,
      MessageType.FILE_TRANSFER,
      fileKind = fileKind
    )
  }

  def fileTransferStarted(key: ContactKey, fileNumber: Int) {
    val where =
      s"""type == ${MessageType.FILE_TRANSFER}
          |AND message_id == $fileNumber
          |AND tox_key = '$key'""".stripMargin

    mDb.update(TABLE_MESSAGES, contentValue(COLUMN_NAME_SUCCESSFULLY_SENT, TRUE), where)
  }

  def addFriendRequest(key: ContactKey, message: String) {
    val values = new ContentValues()
    values.put(COLUMN_NAME_KEY, key.toString)
    values.put(COLUMN_NAME_MESSAGE, message)
    mDb.insert(TABLE_FRIEND_REQUESTS, values)
  }

  def addGroupInvite(key: ContactKey, inviter: FriendKey, data: Array[Byte]) {
    val values = new ContentValues()
    values.put(COLUMN_NAME_KEY, key.toString)
    values.put(COLUMN_NAME_GROUP_INVITER, inviter.toString)
    values.put(COLUMN_NAME_GROUP_DATA, data)
    mDb.insert(TABLE_GROUP_INVITES, values)
  }

  def addMessage(key: ContactKey,
                 senderKey: ToxKey,
                 senderName: ToxNickname,
                 message: String,
                 hasBeenReceived: Boolean,
                 hasBeenRead: Boolean,
                 successfullySent: Boolean,
                 messageType: ToxMessageType,
                 messageId: Int = -1): Long = {


    addToMessagesTable(
      messageId,
      key,
      senderKey,
      senderName,
      message,
      hasBeenReceived,
      hasBeenRead,
      successfullySent,
      size = 0,
      MessageType.fromToxMessageType(messageType)
    )
  }

  def addCallEventMessage(key: ContactKey,
                          senderKey: ToxKey,
                          senderName: ToxNickname,
                          message: String,
                          hasBeenRead: Boolean,
                          kind: CallEventKind): Unit = {
    addToMessagesTable(
      messageId = -1,
      key,
      senderKey,
      senderName,
      message,
      hasBeenReceived = true,
      hasBeenRead = hasBeenRead,
      successfullySent = true,
      size = 0,
      messageType = MessageType.CALL_EVENT,
      callEventKind = kind
    )
  }

  def contactKeyFromContactType(key: String, contactType: ContactType): ContactKey = {
    contactType match {
      case ContactType.FRIEND =>
        FriendKey(key)
      case ContactType.GROUP =>
        GroupKey(key)
      case ContactType.PEER =>
        PeerKey(key)
    }
  }

  def keyFromMaybeContactType(key: String, maybeContactType: Option[ContactType]): ToxKey = {
    maybeContactType match {
      case Some(contactType) =>
        contactKeyFromContactType(key, contactType)
      case _ =>
        if (selfKey.key == key) {
          SelfKey(key)
        } else {
          throw new IllegalArgumentException()
        }
    }
  }

  val unreadCounts: Observable[Map[ContactKey, Int]] = {
    val selectQuery =
      s"""SELECT $TABLE_CONTACTS.$COLUMN_NAME_KEY,
          |COUNT($TABLE_MESSAGES._id),
          |conversation.$COLUMN_NAME_CONTACT_TYPE as $COLUMN_NAME_CONVERSATION_CONTACT_TYPE
          |FROM $TABLE_MESSAGES
          |
        |LEFT JOIN $TABLE_CONTACTS AS conversation ON conversation.$COLUMN_NAME_KEY = $TABLE_MESSAGES.$COLUMN_NAME_SENDER_KEY
          |JOIN $TABLE_CONTACTS ON $TABLE_CONTACTS.tox_key = $TABLE_MESSAGES.tox_key
          |WHERE $TABLE_MESSAGES.$COLUMN_NAME_HAS_BEEN_READ == $FALSE
          |AND $COLUMN_NAME_SENDER_KEY != '${selfKey.toString}'
          |AND ${createSqlEqualsCondition(COLUMN_NAME_FILE_KIND, FileKind.values.filter(_.visible).map(_.kindId), TABLE_MESSAGES)}
          |GROUP BY $TABLE_CONTACTS.tox_key""".stripMargin

    mDb.createQuery(TABLE_MESSAGES, selectQuery).map(closedCursor => {
      val map = scala.collection.mutable.Map.empty[ContactKey, Int]
      closedCursor.use { cursor =>
        if (cursor.moveToFirst()) {
          do {
            val key =
              contactKeyFromContactType(cursor.getString(s"$COLUMN_NAME_KEY"),
                ContactType(cursor.getInt(COLUMN_NAME_CONVERSATION_CONTACT_TYPE)))

            val count = cursor.getInt(1).intValue
            map.put(key, count)
          } while (cursor.moveToNext())
        }
      }

      map.toMap
    })


  }

  def getUnreadCounts: Map[ContactKey, Int] = {
    unreadCounts.toBlocking.first
  }

  val sqlMessageVisible: String = createSqlEqualsCondition(COLUMN_NAME_FILE_KIND, FileKind.values.filter(_.visible).map(_.kindId))

  val sqlIsFileTransfer: String = createSqlEqualsCondition(COLUMN_NAME_TYPE, MessageType.transferValues.map(_.id), TABLE_MESSAGES)

  def getFileId(key: ContactKey, fileNumber: Int): Int = {
    val selectQuery =
      s"""SELECT _id
          |FROM $TABLE_MESSAGES
          |WHERE $COLUMN_NAME_KEY = '$key'
          |AND $sqlIsFileTransfer
          |AND $COLUMN_NAME_MESSAGE_ID == $fileNumber""".stripMargin

    mDb.query(selectQuery).use { cursor =>
      if (cursor.moveToFirst()) {
        cursor.getInt(0)
      } else {
        -1
      }
    }
  }

  def clearFileNumbers() {
    val where = sqlIsFileTransfer
    mDb.update(TABLE_MESSAGES, contentValue(COLUMN_NAME_MESSAGE_ID, -1), where)
  }

  def clearFileNumber(key: ContactKey, fileNumber: Int) {
    val where = sqlIsFileTransfer +
      s" AND $COLUMN_NAME_MESSAGE_ID == $fileNumber AND $COLUMN_NAME_KEY = '$key'"

    mDb.update(TABLE_MESSAGES, contentValue(COLUMN_NAME_MESSAGE_ID, -1), where)
  }

  def fileTransferFinished(key: ContactKey, fileNumber: Int) {
    AntoxLog.debug("fileFinished", AntoxDB.TAG)
    val where = sqlIsFileTransfer +
      s" AND $COLUMN_NAME_MESSAGE_ID == $fileNumber AND $COLUMN_NAME_KEY = '$key'"

    val values = new ContentValues()
    values.put(COLUMN_NAME_HAS_BEEN_RECEIVED, TRUE.asInstanceOf[java.lang.Integer])
    mDb.update(TABLE_MESSAGES, values, where)
  }

  /**
    * Observable called whenever [[TABLE_MESSAGES]] is updated.
    *
    * @return the number of messages caught by the query.
    */
  def messageListUpdatedObservable(key: Option[ContactKey]): Observable[Int] = {
    val whereKey = if (key.isDefined) {
      s"WHERE $COLUMN_NAME_KEY = '${key.get}'"
    } else "WHERE TRUE"

    val query =
      s"""SELECT COUNT(*) FROM $TABLE_MESSAGES
          |$whereKey
      """.stripMargin

    mDb.createQuery(TABLE_MESSAGES, query).map { closedCursor =>
      closedCursor.use { cursor =>
        cursor.moveToFirst()
        cursor.getInt(0)
      }
    }
  }

  def messageListObservable(key: Option[ContactKey]): Observable[ArrayBuffer[Message]] = {
    val selectQuery: String = getMessageQuery(key, RowOrder.ASCENDING)

    mDb.createQuery(TABLE_MESSAGES, selectQuery).map(_.use(messageListFromCursor))
  }

  /**
    * Gets list of messages (including file transfers) limited by takeLast within conversation 'key' if key is Some,
    * otherwise returns a list of all messages.
    *
    * @param key      the conversation key for which the messages should be queried
    * @param takeLast the number of messages to return, starting from from the end of the query.
    *                 e.g. if `takeLast = 2` and the total list of messages is `List("a", "b", "c")`,
    *                 the result of this function would be `List("b", "c")` </code>.
    *                 If this value is less than 0, as it is by default, there will be no limit on the number of messages returned.
    * @return a list of messages constrained by the parameters.
    */
  def getMessageList(key: Option[ContactKey], takeLast: Int = -1): ArrayBuffer[Message] = {
    val selectQuery: String = getMessageQuery(key, RowOrder.ASCENDING, takeLast)

    mDb.query(selectQuery).use(messageListFromCursor)
  }

  private def getMessageQuery(key: Option[ContactKey], orderBy: RowOrder, limit: Int = -1): String = {
    val selection =
      s"""$TABLE_MESSAGES.*,
         |sender.$COLUMN_NAME_CONTACT_TYPE as $COLUMN_NAME_SENDER_CONTACT_TYPE,
         |conversation.$COLUMN_NAME_CONTACT_TYPE as $COLUMN_NAME_CONVERSATION_CONTACT_TYPE""".stripMargin

    val joins =
      s"""LEFT JOIN $TABLE_CONTACTS AS conversation ON conversation.$COLUMN_NAME_KEY = $TABLE_MESSAGES.$COLUMN_NAME_KEY
          |LEFT JOIN $TABLE_CONTACTS AS sender ON sender.$COLUMN_NAME_KEY = $TABLE_MESSAGES.$COLUMN_NAME_SENDER_KEY""".stripMargin

    val whereKey =
      if (key.isDefined) {
        s"WHERE $TABLE_MESSAGES.$COLUMN_NAME_KEY = '${key.get}'"
      } else s"WHERE $TRUE"

    val order = s"ORDER BY $COLUMN_NAME_TIMESTAMP ${orderBy.toString}"

    val query =
      s"""SELECT $selection
          |FROM $TABLE_MESSAGES
          |$joins
          |$whereKey
          |AND $sqlMessageVisible""".stripMargin

    if (limit >= 0) {
      s"""SELECT *
          |FROM ($query ORDER BY $COLUMN_NAME_ID DESC LIMIT $limit)
          |$order""".stripMargin
    } else {
      s"""$query
         |$order""".stripMargin
    }
  }

  private def messageListFromCursor(cursor: Cursor): ArrayBuffer[Message] = {
    val messageList = new ArrayBuffer[Message]()
    if (cursor.moveToFirst()) {
      do {
        val id = cursor.getInt(COLUMN_NAME_ID)
        val timestamp = Timestamp.valueOf(cursor.getString(COLUMN_NAME_TIMESTAMP))
        val messageId = cursor.getInt(COLUMN_NAME_MESSAGE_ID)
        val maybeContactType = cursor.maybeGetInt(COLUMN_NAME_SENDER_CONTACT_TYPE).map(ContactType(_))
        val senderKey = keyFromMaybeContactType(cursor.getString(COLUMN_NAME_SENDER_KEY), maybeContactType)
        val senderName = cursor.getString(COLUMN_NAME_SENDER_NAME)
        val message = cursor.getString(COLUMN_NAME_MESSAGE)
        val received = cursor.getBoolean(COLUMN_NAME_HAS_BEEN_RECEIVED)
        val read = cursor.getBoolean(COLUMN_NAME_HAS_BEEN_READ)
        val sent = cursor.getBoolean(COLUMN_NAME_SUCCESSFULLY_SENT)
        val size = cursor.getInt(COLUMN_NAME_SIZE)
        val messageType = MessageType(cursor.getInt(COLUMN_NAME_TYPE))
        val key = contactKeyFromContactType(cursor.getString(COLUMN_NAME_KEY), ContactType(cursor.getInt(COLUMN_NAME_CONVERSATION_CONTACT_TYPE)))
        val fileKind = FileKind.fromToxFileKind(cursor.getInt(COLUMN_NAME_FILE_KIND))
        val callEventKind = CallEventKind.values.find(_.kindId == cursor.getInt(COLUMN_NAME_CALL_EVENT_KIND)).getOrElse(CallEventKind.Invalid)
        messageList += Message(id, messageId, key, senderKey, senderName, message, received, read, sent,
          timestamp, size, messageType, fileKind, callEventKind)
      } while (cursor.moveToNext())
    }
    messageList
  }

  def getMessageIds(key: Option[ContactKey]): mutable.Set[Integer] = {
    val idSet = new mutable.HashSet[Integer]()
    val selectQuery = getMessageQuery(key, RowOrder.ASCENDING)
    mDb.query(selectQuery).use { cursor =>
      if (cursor.moveToFirst()) {
        do {
          val id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NAME_KEY))
          val fileKind = FileKind.fromToxFileKind(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NAME_FILE_KIND)))

          if (fileKind == FileKind.INVALID || fileKind.visible) idSet.add(id)
        } while (cursor.moveToNext())
      }
    }

    idSet
  }

  def friendRequests: Observable[Seq[FriendRequest]] = {
    val selectQuery = s"SELECT $COLUMN_NAME_KEY, $COLUMN_NAME_MESSAGE FROM $TABLE_FRIEND_REQUESTS"
    mDb.createQuery(TABLE_FRIEND_REQUESTS, selectQuery).map(closedCursor => {
      val friendRequestsList = new ArrayBuffer[FriendRequest]()

      closedCursor.use { cursor =>
        if (cursor.moveToFirst()) {
          do {
            val key = new FriendKey(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME_KEY)))
            val message = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME_MESSAGE))
            friendRequestsList += new FriendRequest(key, message)
          } while (cursor.moveToNext())
        }
      }

      friendRequestsList
    })
  }

  def groupInvites: Observable[Seq[GroupInvite]] = {
    val selectQuery = s"SELECT $COLUMN_NAME_KEY, $COLUMN_NAME_GROUP_INVITER, $COLUMN_NAME_GROUP_DATA FROM $TABLE_GROUP_INVITES"

    mDb.createQuery(TABLE_GROUP_INVITES, selectQuery).map(closedCursor => {
      val groupInvitesList = new ArrayBuffer[GroupInvite]()

      closedCursor.use { cursor =>
        if (cursor.moveToFirst()) {
          do {
            val groupKey = new GroupKey(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME_KEY)))
            val inviter = FriendKey(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME_GROUP_INVITER)))
            val data = cursor.getBlob(cursor.getColumnIndexOrThrow(COLUMN_NAME_GROUP_DATA))
            groupInvitesList += new GroupInvite(groupKey, inviter, data)
          } while (cursor.moveToNext())
        }
      }

      groupInvitesList
    })
  }

  def getUnsentMessageList(contactKey: ContactKey): Array[Message] = {
    val messageList =
      getMessageList(Some(contactKey))
        .filterNot(_.sent)
        .filterNot(_.isFileTransfer)
        .filter(_.senderKey == selfKey)

    messageList.toArray
  }

  def updateUnsentMessage(messageId: Int, id: Long) {
    val values = new ContentValues()
    values.put(COLUMN_NAME_SUCCESSFULLY_SENT, TRUE.toString)
    values.put(COLUMN_NAME_MESSAGE_ID, messageId: java.lang.Integer)
    mDb.update(TABLE_MESSAGES, values, s"_id = $id AND $COLUMN_NAME_SUCCESSFULLY_SENT = $FALSE")
  }

  def setMessageReceived(receipt: Int): Unit = {
    val where = s"$COLUMN_NAME_MESSAGE_ID = $receipt AND $COLUMN_NAME_SUCCESSFULLY_SENT = $TRUE AND type = ${MessageType.MESSAGE.id}"

    mDb.update(TABLE_MESSAGES, contentValue(COLUMN_NAME_HAS_BEEN_RECEIVED, TRUE), where)
  }

  def markIncomingMessagesRead(key: ContactKey) {
    val where = s"$COLUMN_NAME_KEY ='$key' AND $COLUMN_NAME_HAS_BEEN_READ = $FALSE"
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

    mDb.createQuery(TABLE_CONTACTS, selectQuery).map(closedCursor => {
      val friendList = new ArrayBuffer[FriendInfo]()
      closedCursor.use { cursor =>
        if (cursor.moveToFirst()) {
          do {
            val friendInfo = getFriendInfoFromCursor(cursor)
            if (!friendInfo.blocked) friendList += friendInfo
          } while (cursor.moveToNext())
        }
      }

      friendList
    })
  }

  private val groupList: Observable[Seq[GroupInfo]] = {
    val selectQuery =
      s"""SELECT *
          |FROM $TABLE_CONTACTS
          |WHERE $COLUMN_NAME_CONTACT_TYPE == ${ContactType.GROUP.id}""".stripMargin

    mDb.createQuery(TABLE_CONTACTS, selectQuery).map(closedCursor => {
      val groupList = new ArrayBuffer[GroupInfo]()
      closedCursor.use { cursor =>
        if (cursor.moveToFirst()) {
          do {
            val groupInfo = getGroupInfoFromCursor(cursor)
            if (!groupInfo.blocked) groupList += groupInfo
          } while (cursor.moveToNext())
        }
      }

      groupList
    })
  }

  val friendInfoList = friendList
    .combineLatestWith(unreadCounts)((fl, unreadCountList) => {
      fl.map(f => {
        val unreadCount: Int = unreadCountList.get(f.key).getOrElse(0)
        val lastMessage: Option[Message] = getMessageList(Some(f.key), 1).headOption
        f.copy(lastMessage = lastMessage, unreadCount = unreadCount)
      })
    })

  val groupInfoList = groupList
    .combineLatestWith(unreadCounts)((gl, unreadCountList) => {
      gl.map(g => {
        val unreadCount: Int = unreadCountList.get(g.key).getOrElse(0)
        val lastMessage: Option[Message] = getMessageList(Some(g.key), 1).headOption
        g.copy(lastMessage = lastMessage, unreadCount = unreadCount)
      })
    })

  //this is bad FIXME
  val contactListElements = friendInfoList
    .combineLatestWith(friendRequests)((friendInfos, friendRequests) => (friendInfos, friendRequests)) //combine friendinfolist and friend requests and return them in a tuple
    .combineLatestWith(groupInvites)((a, gil) => (a._1, a._2, gil)) //return friendinfolist, friendrequests (a) and groupinvites (gi) in a tuple
    .combineLatestWith(groupInfoList)((a, gil) => (a._1, a._2, a._3, gil)) //return friendinfolist, friendrequests and groupinvites (a), and groupInfoList (gl) in a tuple

  def doesContactExist(key: ContactKey): Boolean = {
    mDb.query(s"SELECT count(*) FROM $TABLE_CONTACTS WHERE $COLUMN_NAME_KEY ='$key'").use { cursor =>
      cursor.moveToFirst()
      val exists = cursor.getInt(0) > 0

      exists
    }
  }

  def setAllOffline() {
    val values = new ContentValues()
    values.put(COLUMN_NAME_ISONLINE, FALSE.toString)
    mDb.update(TABLE_CONTACTS, values, whereClause = s"$COLUMN_NAME_ISONLINE ='$TRUE'")
    values.clear()
  }

  private def deleteWithKey(key: ContactKey, tableName: String): Unit = {
    mDb.delete(tableName, s"$COLUMN_NAME_KEY ='$key'")
  }

  def deleteContact(key: ContactKey): Unit = deleteWithKey(key, TABLE_CONTACTS)

  def deleteFriendRequest(key: ContactKey): Unit = deleteWithKey(key, TABLE_FRIEND_REQUESTS)

  def deleteGroupInvite(key: ContactKey): Unit = deleteWithKey(key, TABLE_GROUP_INVITES)

  def deleteChatLogs(key: ContactKey): Unit = deleteWithKey(key, TABLE_MESSAGES)

  def getFriendRequestMessage(key: ContactKey): String = {
    val selectQuery = s"SELECT message FROM $TABLE_FRIEND_REQUESTS WHERE $COLUMN_NAME_KEY='$key'"
    val message = mDb.query(selectQuery).use { cursor =>
      if (cursor.moveToFirst()) {
        cursor.getString(0)
      } else ""
    }

    message
  }


  private def updateColumnWithKey(table: String, key: ContactKey, columnName: String, value: String): Unit = {
    val values = new ContentValues()
    values.put(columnName, value)
    mDb.update(table, values, s"$COLUMN_NAME_KEY ='$key'")
  }

  private def updateColumnWithKey(table: String, key: ContactKey, columnName: String, value: Boolean): Unit = {
    val values = new ContentValues()
    values.put(columnName, value)
    mDb.update(table, values, s"$COLUMN_NAME_KEY ='$key'")
  }

  def updateContactName(key: ContactKey, newName: String): Unit =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_NAME, newName)

  def updateContactStatusMessage(key: ContactKey, statusMessage: ToxStatusMessage): Unit =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_NOTE, new String(statusMessage.value))

  def updateContactStatus(key: ContactKey, status: ToxUserStatus): Unit =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_STATUS, UserStatus.getStringFromToxUserStatus(status))

  def updateContactOnline(key: ContactKey, online: Boolean): Unit =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_ISONLINE, online)

  def updateFriendAvatar(key: ContactKey, avatar: Option[String]): Unit =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_AVATAR, avatar.getOrElse(""))

  def setAllFriendReceivedAvatar(receivedAvatar: Boolean): Unit = {
    val values = new ContentValues()
    values.put(COLUMN_NAME_RECEIVED_AVATAR, receivedAvatar)
    mDb.update(TABLE_CONTACTS, values, null)
  }

  def updateContactReceivedAvatar(key: ContactKey, receivedAvatar: Boolean): Unit =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_RECEIVED_AVATAR, receivedAvatar)

  def updateContactFavorite(key: ContactKey, favorite: Boolean): Unit =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_FAVORITE, favorite)

  def updateContactUnsentMessage(key: ContactKey, unsentMessage: String): Unit =
    updateColumnWithKey(TABLE_CONTACTS, key, COLUMN_NAME_UNSENT_MESSAGE, unsentMessage)

  def getContactUnsentMessage(key: ContactKey): String = {
    val query = s"SELECT $COLUMN_NAME_UNSENT_MESSAGE FROM $TABLE_CONTACTS WHERE $COLUMN_NAME_KEY = '$key'"

    val unsentMessage =
      mDb.query(query).use { cursor =>
        if (cursor.moveToFirst()) {
          cursor.getString(0)
        } else ""
      }

    unsentMessage
  }

  def getFriendInfo(key: FriendKey): FriendInfo = {
    val query =
      s"SELECT * FROM $TABLE_CONTACTS WHERE $COLUMN_NAME_KEY = '$key'"

    val friendInfo = mDb.query(query).use { cursor =>
      if (cursor.moveToFirst()) {
        getFriendInfoFromCursor(cursor)
      } else {
        throw new IllegalArgumentException(s"Friend key $key not found.")
      }
    }

    friendInfo
  }

  def getGroupInfo(key: ContactKey): GroupInfo = {
    val query =
      s"SELECT * FROM $TABLE_CONTACTS WHERE $COLUMN_NAME_KEY = '$key'"

    val groupInfo = mDb.query(query).use { cursor =>
      if (cursor.moveToFirst()) {
        getGroupInfoFromCursor(cursor)
      } else {
        throw new IllegalArgumentException(s"Group key $key not found.")
      }
    }

    groupInfo
  }

  private def getFriendInfoFromCursor(cursor: Cursor): FriendInfo = {
    val name = ToxNickname.unsafeFromValue(cursor.getString(COLUMN_NAME_NAME).getBytes)
    val key = new FriendKey(cursor.getString(COLUMN_NAME_KEY))
    val status = cursor.getString(COLUMN_NAME_STATUS)
    val statusMessage = cursor.getString(COLUMN_NAME_NOTE)

    val alias =
      Option(cursor.getString(COLUMN_NAME_ALIAS))
        .flatMap(_.toOption)
        .map(_.getBytes)
        .map(ToxNickname.unsafeFromValue)

    val online = cursor.getBoolean(COLUMN_NAME_ISONLINE)
    val blocked = cursor.getBoolean(COLUMN_NAME_ISBLOCKED)
    val avatar = cursor.getString(COLUMN_NAME_AVATAR)
    val receievedAvatar = cursor.getBoolean(COLUMN_NAME_RECEIVED_AVATAR)
    val ignored = cursor.getBoolean(COLUMN_NAME_IGNORED)
    val favorite = cursor.getBoolean(COLUMN_NAME_FAVORITE)

    val file = AVATAR.getAvatarFile(avatar, ctx)

    new FriendInfo(online, name, alias, status, statusMessage, key, file, receievedAvatar, blocked, ignored, favorite)
  }

  private def getGroupInfoFromCursor(cursor: Cursor): GroupInfo = {
    val name = ToxNickname.unsafeFromValue(cursor.getString(COLUMN_NAME_NAME).getBytes)
    val key = new GroupKey(cursor.getString(COLUMN_NAME_KEY))
    val status = cursor.getString(COLUMN_NAME_STATUS)
    val topic = cursor.getString(COLUMN_NAME_NOTE)

    val alias =
      Option(cursor.getString(COLUMN_NAME_ALIAS))
        .flatMap(_.toOption)
        .map(_.getBytes)
        .map(ToxNickname.unsafeFromValue)

    val connected = cursor.getBoolean(COLUMN_NAME_ISONLINE)
    val blocked = cursor.getBoolean(COLUMN_NAME_ISBLOCKED)
    val avatar = cursor.getString(COLUMN_NAME_AVATAR)
    val receievedAvatar = cursor.getBoolean(COLUMN_NAME_RECEIVED_AVATAR)
    val ignored = cursor.getBoolean(COLUMN_NAME_IGNORED)
    val favorite = cursor.getBoolean(COLUMN_NAME_FAVORITE)

    new GroupInfo(key, connected, name, alias, topic, blocked, ignored, favorite)
  }

  def updateAlias(alias: String, key: ContactKey) {
    val where =
      s"$COLUMN_NAME_KEY ='$key'"
    mDb.update(TABLE_CONTACTS, contentValue(COLUMN_NAME_ALIAS, alias), where)
  }

  def isContactBlocked(key: ContactKey): Boolean = {
    val selectQuery = s"SELECT isBlocked FROM $TABLE_CONTACTS WHERE $COLUMN_NAME_KEY ='$key'"
    val isBlocked = mDb.query(selectQuery).use { cursor =>
      cursor.moveToFirst() && cursor.getInt(0) > 0
    }

    isBlocked
  }
}
