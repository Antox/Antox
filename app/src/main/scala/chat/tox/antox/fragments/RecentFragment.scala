package chat.tox.antox.fragments

import android.os.Bundle
import android.view.{LayoutInflater, View, ViewGroup}
import chat.tox.antox.R
import chat.tox.antox.adapters.ContactListAdapter
import chat.tox.antox.utils.LeftPaneItem
import chat.tox.antox.wrapper._

class RecentFragment extends AbstractContactsFragment(showSearch = false, showFab = false) {

  override def updateContacts(contactInfoTuple: (Seq[FriendInfo], Seq[FriendRequest],
    Seq[GroupInvite], Seq[GroupInfo])) {
    contactInfoTuple match {
      case (friendsList, friendRequests, groupInvites, groupList) =>
        leftPaneAdapter = new ContactListAdapter(getActivity)
        updateContactsLists(leftPaneAdapter, friendsList ++ groupList)

        contactsListView.setAdapter(leftPaneAdapter)
    }
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val rootView = super.onCreateView(inflater, container, savedInstanceState)
    rootView.findViewById(R.id.center_text).setVisibility(View.VISIBLE)

    rootView
  }

  def updateContactsLists(leftPaneAdapter: ContactListAdapter, contactList: Seq[ContactInfo]): Unit = {
    val sortedContactList = contactList.filter(c => c.lastMessage != "").sortWith(compareNames).sortWith(compareLastMessageTimestamp)
    if (sortedContactList.nonEmpty) {
      getView.findViewById(R.id.center_text).setVisibility(View.GONE)
      for (contact <- sortedContactList) {
        val itemType = if (contact.isInstanceOf[GroupInfo]) {
          ContactItemType.GROUP
        } else {
          ContactItemType.FRIEND
        }

        val contactPaneItem = new LeftPaneItem(itemType, contact.key, contact.avatar, contact.getAliasOrName, contact.lastMessage,
          contact.online, UserStatus.getToxUserStatusFromString(contact.status), contact.favorite, contact.unreadCount,
          contact.lastMessageTimestamp)
        leftPaneAdapter.addItem(contactPaneItem)
      }
    } else {
      getView.findViewById(R.id.center_text).setVisibility(View.VISIBLE)
    }
  }

  def compareLastMessageTimestamp(a: ContactInfo, b: ContactInfo): Boolean = {
    a.lastMessageTimestamp.after(b.lastMessageTimestamp)
  }
}
