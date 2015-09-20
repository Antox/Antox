package chat.tox.antox.activities

import java.io.{File, IOException}
import java.text.SimpleDateFormat
import java.util.Date

import android.app.Activity
import android.content.{Context, Intent}
import android.net.Uri
import android.os.{Build, Bundle, Environment}
import android.provider.MediaStore
import android.support.v4.content.CursorLoader
import android.util.Log
import android.view.View
import android.widget._
import chat.tox.antox.R
import chat.tox.antox.data.State
import chat.tox.antox.theme.ThemeManager
import chat.tox.antox.tox.{MessageHelper, ToxSingleton}
import chat.tox.antox.transfer.FileDialog
import chat.tox.antox.utils.{BitmapManager, Constants, IconColor}
import chat.tox.antox.wrapper.{FileKind, FriendInfo, ToxKey, UserStatus}
import de.hdodenhof.circleimageview.CircleImageView
import im.tox.tox4j.exceptions.ToxException
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}

class ChatActivity extends GenericChatActivity {

  var photoPath: String = null

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    val extras: Bundle = getIntent.getExtras
    activeKey = new ToxKey(extras.getString("key"))
    val thisActivity = this

    getSupportActionBar.setDisplayHomeAsUpEnabled(true)
    ThemeManager.applyTheme(this, getSupportActionBar)

    this.findViewById(R.id.info).setVisibility(View.GONE)

    /* Set up on click actions for attachment buttons. Could possible just add onClick to the XML?? */
    val attachmentButton = this.findViewById(R.id.attachment_button)
    val cameraButton = this.findViewById(R.id.camera_button)
    val imageButton = this.findViewById(R.id.image_button)

    attachmentButton.setOnClickListener(new View.OnClickListener() {
      override def onClick(v: View) {
        ToxSingleton.getAntoxFriend(activeKey).foreach(friend => {
          if (!friend.isOnline) {
            Toast.makeText(thisActivity, getResources.getString(R.string.chat_ft_failed_friend_offline), Toast.LENGTH_SHORT).show()
            return
          }
        })

        val mPath = new File(Environment.getExternalStorageDirectory + "//DIR//")
        val fileDialog = new FileDialog(thisActivity, mPath, false)
        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
          def fileSelected(file: File) {
            State.transfers.sendFileSendRequest(file.getPath, activeKey, FileKind.DATA, null, thisActivity)
          }
        })
        fileDialog.showDialog()

      }
    })

    cameraButton.setOnClickListener(new View.OnClickListener() {
      override def onClick(v: View) {
        ToxSingleton.getAntoxFriend(activeKey).foreach(friend => {
          if (!friend.online) {
            Toast.makeText(thisActivity, getResources.getString(R.string.chat_ft_failed_friend_offline), Toast.LENGTH_SHORT).show()
            return
          }
        })

        val cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        val image_name = "Antoxpic " + new SimpleDateFormat("hhmm").format(new Date()) + " "
        Log.d(TAG,"image name " + image_name)
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        try {
          val file = File.createTempFile(image_name, ".jpg", storageDir)
          val imageUri = Uri.fromFile(file)
          cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
          photoPath = file.getAbsolutePath
          startActivityForResult(cameraIntent, Constants.PHOTO_RESULT)
        } catch {
          case e: IOException => e.printStackTrace()
        }

      }
    })

    imageButton.setOnClickListener(new View.OnClickListener() {
      override def onClick(v: View) {
        ToxSingleton.getAntoxFriend(activeKey).foreach(friend => {
          if (!friend.isOnline) {
            Toast.makeText(thisActivity, getResources.getString(R.string.chat_ft_failed_friend_offline), Toast.LENGTH_SHORT).show()
            return
          }
        })

        val intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, Constants.IMAGE_RESULT)
      }
    })
  }

  override def onResume(): Unit = {
    super.onResume()
    ToxSingleton.clearUselessNotifications(activeKey)
    titleSub = State.db.friendInfoList
      .subscribeOn(IOScheduler())
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(fi => {
      updateDisplayedState(fi)
    })
  }

  private def updateDisplayedState(fi: Seq[FriendInfo]): Unit = {
    val thisActivity = this
    val key = activeKey
    val mFriend: Option[FriendInfo] = fi
      .find(f => f.key == key)
    mFriend match {
      case Some(friend) =>
        thisActivity.setDisplayName(friend.getAliasOrName)

        val avatar = friend.avatar
        avatar.foreach(avatar => {
          val avatarView = this.findViewById(R.id.avatar).asInstanceOf[CircleImageView]
          BitmapManager.load(avatar, avatarView, isAvatar = true)
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
              State.transfers.sendFileSendRequest(filePath, this.activeKey, FileKind.DATA, null, this)
            } catch {
              case e: Exception => e.printStackTrace()
            }
          }
        }
      }
      if (requestCode == Constants.PHOTO_RESULT) {
        if (photoPath != null) {
          State.transfers.sendFileSendRequest(photoPath, this.activeKey, FileKind.DATA, null, this)
          photoPath = null
        }
      }
    } else {
      Log.d(TAG, "onActivityResult result code not okay, user cancelled")
    }
  }

  def onClickVoiceCallFriend(v: View) {}

  def onClickVideoCallFriend(v: View): Unit = {}

  def onClickInfo(v: View): Unit = {}

  override def onPause(): Unit = {
    super.onPause()
  }

  override def sendMessage(message: String, isAction: Boolean, context: Context): Unit = {
    MessageHelper.sendMessage(this, activeKey, message, isAction, None)
  }

  override def setTyping(typing: Boolean): Unit = {
    val mFriend = ToxSingleton.getAntoxFriend(activeKey)
    mFriend.foreach(friend => {
      try {
        ToxSingleton.tox.setTyping(friend.getFriendNumber, typing)
      } catch {
        case te: ToxException[_] =>
        case e: Exception =>
      }
    })
  }
}
