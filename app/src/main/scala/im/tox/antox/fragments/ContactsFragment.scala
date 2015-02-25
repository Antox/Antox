package im.tox.antox.fragments

import java.io.{FileOutputStream, PrintWriter, File}
import java.text.Collator
import java.util

import android.app.AlertDialog
import android.content.{Context, DialogInterface, Intent}
import android.os.{Environment, Bundle}
import android.support.v4.app.Fragment
import android.text.{Editable, TextWatcher}
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget._
import com.shamanland.fab.{FloatingActionButton, ShowHideOnScroll}
import im.tox.antoxnightly.R
import im.tox.antox.activities.{ChatActivity, FriendProfileActivity}
import im.tox.antox.adapters.LeftPaneAdapter
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.{Reactive, ToxSingleton}
import im.tox.antox.utils.FileDialog.DirectorySelectedListener
import im.tox.antox.utils._
import im.tox.tox4j.exceptions.ToxException
import rx.lang.scala.{Observable, Subscription}
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}

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
            val friend = new LeftPaneItem(f.key, f.name, f.statusMessage,
              f.isOnline, f.getFriendStatusAsToxUserStatus, f.unreadCount,
              f.lastMessageTimestamp)
            leftPaneAdapter.addItem(friend)
          }
        }
        contactsListView.setAdapter(leftPaneAdapter)
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
        val isContact = item.viewType == Constants.TYPE_CONTACT
        val items = if (isContact) {
          Array[CharSequence](getResources.getString(R.string.friend_action_profile),
            getResources.getString(R.string.friend_action_delete),
            getResources.getString(R.string.friend_action_export_chat),
            getResources.getString(R.string.friend_action_delete_chat))
        } else {
          Array[CharSequence]("")
        }
        builder.setTitle(getResources.getString(R.string.contacts_actions_on) +
          " " +
          item.first)
          .setCancelable(true)
          .setItems(items, new DialogInterface.OnClickListener() {

            def onClick(dialog: DialogInterface, index: Int) {
              if (isContact) {
                val key = item.key
                if (key != "") index match {
                  case 0 =>
                    val profile = new Intent(getActivity, classOf[FriendProfileActivity])
                    profile.putExtra("key", key)
                    startActivity(profile)

                  case 1 => showDeleteFriendDialog(getActivity, key)
                  case 2 => exportChat(getActivity, key)
                  case 3 => showDeleteChatDialog(getActivity, key)
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

      override def beforeTextChanged(charSequence: CharSequence, start: Int, count: Int, after: Int) {
      }

      override def onTextChanged(charSequence: CharSequence, start: Int, before: Int, count: Int) {
        if (leftPaneAdapter != null) leftPaneAdapter.getFilter().filter(charSequence)
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
                ToxSingleton.tox.deleteFriend(friend.getFriendnumber)
                ToxSingleton.save()
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

  def exportChat(context: Context, fkey: String) {
    val key = fkey
    val fileDialog = new FileDialog(this.getActivity, Environment.getExternalStorageDirectory, true)
    fileDialog.addDirectoryListener(new DirectorySelectedListener {
      override def directorySelected(directory: File): Unit = {
        try {
          println("exporting chat log")
          val db = new AntoxDB(getActivity)
          val messageList: util.ArrayList[Message] = db.getMessageList(key, actionMessages = true)
          val exportPath = directory.getPath + "/" + ToxSingleton.getAntoxFriend(key).get.name + "-" + key.substring(0, 7) + "-log.txt"

          val log = new PrintWriter(new FileOutputStream(exportPath, false))
          for (message: Message <- messageList) {
            val formattedMessage = message.logFormat()
            if (formattedMessage.isDefined)
              log.print(formattedMessage.get + '\n')
          }
          log.close()
          Toast.makeText(context, getResources.getString(R.string.friend_action_chat_log_exported, exportPath), Toast.LENGTH_SHORT).show()
          db.close()
        } catch {
          case e: Exception =>
            Toast.makeText(context, getResources.getString(R.string.friend_action_chat_log_export_failed), Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
      }
    })
    fileDialog.showDialog()
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
    Collator.getInstance().compare(a.getAliasOrName().toLowerCase, b.getAliasOrName().toLowerCase) < 0
  }

  def compareOnline(a: FriendInfo, b: FriendInfo): Boolean = {
    if (a.isOnline && !b.isOnline) true else false
  }
}
