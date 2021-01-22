package chat.tox.antox.fragments

import java.io.{File, FileOutputStream, PrintWriter}
import java.text.Collator

import android.app.AlertDialog
import android.content._
import android.content.res.ColorStateList
import android.os.{Bundle, Environment}
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.text.{Editable, TextWatcher}
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.AdapterView.{OnItemClickListener, OnItemLongClickListener}
import android.widget._
import chat.tox.antox.R
import chat.tox.antox.activities.{ChatActivity, FriendProfileActivity, GroupChatActivity}
import chat.tox.antox.adapters.ContactListAdapter
import chat.tox.antox.av.Call
import chat.tox.antox.data.{CallEventKind, State}
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.utils._
import chat.tox.antox.wrapper._
import com.github.angads25.filepicker.controller.DialogSelectionListener
import com.github.angads25.filepicker.model.{DialogConfigs, DialogProperties}
import com.github.angads25.filepicker.view.FilePickerDialog
import im.tox.tox4j.exceptions.ToxException
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}
import rx.lang.scala.{Observable, Subscription}

import scala.util.Try

abstract class AbstractContactsFragment extends Fragment with OnItemClickListener with OnItemLongClickListener {

  var showSearch: Boolean = _

  var showFab: Boolean = true

  protected var contactsListView: ListView = _

  protected var leftPaneAdapter: ContactListAdapter = _

  protected var contactChangeSub: Option[Subscription] = None

  protected var activeKey: ContactKey = _

  def this(showSearch: Boolean, showFab: Boolean) {
    this()
    this.showSearch = showSearch
    this.showFab = showFab
  }

  def updateContacts(contactInfoTuple: (Seq[FriendInfo], Seq[FriendRequest],
    Seq[GroupInvite], Seq[GroupInfo]), activeCalls: Iterable[Call]): Unit

  override def onResume() {
    super.onResume()
    val db = State.db
    contactChangeSub = Try(db.contactListElements
      .combineLatest(State.callManager.activeCallObservable)
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(tuple => updateContacts(tuple._1, tuple._2))).toOption
  }

  override def onPause() {
    super.onPause()
    contactChangeSub.foreach(_.unsubscribe())
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val rootView = inflater.inflate(R.layout.fragment_contacts, container, false)
    contactsListView = rootView.findViewById(R.id.contacts_list).asInstanceOf[ListView]
    contactsListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE)
    contactsListView.setOnItemClickListener(this)
    contactsListView.setOnItemLongClickListener(this)

    if (showFab) {
      val fab = rootView.findViewById(R.id.fab).asInstanceOf[FloatingActionButton]
      val parser = getResources.getXml(R.color.fab_colors_list)
      fab.setBackgroundTintList(ColorStateList.createFromXml(getResources, parser))
      rootView.findViewById(R.id.fab).setVisibility(View.VISIBLE)
    } else {
      rootView.findViewById(R.id.fab).setVisibility(View.GONE)
    }

    if (showSearch) {
      val search = rootView.findViewById(R.id.searchBar).asInstanceOf[EditText]
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

  def getSecondImage(contact: ContactInfo): Option[Int] = {
    contact.lastMessage.map(_.callEventKind).filterNot(_ == CallEventKind.Invalid).map(_.imageRes)
  }

  override def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
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

  override def onItemLongClick(parent: AdapterView[_], itemView: View, index: Int, id: Long): Boolean = {
    val item = parent.getAdapter.asInstanceOf[Adapter].getItem(index).asInstanceOf[LeftPaneItem]
    createLeftPanePopup(item)
    true
  }

  def createLeftPanePopup(parentItem: LeftPaneItem): Unit = {
    val items = parentItem.viewType match {
      case ContactItemType.FRIEND =>
        Array[CharSequence](getResources.getString(R.string.friend_action_profile),
          getResources.getString(R.string.friend_action_delete),
          getResources.getString(R.string.friend_action_export_chat),
          getResources.getString(R.string.friend_action_delete_chat))

      case ContactItemType.GROUP =>
        Array[CharSequence](getResources.getString(R.string.group_action_delete))

      case ContactItemType.FRIEND_REQUEST | ContactItemType.GROUP_INVITE =>
        Array[CharSequence](getResources.getString(R.string.request_action_copy_id))

      case _ => Array[CharSequence]()
    }

    if (parentItem != null && items.nonEmpty) {
      new AlertDialog.Builder(getActivity, R.style.AppCompatAlertDialogStyle)
        .setTitle(getResources.getString(R.string.contacts_actions_on) + " " + parentItem.first)
        .setCancelable(true)
        .setItems(items,
          new DialogInterface.OnClickListener() {
            def onClick(dialog: DialogInterface, index: Int) {
              parentItem.viewType match {
                case ContactItemType.FRIEND =>
                  val key = parentItem.key.asInstanceOf[FriendKey]
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

                case ContactItemType.GROUP =>
                  val key = parentItem.key.asInstanceOf[GroupKey]
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

                case ContactItemType.FRIEND_REQUEST | ContactItemType.GROUP_INVITE =>
                  val key = parentItem.key
                  val clipboard = getActivity.getSystemService(Context.CLIPBOARD_SERVICE).asInstanceOf[ClipboardManager]
                  clipboard.setPrimaryClip(ClipData.newPlainText(null, key.toString))

                  val text = getString(R.string.request_id_copied)
                  val duration = Toast.LENGTH_SHORT
                  val toast = Toast.makeText(getActivity, text, duration)
                  toast.show()

                case _ => //do nothing
              }
              dialog.cancel()
            }
          })
        .show()
    }
  }

  def showDeleteFriendDialog(context: Context, friendKey: FriendKey) {
    val deleteFriendDialog = View.inflate(context, R.layout.dialog_delete_friend, null)
    val deleteLogsCheckboxView = deleteFriendDialog.findViewById(R.id.deleteChatLogsCheckBox).asInstanceOf[CheckBox]
    new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle)
      .setView(deleteFriendDialog).setCancelable(false)
      .setPositiveButton(R.string.button_yes,
        new DialogInterface.OnClickListener() {
          def onClick(dialog: DialogInterface, id: Int) {
            Observable[Boolean](subscriber => {
              val db = State.db
              if (deleteLogsCheckboxView.isChecked) {
                db.deleteChatLogs(friendKey)
              }
              db.deleteContact(friendKey)
              try {
                ToxSingleton.tox.deleteFriend(friendKey)
                ToxSingleton.save()
              } catch {
                case e: ToxException[_] =>
              }
              AntoxNotificationManager.mNotificationManager.foreach(manager => {
                manager.cancel(AntoxNotificationManager.generateNotificationId(friendKey))
              })
              subscriber.onCompleted()
            }).subscribeOn(IOScheduler()).subscribe()
          }
        })
      .setNegativeButton(R.string.button_no,
        new DialogInterface.OnClickListener() {
          def onClick(dialog: DialogInterface, id: Int) {
            dialog.cancel()
          }
        })
      .show()
  }

  def exportChat(context: Context, friendKey: FriendKey) {
    val path = Environment.getExternalStorageDirectory
    val properties: DialogProperties = new DialogProperties()
    properties.selection_mode = DialogConfigs.SINGLE_MODE
    properties.selection_type = DialogConfigs.DIR_SELECT
    properties.root = path
    properties.error_dir = path
    properties.extensions = null
    val dialog: FilePickerDialog = new FilePickerDialog(this.getActivity, properties)
    dialog.setTitle(R.string.select_file)

    dialog.setDialogSelectionListener(new DialogSelectionListener() {
      override def onSelectedFilePaths(files: Array[String]) = {
        // files is the array of the paths of files selected by the Application User.
        // since we only want single file selection, use the first entry
        if (files != null) {
          if (files.length > 0) {
            if (files(0) != null) {
              if (files(0).length > 0) {
                val directory: File = new File(files(0))
                try {
                  val db = State.db
                  val messageList: Seq[Message] = db.getMessageList(Some(friendKey))
                  val exportPath = directory.getPath + "/" + db.getFriendInfo(friendKey).name + "-" + UiUtils.trimId(friendKey) + "-log.txt"
                  val log = new PrintWriter(new FileOutputStream(exportPath, false))

                  messageList.foreach(message => {
                    val formattedMessage = message.logFormat()
                    if (formattedMessage.isDefined) {
                      log.print(formattedMessage.get + '\n')
                    }
                  })

                  log.close()
                  Toast.makeText(context, getResources.getString(R.string.friend_action_chat_log_exported, exportPath), Toast.LENGTH_SHORT).show()
                } catch {
                  case e: Exception =>
                    Toast.makeText(context, getResources.getString(R.string.friend_action_chat_log_export_failed), Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
              }
            }
          }
          else {
            try {
              val db = State.db
              val messageList: Seq[Message] = db.getMessageList(Some(friendKey))
              val exportPath = path.getPath + "/" + db.getFriendInfo(friendKey).name + "-" + UiUtils.trimId(friendKey) + "-log.txt"
              val log = new PrintWriter(new FileOutputStream(exportPath, false))

              messageList.foreach(message => {
                val formattedMessage = message.logFormat()
                if (formattedMessage.isDefined) {
                  log.print(formattedMessage.get + '\n')
                }
              })

              log.close()
              Toast.makeText(context, getResources.getString(R.string.friend_action_chat_log_exported, exportPath), Toast.LENGTH_SHORT).show()
            } catch {
              case e: Exception =>
                Toast.makeText(context, getResources.getString(R.string.friend_action_chat_log_export_failed), Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
          }
        }
      }
    })

    dialog.show()
  }

  def showDeleteChatDialog(context: Context, key: ContactKey) {
    new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle)
      .setMessage(getResources.getString(R.string.friend_action_delete_chat_confirmation))
      .setPositiveButton(getResources.getString(R.string.button_yes),
        new DialogInterface.OnClickListener() {
          def onClick(dialog: DialogInterface, id: Int) {
            val db = State.db
            db.deleteChatLogs(key)
            AntoxNotificationManager.mNotificationManager.foreach(manager => {
              manager.cancel(AntoxNotificationManager.generateNotificationId(key))
            })
          }
        })
      .setNegativeButton(getResources.getString(R.string.button_no),
        new DialogInterface.OnClickListener() {
          def onClick(dialog: DialogInterface, id: Int) {
            dialog.cancel()
          }
        })
      .show()
  }

  def compareNames(a: ContactInfo, b: ContactInfo): Boolean = {
    Collator.getInstance().compare(a.getDisplayName.toLowerCase, b.getDisplayName.toLowerCase) < 0
  }

  def compareOnline(a: FriendInfo, b: FriendInfo): Boolean = {
    a.online && !b.online
  }

  def compareFavorite(a: ContactInfo, b: ContactInfo): Boolean = {
    a.favorite && !b.favorite
  }
}
