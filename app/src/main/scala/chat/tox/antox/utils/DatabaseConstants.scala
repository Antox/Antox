package chat.tox.antox.utils

import android.content.ContentValues
import android.database.Cursor

object DatabaseConstants {

  object RowOrder extends Enumeration {
    type RowOrder = Value
    val ASCENDING = Value("ASC")
    val DESCENDING = Value("DESC")
  }

  val FALSE = 0

  val TRUE = 1

  val DATABASE_VERSION = 16

  val USER_DATABASE_VERSION = 5

  /**
   * Table used for storing all contacts and contact-related information, including friends and groups (may store group peers in the future).
   * Friends which have not yet accepted a sent friend request and groups which are not yet connected to are also stored in this table.
   */
  val TABLE_CONTACTS = "contacts"

  /**
   * Table used to store messages.
   * When messages are deleted they are removed from the table.
   */
  val TABLE_MESSAGES = "messages"

  /**
   * Table used to store incoming friend requests.
   * When the FR is accepted or rejected it is removed from the table.
   */
  val TABLE_FRIEND_REQUESTS = "friend_requests"

  /**
   * Table used to store group invites.
   */
  val TABLE_GROUP_INVITES = "group_invites"

  /**
   * Table used by [[chat.tox.antox.data.UserDB]] to store profile information.
   * When a profile is removed it is deleted from the table.
   */
  val TABLE_USERS = "users"

  val COLUMN_NAME_KEY = "tox_key"

  /**
   * [[chat.tox.antox.wrapper.ToxKey]] indicating the sender of a message. Will always correspond to a key in [[TABLE_CONTACTS]].
   */
  val COLUMN_NAME_SENDER_KEY = "sender_key"

  val COLUMN_NAME_GROUP_INVITER = "group_inviter"

  val COLUMN_NAME_GROUP_DATA = "group_data"

  val COLUMN_NAME_SENDER_NAME = "sender_name"

  val COLUMN_NAME_MESSAGE = "message"

  val COLUMN_NAME_PROFILE_NAME = "username"

  val COLUMN_NAME_NAME = "name"

  val COLUMN_NAME_TOXME_DOMAIN = "domain"

  /**
   * ToxMe password for this profile.
   */
  val COLUMN_NAME_PASSWORD = "password"

  val COLUMN_NAME_NICKNAME = "nickname"

  val COLUMN_NAME_TIMESTAMP = "timestamp"

  /**
   * Status message in [[chat.tox.antox.data.AntoxDB]]
   */
  val COLUMN_NAME_NOTE = "note"

  /**
   * Status message in [[chat.tox.antox.data.UserDB]].
   */
  val COLUMN_NAME_STATUS_MESSAGE = "status_message"

  /**
   * Whether or not the chat logs for this profile will be kept on logout.
   * Defaults to true as it is mirrored by a preference.
   */
  val COLUMN_NAME_LOGGING_ENABLED = "logging_enabled"

  val COLUMN_NAME_STATUS = "status"

  val COLUMN_NAME_TYPE = "type"

  val COLUMN_NAME_ID = "_id"

  val COLUMN_NAME_MESSAGE_ID = "message_id"

  val COLUMN_NAME_HAS_BEEN_RECEIVED = "has_been_received"

  val COLUMN_NAME_HAS_BEEN_READ = "has_been_read"

  val COLUMN_NAME_SUCCESSFULLY_SENT = "successfully_sent"

  val COLUMN_NAME_SIZE = "size"

  /**
   * File kind of a message. [[chat.tox.antox.wrapper.FileKind.INVALID]] if the message is not a file.
   */
  val COLUMN_NAME_FILE_KIND = "file_kind"

  val COLUMN_NAME_ISONLINE = "isonline"

  val COLUMN_NAME_ALIAS = "alias"

  val COLUMN_NAME_IGNORED = "ignored"

  val COLUMN_NAME_ISBLOCKED = "isblocked"

  val COLUMN_NAME_FAVORITE = "favorite"

  val COLUMN_NAME_AVATAR = "avatar"

  val COLUMN_NAME_CONTACT_TYPE = "contact_type"

  /**
   * Whether this contact has received our latest avatar.
   */
  val COLUMN_NAME_RECEIVED_AVATAR = "received_avatar"

  val COLUMN_NAME_UNSENT_MESSAGE = "unsent_message"

  val COLUMN_NAME_SENDER_CONTACT_TYPE = "sender_contact_type"

  val COLUMN_NAME_CONVERSATION_CONTACT_TYPE = "conversation_contact_type"

  def createSqlEqualsCondition(columnName: String, list: Iterable[_], tableName: String = ""): String = {
    val table = if (!tableName.isEmpty) tableName + "." else ""

    "(" + list.map(i => s"$table$columnName == " + i.toString).mkString(" OR ") + ")"
  }

  def contentValue(key: String, value: String): ContentValues = {
    val values = new ContentValues()
    values.put(key, value)
    values
  }

  def contentValue(key: String, value: Int): ContentValues = {
    val values = new ContentValues()
    values.put(key, value.asInstanceOf[java.lang.Integer])
    values
  }

  implicit class RichCursor(val cursor: Cursor) extends AnyVal {
    def getString(columnName: String): String = {
      val index = cursor.getColumnIndexOrThrow(columnName)
      cursor.getString(index)
    }

    def getInt(columnName: String): Int = {
      val index = cursor.getColumnIndexOrThrow(columnName)
      cursor.getInt(index)
    }

    def maybeGetInt(columnName: String): Option[Int] = {
      val index = cursor.getColumnIndex(columnName)
      if (index < 0 || cursor.getType(index) == Cursor.FIELD_TYPE_NULL) {
        None
      } else {
        Option(cursor.getInt(index))
      }
    }

    def getBoolean(columnName: String): Boolean = {
      val index = cursor.getColumnIndexOrThrow(columnName)
      cursor.getInt(index) > 0
    }

    def getBlob(columnName: String): Array[Byte] = {
      val index = cursor.getColumnIndexOrThrow(columnName)
      cursor.getBlob(index)
    }
  }
}