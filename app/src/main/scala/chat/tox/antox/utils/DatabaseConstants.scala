package chat.tox.antox.utils

import android.content.ContentValues
import android.database.Cursor

object DatabaseConstants {

  val FALSE = 0

  val TRUE = 1

  val DATABASE_VERSION = 14

  val USER_DATABASE_VERSION = 5

  val TABLE_CONTACTS = "contacts"

  val TABLE_MESSAGES = "messages"

  val TABLE_FRIEND_REQUESTS = "friend_requests"

  val TABLE_GROUP_INVITES = "group_invites"

  val TABLE_USERS = "users"

  val COLUMN_NAME_KEY = "tox_key"

  val COLUMN_NAME_GROUP_INVITER = "group_inviter"

  val COLUMN_NAME_GROUP_DATA = "group_data"

  val COLUMN_NAME_SENDER_NAME = "sender_name"

  val COLUMN_NAME_MESSAGE = "message"

  val COLUMN_NAME_NAME = "name"

  val COLUMN_NAME_USERNAME = "username"

  val COLUMN_NAME_DNS_DOMAIN = "domain"

  val COLUMN_NAME_PASSWORD = "password"

  val COLUMN_NAME_NICKNAME = "nickname"

  val COLUMN_NAME_TIMESTAMP = "timestamp"

  val COLUMN_NAME_NOTE = "note"

  val COLUMN_NAME_STATUS_MESSAGE = "status_message"

  val COLUMN_NAME_LOGGING_ENABLED = "logging_enabled"

  val COLUMN_NAME_STATUS = "status"

  val COLUMN_NAME_TYPE = "type"

  val COLUMN_NAME_ID = "_id"

  val COLUMN_NAME_MESSAGE_ID = "message_id"

  val COLUMN_NAME_HAS_BEEN_RECEIVED = "has_been_received"

  val COLUMN_NAME_HAS_BEEN_READ = "has_been_read"

  val COLUMN_NAME_SUCCESSFULLY_SENT = "successfully_sent"

  val COLUMN_NAME_SIZE = "size"

  val COLUMN_NAME_FILE_KIND = "file_kind"

  val COLUMN_NAME_ISONLINE = "isonline"

  val COLUMN_NAME_ALIAS = "alias"

  val COLUMN_NAME_IGNORED = "ignored"

  val COLUMN_NAME_ISBLOCKED = "isblocked"

  val COLUMN_NAME_FAVORITE = "favorite"

  val COLUMN_NAME_AVATAR = "avatar"

  val COLUMN_NAME_CONTACT_TYPE = "contact_type"

  val COLUMN_NAME_RECEIVED_AVATAR = "received_avatar"

  val COLUMN_NAME_UNSENT_MESSAGE = "unsent_message"


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
      cursor.getString(cursor.getColumnIndexOrThrow(columnName))
    }

    def getInt(columnName: String): Int = {
      cursor.getInt(cursor.getColumnIndexOrThrow(columnName))
    }

    def getBoolean(columnName: String): Boolean = {
      cursor.getInt(cursor.getColumnIndexOrThrow(columnName)) > 0
    }
  }
}