package chat.tox.antox.utils

import java.io.File
import java.sql.Timestamp

import chat.tox.antox.fragments.ContactItemType
import chat.tox.antox.fragments.ContactItemType.ContactItemType
import chat.tox.antox.wrapper.ContactKey
import im.tox.tox4j.core.enums.ToxUserStatus

class LeftPaneItem(
  val viewType: ContactItemType,
  val key: ContactKey,
  val image: Option[File],
  val first: String, // name
  val second: String, // status message, or last message depending on which tab
  val isOnline: Boolean,
  val status: ToxUserStatus,
  val favorite: Boolean,
  val count: Int,
  val timestamp: Timestamp) {

  def this(
    key: ContactKey,
    image: Option[File],
    first: String,
    second: String,
    isOnline: Boolean,
    status: ToxUserStatus,
    favorite: Boolean,
    count: Int,
    timestamp: Timestamp) =
    this(ContactItemType.FRIEND, key, image, first, second, isOnline, status, favorite, count, timestamp)

  def this(viewType: ContactItemType, key: ContactKey, message: String) =
    this(viewType, key, None, key.toString, message, false, null, false, 0, null)

}