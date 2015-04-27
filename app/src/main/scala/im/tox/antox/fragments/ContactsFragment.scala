package im.tox.antox.fragments

import android.os.Bundle
import android.view.{LayoutInflater, View, ViewGroup}
import im.tox.antoxnightly.R
import im.tox.antox.adapters.ContactListAdapter
import im.tox.antox.utils.{UIUtils, LeftPaneItem}
import im.tox.antox.wrapper.{FriendInfo, FriendRequest, GroupInfo, GroupInvite}
import im.tox.tox4j.core.enums.ToxStatus

class ContactsFragment extends AbstractContactsFragment(showSearch = true, showFab = true) {

  override def updateContacts(contactInfoTuple: (Array[FriendInfo], Array[FriendRequest],
    Array[GroupInvite], Array[GroupInfo])) {
    contactInfoTuple match {
      case (friendsList, friendRequests, groupInvites, groupList) =>
        leftPaneAdapter = new ContactListAdapter(getActivity)
        updateFriendsList(leftPaneAdapter, friendsList)
        updateFriendRequests(leftPaneAdapter, friendRequests)
        updateGroupInvites(leftPaneAdapter, groupInvites)
        updateGroupList(leftPaneAdapter, groupList)

        contactsListView.setAdapter(leftPaneAdapter)
    }
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val rootView = super.onCreateView(inflater, container, savedInstanceState)
    rootView.findViewById(R.id.center_text).setVisibility(View.GONE)
    rootView
  }

  def updateFriendsList(leftPaneAdapter: ContactListAdapter, friendsList: Array[FriendInfo]): Unit = {
    val sortedFriendsList = friendsList.sortWith(compareNames).sortWith(compareOnline)
    if (sortedFriendsList.length > 0) {
      var onlineAdded = false
      var offlineAdded = false
      for (f <- sortedFriendsList) {
        if (!offlineAdded && !f.online) {
          leftPaneAdapter.addItem(new LeftPaneItem(getResources.getString(R.string.contacts_delimiter_offline)))
          offlineAdded = true
        }
        if (!onlineAdded && f.online) {
          leftPaneAdapter.addItem(new LeftPaneItem(getResources.getString(R.string.contacts_delimiter_online)))
          onlineAdded = true
        }
        val friend = new LeftPaneItem(f.key, f.avatar, f.name, f.statusMessage,
          f.online, f.getFriendStatusAsToxUserStatus, f.unreadCount,
          f.lastMessageTimestamp)
        leftPaneAdapter.addItem(friend)
      }
    }
  }

  def updateFriendRequests(leftPaneAdapter: ContactListAdapter, friendRequests: Array[FriendRequest]): Unit = {
    if (friendRequests.length > 0) {
      leftPaneAdapter.addItem(new LeftPaneItem(getResources.getString(R.string.contacts_delimiter_requests)))
      for (r <- friendRequests) {
        val request = new LeftPaneItem(ContactItemType.FRIEND_REQUEST, r.requestKey, r.requestMessage)
        leftPaneAdapter.addItem(request)
      }
    }
  }

  def updateGroupInvites(leftPaneAdapter: ContactListAdapter, groupInvites: Array[GroupInvite]): Unit = {
    if (groupInvites.length > 0) {
      leftPaneAdapter.addItem(new LeftPaneItem(getResources.getString(R.string.contacts_delimiter_invites)))
      for (invite <- groupInvites) {
        val request = new LeftPaneItem(ContactItemType.GROUP_INVITE, invite.groupId, getResources.getString(R.string.invited_by) + " " + invite.inviter)
        leftPaneAdapter.addItem(request)
      }
    }
  }

  def updateGroupList(leftPaneAdapter: ContactListAdapter, groups: Array[GroupInfo]): Unit = {
    println("update group list " + groups.length)
    if (groups.length > 0) {
      leftPaneAdapter.addItem(new LeftPaneItem(getResources.getString(R.string.contacts_delimiter_groups)))
      for (group <- groups) {
        val groupPane: LeftPaneItem = new LeftPaneItem(ContactItemType.GROUP, group.id, group.avatar, group.name, group.topic,
          group.connected, ToxStatus.NONE, group.unreadCount, group.lastMessageTimestamp)
        leftPaneAdapter.addItem(groupPane)
      }
    }
  }
}
