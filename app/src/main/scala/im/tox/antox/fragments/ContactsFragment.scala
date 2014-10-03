package im.tox.antox.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.Adapter
import android.widget.AdapterView
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import com.shamanland.fab.FloatingActionButton
import com.shamanland.fab.ShowHideOnScroll
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import im.tox.antox.R
import im.tox.antox.activities.FriendProfileActivity
import im.tox.antox.activities.ChatActivity
import im.tox.antox.adapters.LeftPaneAdapter
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.tox.Reactive
import im.tox.antox.utils.AntoxFriend
import im.tox.antox.utils.Constants
import im.tox.antox.utils.FriendInfo
import im.tox.antox.utils.FriendRequest
import im.tox.antox.utils.LeftPaneItem
import im.tox.antox.utils.Tuple
import im.tox.jtoxcore.FriendExistsException
import im.tox.jtoxcore.ToxException
import rx.lang.scala.JavaConversions
import rx.lang.scala.Observable
import rx.lang.scala.Observer
import rx.lang.scala.Subscriber
import rx.lang.scala.Subscription
import rx.lang.scala.Subject
import rx.lang.scala.schedulers.IOScheduler
import rx.lang.scala.schedulers.AndroidMainThreadScheduler
//remove if not needed
import scala.collection.JavaConversions._

class ContactsFragment extends Fragment {

  private var contactsListView: ListView = _

  private var leftPaneAdapter: LeftPaneAdapter = _

  private var friendInfoSub: Subscription = _

  private var activeKey: String = _

  def updateContacts(friendsTuple: (Array[FriendInfo], Array[FriendRequest])) {
    friendsTuple match {
      case (friendsList, friendRequests) => {
        val sortedFriendsList = friendsList.sortWith(compareNames).sortWith(compareOnline)
        leftPaneAdapter = new LeftPaneAdapter(getActivity)
        if (friendRequests.length > 0) {
          leftPaneAdapter.addItem(new LeftPaneItem(getResources.getString(R.string.contacts_delimiter_requests)))
          for (r <- friendRequests) {
            val request = new LeftPaneItem(r.requestKey, r.requestMessage)
            leftPaneAdapter.addItem(request)
          }
        }
        if (sortedFriendsList.length > 0) {
          var onlineAdded = false
          var offlineAdded = false
          for (f <- sortedFriendsList) {
            if (!offlineAdded && !f.isOnline) {
              leftPaneAdapter.addItem(new LeftPaneItem(getResources.getString(R.string.contacts_delimiter_offline)))
              offlineAdded = true
            }
            if (!onlineAdded && f.isOnline) {
              leftPaneAdapter.addItem(new LeftPaneItem(getResources.getString(R.string.contacts_delimiter_online)))
              onlineAdded = true
            }
            val friend = new LeftPaneItem(f.friendKey, f.friendName, f.lastMessage,
              f.isOnline, f.getFriendStatusAsToxUserStatus, f.unreadCount,
              f.lastMessageTimestamp)
            leftPaneAdapter.addItem(friend)
          }
        }
        contactsListView.setAdapter(leftPaneAdapter)
        println("updated contacts")
      }
    }
  }

  override def onResume() {
    super.onResume()
    friendInfoSub = Reactive.friendListAndRequests.observeOn(AndroidMainThreadScheduler())
      .subscribe(updateContacts(_))
    val fab = getActivity.findViewById(R.id.fab).asInstanceOf[FloatingActionButton]
    fab.setSize(FloatingActionButton.SIZE_NORMAL)
    fab.setColor(R.color.fab_normal)
    fab.initBackground()
    fab.setImageResource(R.drawable.ic_action_new)
    contactsListView.setOnTouchListener(new ShowHideOnScroll(fab))
  }

  override def onPause() {
    super.onPause()
    friendInfoSub.unsubscribe()
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val rootView = inflater.inflate(R.layout.fragment_contacts, container, false)
    contactsListView = rootView.findViewById(R.id.contacts_list).asInstanceOf[ListView]
    contactsListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE)
    contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

      override def onItemClick(parent: AdapterView[_],
        view: View,
        position: Int,
        id: Long) {
        val item = parent.getAdapter.asInstanceOf[Adapter].getItem(position).asInstanceOf[LeftPaneItem]
        val `type` = item.viewType
        if (`type` != Constants.TYPE_FRIEND_REQUEST) {
          val key = item.key
          if (key != "") {
            ToxSingleton.changeActiveKey(key)
            val intent = new Intent(getActivity, classOf[ChatActivity])
            intent.putExtra("key", key)
            startActivity(intent)
          }
        }
      }
    })
    contactsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

      override def onItemLongClick(parent: AdapterView[_],
        itemView: View,
        index: Int,
        id: Long): Boolean = {
        val item = parent.getAdapter.asInstanceOf[Adapter].getItem(index).asInstanceOf[LeftPaneItem]
        val builder = new AlertDialog.Builder(getActivity)
        val isFriendRequest = item.viewType == Constants.TYPE_FRIEND_REQUEST
        var items = if (!isFriendRequest) {
          Array[CharSequence](getResources.getString(R.string.friend_action_profile), getResources.getString(R.string.friend_action_delete), getResources.getString(R.string.friend_action_delete_chat), getResources.getString(R.string.contacts_resend_friend_request))
        } else {
          Array[CharSequence]("")
        }
        builder.setTitle(getResources.getString(R.string.contacts_actions_on) +
          " " +
          item.first)
          .setCancelable(true)
          .setItems(items, new DialogInterface.OnClickListener() {

            def onClick(dialog: DialogInterface, index: Int) {
              if (!isFriendRequest) {
                val key = item.key
                if (key != "") index match {
                  case 0 =>
                    var profile = new Intent(getActivity, classOf[FriendProfileActivity])
                    profile.putExtra("key", key)
                    startActivity(profile)

                  case 1 => showDeleteFriendDialog(getActivity, key)
                  case 2 => showDeleteChatDialog(getActivity, key)
                  case 3 => try {
                    ToxSingleton.jTox.addFriend(key, getResources.getString(R.string.addfriend_default_message))
                  } catch {
                    case e: ToxException =>
                    case e: FriendExistsException =>
                  }
                }
              }
              dialog.cancel()
            }
          })
        val alert = builder.create()
        if (item != null) {
          if (item.viewType != Constants.TYPE_HEADER) {
            alert.show()
          }
        }
        return true
      }
    })
    val search = rootView.findViewById(R.id.searchBar).asInstanceOf[EditText]
    search.addTextChangedListener(new TextWatcher() {

      override def beforeTextChanged(charSequence: CharSequence,
        i: Int,
        i2: Int,
        i3: Int) {
      }

      override def onTextChanged(charSequence: CharSequence,
        i: Int,
        i2: Int,
        i3: Int) {
        if (leftPaneAdapter != null) leftPaneAdapter.getFilter.filter(charSequence)
      }

      override def afterTextChanged(editable: Editable) {
      }
    })
    rootView
  }

  def showDeleteFriendDialog(context: Context, fkey: String) {
    val key = fkey
    val delete_friend_dialog = View.inflate(context, R.layout.dialog_delete_friend, null)
    val deleteLogsCheckboxView = delete_friend_dialog.findViewById(R.id.deleteChatLogsCheckBox).asInstanceOf[CheckBox]
    val builder = new AlertDialog.Builder(context)
    builder.setView(delete_friend_dialog).setCancelable(false)
      .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

        def onClick(dialog: DialogInterface, id: Int) {
          Observable[Boolean](subscriber => {
            val db = new AntoxDB(getActivity)
            if (deleteLogsCheckboxView.isChecked) db.deleteChat(key)
            db.deleteFriend(key)
            db.close()
            val mFriend = ToxSingleton.getAntoxFriend(key)
            mFriend.foreach(friend => {
              try {
                ToxSingleton.jTox.deleteFriend(friend.getFriendnumber)
              } catch {
                case e: ToxException =>
              }
            })
            subscriber.onCompleted()
            ToxSingleton.updateFriendsList(getActivity)
            ToxSingleton.updateMessages(getActivity)
          }).subscribeOn(IOScheduler()).subscribe()
        }
      })
      .setNegativeButton("No", new DialogInterface.OnClickListener() {

        def onClick(dialog: DialogInterface, id: Int) {
          dialog.cancel()
        }
      })
    builder.show()
  }

  def showDeleteChatDialog(context: Context, fkey: String) {
    val key = fkey
    val builder = new AlertDialog.Builder(context)
    builder.setMessage(getResources.getString(R.string.friend_action_delete_chat_confirmation))
      .setCancelable(false)
      .setPositiveButton(getResources.getString(R.string.button_yes), new DialogInterface.OnClickListener() {

        def onClick(dialog: DialogInterface, id: Int) {
          val db = new AntoxDB(getActivity)
          db.deleteChat(key)
          db.close()
          ToxSingleton.updateMessages(getActivity)
        }
      })
      .setNegativeButton(getResources.getString(R.string.button_no), new DialogInterface.OnClickListener() {

        def onClick(dialog: DialogInterface, id: Int) {
        }
      })
    builder.show()
  }

  def compareNames(a: FriendInfo, b: FriendInfo): Boolean = {
    if (a.alias != "") {
      if (b.alias != "") (a.alias.toUpperCase().compareTo(b.alias.toUpperCase()) == -1) else (a.alias.toUpperCase().compareTo(b.friendName.toUpperCase()) == -1)
    } else {
      if (b.alias != "") (a.friendName.toUpperCase().compareTo(b.alias.toUpperCase()) == -1) else (a.friendName.toUpperCase().compareTo(b.friendName.toUpperCase()) == -1)
    }
  }

  def compareOnline(a: FriendInfo, b: FriendInfo): Boolean = {
    if (a.isOnline && !b.isOnline) true else false
  }
}
