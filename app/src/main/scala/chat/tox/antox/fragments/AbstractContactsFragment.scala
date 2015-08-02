package chat.tox.antox.fragments

import java.io.{File, FileOutputStream, PrintWriter}
import java.text.Collator

import android.app.AlertDialog
import android.content.{Context, DialogInterface, Intent}
import android.os.{Bundle, Environment}
import android.support.v4.app.Fragment
import android.text.{Editable, TextWatcher}
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget._
import chat.tox.antox.R
import chat.tox.antox.activities.{ChatActivity, FriendProfileActivity, GroupChatActivity}
import chat.tox.antox.adapters.ContactListAdapter
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.transfer.FileDialog
import chat.tox.antox.transfer.FileDialog.DirectorySelectedListener
import chat.tox.antox.utils._
import chat.tox.antox.wrapper._
import com.shamanland.fab.{FloatingActionButton, ShowHideOnScroll}
import im.tox.tox4j.exceptions.ToxException
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}
import rx.lang.scala.{Observable, Subscription}

abstract class AbstractContactsFragment extends Fragment {

  var showSearch: Boolean = _

  var showFab: Boolean = true

  protected var contactsListView: ListView = _

  protected var leftPaneAdapter: ContactListAdapter = _

  protected var contactChangeSub: Subscription = _

  protected var activeKey: ToxKey = _

  def this (showSearch: Boolean, showFab: Boolean) {
    this()
    this.showSearch = showSearch
    this.showFab = showFab
  }

  def updateContacts(contactInfoTuple: (Seq[FriendInfo], Seq[FriendRequest],
    Seq[GroupInvite], Seq[GroupInfo]))

  override def onResume() {
    super.onResume()
    val db = State.db
    contactChangeSub = db.contactListElements.observeOn(AndroidMainThreadScheduler())
      .subscribe(updateContacts(_))
  }

  override def onPause() {
    super.onPause()
    contactChangeSub.unsubscribe()
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
        if (`type` != ContactItemType.FRIEND_REQUEST && `type` != ContactItemType.GROUP_INVITE) {
          val key = item.key
            ToxSingleton.changeActiveKey(key)
            val intent = if (`type` == ContactItemType.FRIEND) {
              new Intent(getActivity, classOf[ChatActivity])
            } else {
              new Intent(getActivity, classOf[GroupChatActivity])
            }
            intent.putExtra("key", key.toString)
            startActivity(intent)

        }
      }
    })

    if (showFab) {
      val fab = rootView.findViewById(R.id.fab).asInstanceOf[FloatingActionButton]
      fab.setSize(FloatingActionButton.SIZE_NORMAL)
      fab.setColor(getResources.getColor(R.color.fab_normal))
      fab.initBackground()
      fab.setImageResource(R.drawable.ic_action_new)
      contactsListView.setOnTouchListener(new ShowHideOnScroll(fab))
    } else {
      rootView.findViewById(R.id.fab).setVisibility(View.GONE)
    }

    contactsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

      override def onItemLongClick(parent: AdapterView[_],
        itemView: View,
        index: Int,
        id: Long): Boolean = {
        val item = parent.getAdapter.asInstanceOf[Adapter].getItem(index).asInstanceOf[LeftPaneItem]
        createLeftPanePopup(item)
        true
      }
    })
    val search = rootView.findViewById(R.id.searchBar).asInstanceOf[EditText]
    if (showSearch) {
      search.addTextChangedListener(new TextWatcher {

        override def beforeTextChanged(charSequence: CharSequence, start: Int, count: Int, after: Int) {
        }

        override def onTextChanged(charSequence: CharSequence, start: Int, before: Int, count: Int) {
          if (leftPaneAdapter != null) leftPaneAdapter.getFilter().filter(charSequence)
        }

        override def afterTextChanged(editable: Editable) {
        }
      })
    } else {
      rootView.findViewById(R.id.contact_search_view).setVisibility(View.GONE)
    }

    rootView.findViewById(R.id.center_text).setVisibility(View.GONE)

    rootView
  }

  def createLeftPanePopup(parentItem: LeftPaneItem): Unit =  {
    val items = if (parentItem.viewType == ContactItemType.FRIEND) {
      Array[CharSequence](getResources.getString(R.string.friend_action_profile),
        getResources.getString(R.string.friend_action_delete),
        getResources.getString(R.string.friend_action_export_chat),
        getResources.getString(R.string.friend_action_delete_chat))
    } else if (parentItem.viewType == ContactItemType.GROUP) {
      Array[CharSequence](getResources.getString(R.string.group_action_delete))
    } else {
      Array[CharSequence]("")
    }

    val builder = new AlertDialog.Builder(getActivity, R.style.AppCompatAlertDialogStyle)

    builder.setTitle(getResources.getString(R.string.contacts_actions_on) +
      " " +
      parentItem.first)
      .setCancelable(true)
      .setItems(items, new DialogInterface.OnClickListener() {

      def onClick(dialog: DialogInterface, index: Int) {
        val key = parentItem.key
        if (parentItem.viewType == ContactItemType.FRIEND) {
          index match {
            case 0 =>
              val profile = new Intent(getActivity, classOf[FriendProfileActivity])
              profile.putExtra("key", key.toString)
              profile.putExtra("avatar", parentItem.image)
              profile.putExtra("name", parentItem.first)
              startActivity(profile)

            case 1 => showDeleteFriendDialog(getActivity, key)
            case 2 => exportChat(getActivity, key)
            case 3 => showDeleteChatDialog(getActivity, key)
          }
        }

        if (parentItem.viewType == ContactItemType.GROUP) {
          index match {
            case 0 =>
              val db = State.db
              db.deleteChatLogs(key)
              db.deleteContact(key)
              val group = ToxSingleton.getGroupList.getGroup(key)
              try {
                group.leave(getResources.getString(R.string.group_default_part_message))
              } catch {
                case e: ToxException[_] =>
              }

              ToxSingleton.save()
          }
        }
        dialog.cancel()
      }
    })

    val alert = builder.create()
    if (parentItem != null) {
      alert.show()
    }
  }

  def showDeleteFriendDialog(context: Context, fkey: ToxKey) {
    val key = fkey
    val delete_friend_dialog = View.inflate(context, R.layout.dialog_delete_friend, null)
    val deleteLogsCheckboxView = delete_friend_dialog.findViewById(R.id.deleteChatLogsCheckBox).asInstanceOf[CheckBox]
    val builder = new AlertDialog.Builder(context)
    builder.setView(delete_friend_dialog).setCancelable(false)
      .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

      def onClick(dialog: DialogInterface, id: Int) {
        Observable[Boolean](subscriber => {
          val db = State.db
          if (deleteLogsCheckboxView.isChecked) db.deleteChatLogs(key)
          db.deleteContact(key)
          val mFriend = ToxSingleton.getAntoxFriend(key)
          mFriend.foreach(friend => {
            try {
              ToxSingleton.tox.deleteFriend(friend.getFriendNumber)
              ToxSingleton.save()
            } catch {
              case e: ToxException[_] =>
            }
          })
          subscriber.onCompleted()
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

  def exportChat(context: Context, fkey: ToxKey) {
    val key = fkey
    val fileDialog = new FileDialog(this.getActivity, Environment.getExternalStorageDirectory, true)
    fileDialog.addDirectoryListener(new DirectorySelectedListener {
      override def directorySelected(directory: File): Unit = {
        try {
          val db = State.db
          val messageList: Seq[Message] = db.getMessageList(Some(key), actionMessages = true)
          val exportPath = directory.getPath + "/" + ToxSingleton.getAntoxFriend(key).get.name + "-" + UIUtils.trimId(key) + "-log.txt"

          val log = new PrintWriter(new FileOutputStream(exportPath, false))
          for (message: Message <- messageList) {
            val formattedMessage = message.logFormat()
            if (formattedMessage.isDefined)
              log.print(formattedMessage.get + '\n')
          }
          log.close()
          Toast.makeText(context, getResources.getString(R.string.friend_action_chat_log_exported, exportPath), Toast.LENGTH_SHORT).show()
        } catch {
          case e: Exception =>
            Toast.makeText(context, getResources.getString(R.string.friend_action_chat_log_export_failed), Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
      }
    })
    fileDialog.showDialog()
  }

  def showDeleteChatDialog(context: Context, fkey: ToxKey) {
    val key = fkey
    val builder = new AlertDialog.Builder(context)
    builder.setMessage(getResources.getString(R.string.friend_action_delete_chat_confirmation))
      .setCancelable(false)
      .setPositiveButton(getResources.getString(R.string.button_yes), new DialogInterface.OnClickListener() {

        def onClick(dialog: DialogInterface, id: Int) {
          val db = State.db
          db.deleteChatLogs(key)
        }
      })
      .setNegativeButton(getResources.getString(R.string.button_no), new DialogInterface.OnClickListener() {

        def onClick(dialog: DialogInterface, id: Int) {
        }
      })
    builder.show()
  }

  def compareNames(a: ContactInfo, b: ContactInfo): Boolean = {
    Collator.getInstance().compare(a.getAliasOrName.toLowerCase, b.getAliasOrName.toLowerCase) < 0
  }

  def compareOnline(a: FriendInfo, b: FriendInfo): Boolean = {
    a.online && !b.online
  }

  def compareFavorite(a: ContactInfo, b: ContactInfo): Boolean = {
    a.favorite && !b.favorite
  }
}
