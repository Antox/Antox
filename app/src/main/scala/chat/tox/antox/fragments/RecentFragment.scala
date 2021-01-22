package chat.tox.antox.fragments

import java.sql.Timestamp

import android.os.Bundle
import android.view.{LayoutInflater, View, ViewGroup}
import chat.tox.antox.R
import chat.tox.antox.adapters.ContactListAdapter
import chat.tox.antox.av.Call
import chat.tox.antox.data.CallEventKind
import chat.tox.antox.utils.{LeftPaneItem, TimestampUtils}
import chat.tox.antox.wrapper._

class RecentFragment extends AbstractContactsFragment(showSearch = false, showFab = false) {

  override def updateContacts(contactInfoTuple: (Seq[FriendInfo], Seq[FriendRequest],
    Seq[GroupInvite], Seq[GroupInfo]), activeCalls: Iterable[Call]) {
    contactInfoTuple match {
      case (friendsList, friendRequests, groupInvites, groupList) =>
        leftPaneAdapter = new ContactListAdapter(getActivity)
        updateContactsLists(leftPaneAdapter, friendsList ++ groupList, activeCalls)
        contactsListView.setAdapter(leftPaneAdapter)
    }
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val rootView = super.onCreateView(inflater, container, savedInstanceState)
    rootView.findViewById(R.id.center_text).setVisibility(View.VISIBLE)

    rootView
  }

  def updateContactsLists(leftPaneAdapter: ContactListAdapter, contactList: Seq[ContactInfo], activeCalls: Iterable[Call]): Unit = {
    val sortedContactList = contactList.filter(c => c.lastMessage.isDefined).sortWith(compareNames).sortWith(compareLastMessageTimestamp)
    if (sortedContactList.nonEmpty) {
      getView.findViewById(R.id.center_text).setVisibility(View.GONE)
      for (contact <- sortedContactList) {
        val itemType = if (contact.isInstanceOf[GroupInfo]) {
          ContactItemType.GROUP
        } else {
          ContactItemType.FRIEND
        }

        val activeCall = activeCalls.find(_.contactKey == contact.key).filterNot(_.ringing)
        val (timestamp, second, secondImage) =
          activeCall match {
            case Some(call) if call.active =>
              (new Timestamp(call.duration.toMillis), getResources.getString(R.string.call_ongoing), Some(CallEventKind.Answered.imageRes))
            case None =>
              val stamp = contact.lastMessage.map(_.timestamp).getOrElse(TimestampUtils.emptyTimestamp())
              (stamp, contact.lastMessage.map(_.toNotificationFormat(getActivity)).getOrElse(""), getSecondImage(contact))
          }

        val lastMessage = contact.lastMessage.get
        val contactPaneItem = new LeftPaneItem(itemType, contact.key, contact.avatar, contact.getDisplayName, second,
          secondImage, contact.online, UserStatus.getToxUserStatusFromString(contact.status), contact.favorite, contact.unreadCount,
          timestamp, activeCall.isDefined)

        if (activeCall.isDefined) {
          leftPaneAdapter.insert(0, contactPaneItem) // active call goes to the top
        } else {
          leftPaneAdapter.addItem(contactPaneItem)
        }
      }
    } else {
      getView.findViewById(R.id.center_text).setVisibility(View.VISIBLE)
    }
  }

  def compareLastMessageTimestamp(a: ContactInfo, b: ContactInfo): Boolean = {
    def lastMessageTimstamp(info: ContactInfo): Timestamp =
      info.lastMessage.map(_.timestamp).getOrElse(TimestampUtils.emptyTimestamp())

    lastMessageTimstamp(a).after(lastMessageTimstamp(b))
  }
}
