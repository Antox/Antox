package chat.tox.antox.utils

import java.io.File
import java.sql.Timestamp

import chat.tox.antox.fragments.ContactItemType.ContactItemType
import chat.tox.antox.wrapper.ContactKey
import im.tox.tox4j.core.enums.ToxUserStatus

class LeftPaneItem(
                    val viewType: ContactItemType,
                    val key: ContactKey,
                    val image: Option[File],
                    val first: String, // name
                    val second: String, // status message, last message or call info depending on which tab
                    val secondImage: Option[Int],
                    val isOnline: Boolean,
                    val status: ToxUserStatus,
                    val favorite: Boolean,
                    val count: Int,
                    val timestamp: Timestamp, // if call is active this is start time
                    val activeCall: Boolean) {

  def this(viewType: ContactItemType, key: ContactKey, message: String) =
    this(viewType, key, None, key.toString, message, None, false, null, false, 0, null, false)

}