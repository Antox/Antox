package chat.tox.antox.activities

import java.io.{File, IOException}
import java.text.SimpleDateFormat
import java.util.Date

import android.app.Activity
import android.content.{Context, Intent}
import android.net.Uri
import android.os.{Build, Bundle, Environment, SystemClock}
import android.provider.MediaStore
import android.support.v4.content.CursorLoader
import android.view.View
import android.view.View.OnClickListener
import android.widget._
import chat.tox.antox.R
import chat.tox.antox.av.{Call, CameraUtils}
import chat.tox.antox.data.State
import chat.tox.antox.theme.ThemeManager
import chat.tox.antox.tox.{MessageHelper, ToxSingleton}
import chat.tox.antox.utils.StringExtensions.RichString
import chat.tox.antox.utils.ViewExtensions.RichView
import chat.tox.antox.utils._
import chat.tox.antox.wrapper._
import com.github.angads25.filepicker.controller.DialogSelectionListener
import com.github.angads25.filepicker.model.{DialogConfigs, DialogProperties}
import com.github.angads25.filepicker.view.FilePickerDialog
import de.hdodenhof.circleimageview.CircleImageView
import im.tox.tox4j.core.data.ToxFileId
import im.tox.tox4j.core.enums.ToxMessageType
import im.tox.tox4j.exceptions.ToxException
import rx.lang.scala.Subscription
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}

class ChatActivity extends GenericChatActivity[FriendKey] {

  val photoPathSaveKey = "PHOTO_PATH"
  var photoPath: Option[String] = None

  var activeCallBarView: RelativeLayout = _
  var activeCallBarClickable: RelativeLayout = _
  var activeCallBarClickSubscription: Option[Subscription] = None
  var activeCallSubscription: Option[Subscription] = None

  override def getKey(key: String): FriendKey = new FriendKey(key)

  override def onCreate(savedInstanceState: Bundle): Unit = {

    super.onCreate(savedInstanceState)
    val thisActivity = this

    getSupportActionBar.setDisplayHomeAsUpEnabled(true)
    ThemeManager.applyTheme(this, getSupportActionBar)

    //findViewById(R.id.info).setVisibility(View.GONE)

    /* Set up on click actions for attachment buttons. Could possible just add onClick to the XML?? */
    val attachmentButton = findViewById(R.id.attachment_button)
    val cameraButton = findViewById(R.id.camera_button)
    val imageButton = findViewById(R.id.image_button)

    activeCallBarView = findViewById(R.id.call_bar_wrap).asInstanceOf[RelativeLayout]
    activeCallBarView.setVisibility(View.GONE)

    activeCallBarClickable = findViewById(R.id.call_bar_clickable).asInstanceOf[RelativeLayout]

    attachmentButton.setOnClickListener(new View.OnClickListener() {
      override def onClick(v: View) {
        val friendInfo = State.db.getFriendInfo(activeKey)
        if (!friendInfo.online) {
          Toast.makeText(thisActivity, getResources.getString(R.string.chat_ft_failed_friend_offline), Toast.LENGTH_SHORT).show()
          return
        }

        val path = Environment.getExternalStorageDirectory
        val properties: DialogProperties = new DialogProperties()
        properties.selection_mode = DialogConfigs.SINGLE_MODE
        properties.selection_type = DialogConfigs.FILE_SELECT
        properties.root = path
        properties.error_dir = path
        properties.extensions = null
        val dialog: FilePickerDialog = new FilePickerDialog(thisActivity, properties)
        dialog.setTitle(R.string.select_file)

        dialog.setDialogSelectionListener(new DialogSelectionListener() {
          override def onSelectedFilePaths(files: Array[String]) = {
            // files is the array of the paths of files selected by the Application User.
            // since we only want single file selection, use the first entry
            if (files != null) {
              if (files.length > 0) {
                if (files(0) != null) {
                  if (files(0).length > 0) {
                    val filePath: String = new File(files(0)).getAbsolutePath()
                    State.transfers.sendFileSendRequest(filePath, activeKey, FileKind.DATA, ToxFileId.empty, thisActivity)
                  }
                }
              }
            }
          }
        })

        dialog.show()
      }
    })

    cameraButton.setOnClickListener(new View.OnClickListener() {
      override def onClick(v: View) {
        val friendInfo = State.db.getFriendInfo(activeKey)
        if (!friendInfo.online) {
          Toast.makeText(thisActivity, getResources.getString(R.string.chat_ft_failed_friend_offline), Toast.LENGTH_SHORT).show()
          return
        }

        val cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        val image_name = "Antoxpic " + new SimpleDateFormat("hhmm").format(new Date()) + " "

        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        try {
          val file = File.createTempFile(image_name, ".jpg", storageDir)
          val imageUri = Uri.fromFile(file)
          cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
          photoPath = Some(file.getAbsolutePath)
          startActivityForResult(cameraIntent, Constants.PHOTO_RESULT)
        } catch {
          case e: IOException => e.printStackTrace()
        }

      }
    })

    imageButton.setOnClickListener(new View.OnClickListener() {
      override def onClick(v: View) {
        val friendInfo = State.db.getFriendInfo(activeKey)
        if (!friendInfo.online) {
          Toast.makeText(thisActivity, getResources.getString(R.string.chat_ft_failed_friend_offline), Toast.LENGTH_SHORT).show()
          return
        }

        val intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, Constants.IMAGE_RESULT)
      }
    })
  }

  override def onSaveInstanceState(savedInstanceState: Bundle): Unit = {
    super.onSaveInstanceState(savedInstanceState)

    //save the photo path to prevent it being lost on rotation
    savedInstanceState.putString(photoPathSaveKey, photoPath.getOrElse(""))
  }

  override def onRestoreInstanceState(savedInstanceState: Bundle): Unit = {
    super.onRestoreInstanceState(savedInstanceState)

    val rawPhotoPath = savedInstanceState.getString(photoPathSaveKey)
    photoPath = rawPhotoPath.toOption
  }

  override def onResume(): Unit = {
    super.onResume()
    //clear notifications that have already been seen
    AntoxNotificationManager.clearMessageNotification(activeKey)

    titleSub = State.db.friendInfoList
      .subscribeOn(IOScheduler())
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(fi => {
        updateDisplayedState(fi)
      })

    activeCallSubscription =
      Some(State.callManager.activeCallObservable.observeOn(AndroidMainThreadScheduler()).subscribe(activeCalls => {
        val activeAssociatedCall = activeCalls.find(_.contactKey == activeKey)

        // if there's an active call associated with this contact, show the call bar otherwise hide it
        activeAssociatedCall match {

          // check that the call is not ringing to prevent the bar appearing too early
          case Some(call) if !call.ringing =>
            activeCallBarView.setVisibility(View.VISIBLE)
            activeCallBarClickable.setOnClickListener(new OnClickListener {
              override def onClick(v: View): Unit = {
                startCallActivity(call, v.getCenterLocationOnScreen())
              }
            })
            val chronometer = findViewById(R.id.call_bar_chronometer).asInstanceOf[Chronometer]
            chronometer.setBase(SystemClock.elapsedRealtime() - call.duration.toMillis)
            chronometer.start()

          case _ =>
            activeCallBarClickable.setOnClickListener(null)
            activeCallBarView.setVisibility(View.GONE)
        }
      }))
  }

  private def updateDisplayedState(friendInfoList: Seq[FriendInfo]): Unit = {
    val thisActivity = this
    val key = activeKey
    val mFriend: Option[FriendInfo] = friendInfoList.find(f => f.key == key)

    mFriend match {
      case Some(friend) =>
        thisActivity.setDisplayName(friend.getDisplayName)

        val avatar = friend.avatar
        avatar.foreach(avatar => {
          val avatarView = this.findViewById(R.id.chat_avatar).asInstanceOf[CircleImageView]
          BitmapManager.load(avatar, isAvatar = true).foreach(avatarView.setImageBitmap)
        })


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
          thisActivity.statusIconView.setBackground(thisActivity.getResources
            .getDrawable(IconColor.iconDrawable(friend.online, UserStatus.getToxUserStatusFromString(friend.status))))
        } else {
          thisActivity.statusIconView.setBackgroundDrawable(thisActivity.getResources
            .getDrawable(IconColor.iconDrawable(friend.online, UserStatus.getToxUserStatusFromString(friend.status))))
        }

      case None =>
        thisActivity.setDisplayName("")
    }
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == Activity.RESULT_OK) {
      if (requestCode == Constants.IMAGE_RESULT) {
        val uri = data.getData
        val filePathColumn = Array(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME)
        val loader = new CursorLoader(this, uri, filePathColumn, null, null, null)
        val cursor = loader.loadInBackground()
        if (cursor != null) {
          if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndexOrThrow(filePathColumn(0))
            val filePath = cursor.getString(columnIndex)
            val fileNameIndex = cursor.getColumnIndexOrThrow(filePathColumn(1))
            val fileName = cursor.getString(fileNameIndex)
            try {
              State.transfers.sendFileSendRequest(filePath, this.activeKey, FileKind.DATA, ToxFileId.empty, this)
            } catch {
              case e: Exception => e.printStackTrace()
            }
          }
        }
      }
      if (requestCode == Constants.PHOTO_RESULT) {
        photoPath.foreach(path => State.transfers.sendFileSendRequest(path, this.activeKey, FileKind.DATA, ToxFileId.empty, this))
        photoPath = None
      }
    } else {
      AntoxLog.debug("onActivityResult result code not okay, user cancelled")
    }
  }

  def startCallActivity(call: Call, clickLocation: Location): Unit = {
    val callActivity = new Intent(this, classOf[CallActivity])
    callActivity.putExtra("key", call.contactKey.toString)
    callActivity.putExtra("call_number", call.callNumber.value)
    callActivity.putExtra("click_location", clickLocation)
    startActivity(callActivity)
  }

  override def onClickInfo(clickLocation: Location): Unit = {
    val intent = new Intent(this, classOf[FriendProfileActivity])
    val friendInfo = State.db.getFriendInfo(activeKey)
    intent.putExtra("key", activeKey.key)
    intent.putExtra("avatar", friendInfo.avatar)

    intent.putExtra("name", friendInfo.alias.getOrElse(friendInfo.name).toString())
    startActivity(intent)
  }

  def onClickCall(video: Boolean, clickLocation: Location): Unit = {
    AntoxLog.debug("Calling friend")
    if (!State.db.getFriendInfo(activeKey).online) {
      AntoxLog.debug("Friend not online")
      return
    }

    val activeCalls = State.callManager.calls.filter(_.active)

    //end all calls that are active that do not belong to this activity
    activeCalls.filterNot(_.contactKey == activeKey).foreach(_.end())

    val associatedActiveCall = activeCalls.find(_.active)
    associatedActiveCall match {
      case Some(call) =>
        //an active call already exists, resume its activity
        startCallActivity(call, clickLocation)

      case None =>
        //an active call does not exist, start a new call and start a corresponding activity
        val call = Call(CallNumber.fromFriendNumber(ToxSingleton.tox.getFriendNumber(activeKey)), activeKey, incoming = false)
        State.callManager.add(call)
        call.startCall(sendingAudio = true, sendingVideo = video)

        startCallActivity(call, clickLocation)
    }
  }

  override def onClickVoiceCall(clickLocation: Location): Unit = {
    onClickCall(video = false, clickLocation)
  }

  override def onClickVideoCall(clickLocation: Location): Unit = {
    // don't send video if the device doesn't have a camera
    val sendingVideo = CameraUtils.deviceHasCamera(this)

    onClickCall(video = sendingVideo, clickLocation)
  }

  override def onPause(): Unit = {
    super.onPause()

    activeCallSubscription.foreach(_.unsubscribe())
  }

  override def sendMessage(message: String, messageType: ToxMessageType, context: Context): Unit = {
    MessageHelper.sendMessage(this, activeKey, message, messageType, None)
  }

  override def setTyping(typing: Boolean): Unit = {
    try {
      ToxSingleton.tox.setTyping(activeKey, typing)
    } catch {
      case te: ToxException[_] =>
      case e: Exception =>
    }
  }
}
