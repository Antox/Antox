package im.tox.antox.utils

object Constants {

  val START_TOX = "im.tox.antox.START_TOX"

  val STOP_TOX = "im.tox.antox.STOP_TOX"

  val BROADCAST_ACTION = "im.tox.antox.BROADCAST"

  val SWITCH_TO_FRIEND = "im.tox.antox.SWITCH_TO_FRIEND"

  val UPDATE = "im.tox.antox.UPDATE"

  val DOWNLOAD_DIRECTORY = "Tox Received Files"

  val AVATAR_DIRECTORY = "avatars"

  val PROFILE_EXPORT_DIRECTORY = "Tox Exported Profiles"

  val DATABASE_VERSION = 12

  val USER_DATABASE_VERSION = 4

  val TABLE_CONTACTS = "contacts"

  val TABLE_CHAT_LOGS = "messages"

  val TABLE_FRIEND_REQUESTS = "friend_requests"

  val TABLE_GROUP_INVITES = "group_invites"

  val COLUMN_NAME_KEY = "tox_key"

  val COLUMN_NAME_GROUP_INVITER = "group_inviter"

  val COLUMN_NAME_GROUP_DATA = "group_data"

  val COLUMN_NAME_SENDER_NAME = "sender_name"

  val COLUMN_NAME_MESSAGE = "message"

  val COLUMN_NAME_NAME = "name"

  val COLUMN_NAME_USERNAME = "username"

  val COLUMN_NAME_TIMESTAMP = "timestamp"

  val COLUMN_NAME_NOTE = "note"

  val COLUMN_NAME_STATUS = "status"

  val COLUMN_NAME_MESSAGE_ID = "message_id"

  val COLUMN_NAME_HAS_BEEN_RECEIVED = "has_been_received"

  val COLUMN_NAME_HAS_BEEN_READ = "has_been_read"

  val COLUMN_NAME_SUCCESSFULLY_SENT = "successfully_sent"

  val COLUMN_NAME_FILE_KIND = "file_kind"

  val COLUMN_NAME_ISONLINE = "isonline"

  val COLUMN_NAME_ALIAS = "alias"

  val COLUMN_NAME_IGNORED = "ignored"

  val COLUMN_NAME_ISBLOCKED = "isblocked"

  val COLUMN_NAME_FAVORITE = "favorite"

  val COLUMN_NAME_AVATAR = "avatar"

  val COLUMN_NAME_CONTACT_TYPE = "contact_type"

  val COLUMN_NAME_RECEIVED_AVATAR = "received_avatar"

  val COLUMN_NAME_ACTIVE_MESSAGE = "active_message"

  val ADD_FRIEND_REQUEST_CODE = 0

  val WELCOME_ACTIVITY_REQUEST_CODE = 3

  val IMAGE_RESULT = 0

  val PHOTO_RESULT = 1

  val FILE_RESULT = 3

  val UNREAD_COUNT_LIMIT = 99

  val MAX_NAME_LENGTH = 128

  val MAX_MESSAGE_LENGTH = 1367 //in bytes

  val MAX_AVATAR_SIZE = 64 * 1024 //in bytes

}
