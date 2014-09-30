package im.tox.antox.utils

//remove if not needed
import scala.collection.JavaConversions._

object Constants {

  val TYPE_HEADER = 0

  val TYPE_FRIEND_REQUEST = 1

  val TYPE_CONTACT = 2

  val TYPE_MAX_COUNT = 3

  val START_TOX = "im.tox.antox.START_TOX"

  val STOP_TOX = "im.tox.antox.STOP_TOX"

  val BROADCAST_ACTION = "im.tox.antox.BROADCAST"

  val SWITCH_TO_FRIEND = "im.tox.antox.SWITCH_TO_FRIEND"

  val UPDATE = "im.tox.antox.UPDATE"

  val DOWNLOAD_DIRECTORY = "Tox received files"

  val DATABASE_VERSION = 1

  val TABLE_FRIENDS = "friends"

  val TABLE_CHAT_LOGS = "messages"

  val TABLE_FRIEND_REQUEST = "friend_requests"

  val COLUMN_NAME_KEY = "tox_key"

  val COLUMN_NAME_MESSAGE = "message"

  val COLUMN_NAME_USERNAME = "username"

  val COLUMN_NAME_TIMESTAMP = "timestamp"

  val COLUMN_NAME_NOTE = "note"

  val COLUMN_NAME_STATUS = "status"

  val COLUMN_NAME_MESSAGE_ID = "message_id"

  val COLUMN_NAME_HAS_BEEN_RECEIVED = "has_been_received"

  val COLUMN_NAME_HAS_BEEN_READ = "has_been_read"

  val COLUMN_NAME_SUCCESSFULLY_SENT = "successfully_sent"

  val COLUMN_NAME_ISONLINE = "isonline"

  val COLUMN_NAME_ALIAS = "alias"

  val COLUMN_NAME_ISBLOCKED = "isblocked"

  val ADD_FRIEND_REQUEST_CODE = 0

  val WELCOME_ACTIVITY_REQUEST_CODE = 3

  val IMAGE_RESULT = 0

  val PHOTO_RESULT = 1

  val FILE_RESULT = 3

  val MESSAGE_TYPE_OWN = 1

  val MESSAGE_TYPE_FRIEND = 2

  val MESSAGE_TYPE_FILE_TRANSFER = 3

  val MESSAGE_TYPE_FILE_TRANSFER_FRIEND = 4

  val MESSAGE_TYPE_ACTION = 5

  var epoch: Long = _
}
