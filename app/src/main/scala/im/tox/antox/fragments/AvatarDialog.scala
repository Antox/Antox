package im.tox.antox.fragments

import java.io.{IOException, File}
import java.text.SimpleDateFormat
import java.util.Date

import android.app.{Dialog, Activity}
import android.content.{Context, Intent}
import android.graphics.{BitmapFactory, Bitmap}
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.{Bundle, Environment}
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.v4.app.DialogFragment
import android.support.v4.content.{CursorLoader, FileProvider}
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup.LayoutParams
import android.widget.{Toast, Button, ImageView}
import im.tox.antox.data.{State, AntoxDB}
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.transfer.FileUtils
import im.tox.antox.utils.Constants
import im.tox.antox.wrapper.BitmapUtils.RichBitmap
import im.tox.antox.wrapper.FileKind
import im.tox.antox.wrapper.FileKind.AVATAR
import im.tox.antoxnightly.R

//not a DialogFragment (i. because they don't work with PreferenceActivity
class AvatarDialog(activity: Activity) {

  val preferences = PreferenceManager.getDefaultSharedPreferences(activity)

  def onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    if (resultCode == Activity.RESULT_OK) {
      val name = preferences.getString("tox_id", "")
      val avatarFile = new File(AVATAR.getStorageDir(activity), name)

      if (requestCode == Constants.IMAGE_RESULT) {
        val uri = data.getData
        val filePathColumn = Array(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME)
        val loader = new CursorLoader(activity, uri, filePathColumn, null, null, null)
        val cursor = loader.loadInBackground()
        if (cursor != null) {
          if (cursor.moveToFirst()) {
            val imageFile = new File(cursor.getString(cursor.getColumnIndexOrThrow(filePathColumn(0))))
            if (!imageFile.exists()) return

            FileUtils.copy(imageFile, avatarFile)
          }
        }
      }

      resizeAvatar(avatarFile) match {
        case Some(bitmap) =>
          FileUtils.writeBitmap(bitmap, Bitmap.CompressFormat.PNG, 0, avatarFile)
          preferences.edit().putString("avatar", name).commit()
        case None =>
          avatarFile.delete()
          Toast.makeText(activity, activity.getResources.getString(R.string.avatar_too_large_error), Toast.LENGTH_SHORT)
      }
      val db = new AntoxDB(activity)
      db.setAllFriendReceivedAvatar(false)
      db.close()
      State.transfers.updateSelfAvatar(activity)

    }
  }

  def resizeAvatar(avatar: File): Option[Bitmap] = {
    val rawBitmap = BitmapFactory.decodeFile(avatar.getPath)
    val cropDimension =
      if (rawBitmap.getWidth >= rawBitmap.getHeight) {
        rawBitmap.getHeight
      } else {
        rawBitmap.getWidth
      }

    var bitmap = ThumbnailUtils.extractThumbnail(rawBitmap, cropDimension, cropDimension)
    val MAX_DIMENSIONS = 256
    val MIN_DIMENSIONS = 16

    var currSize = MAX_DIMENSIONS
    while (currSize >= MIN_DIMENSIONS && bitmap.getSizeInBytes > Constants.MAX_AVATAR_SIZE) {
      bitmap = Bitmap.createScaledBitmap(bitmap, currSize, currSize, false)
      currSize /= 2
    }

    if (bitmap.getSizeInBytes > Constants.MAX_AVATAR_SIZE) {
      None
    } else {
      Some(bitmap)
    }
  }

  def refreshAvatar(avatarView: ImageView): Unit = {
    val avatar = AVATAR.getAvatarFile(preferences.getString("avatar", ""), activity)
    if (avatar.isDefined && avatar.get.exists()) {
      avatarView.setImageURI(Uri.fromFile(avatar.get))
    } else {
      avatarView.setImageResource(R.drawable.ic_action_contact)
    }
  }

  var mDialog: Option[Dialog] = None

  def show(): Unit = {
    val inflator = activity.getLayoutInflater
    val view = inflator.inflate(R.layout.dialog_avatar, null)
    mDialog = Some(new AlertDialog.Builder(activity, R.style.AppCompatAlertDialogStyle)
      .setView(view).create())

    val photoButton = view.findViewById(R.id.avatar_takephoto).asInstanceOf[Button]
    val fileButton = view.findViewById(R.id.avatar_pickfile).asInstanceOf[Button]

    photoButton.setOnClickListener(new View.OnClickListener {
      override def onClick(v: View): Unit = {
        val cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(activity.getPackageManager) != null) {
          val fileName = preferences.getString("tox_id", "")
          try {
            val file = new File(AVATAR.getStorageDir(activity), fileName)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file))
            activity.startActivityForResult(cameraIntent, Constants.PHOTO_RESULT)
          } catch {
            case e: IOException => e.printStackTrace()
          }
        } else {
          Toast.makeText(activity, activity.getResources.getString(R.string.no_camera_intent_error), Toast.LENGTH_SHORT)
        }
      }
    })

    fileButton.setOnClickListener(new OnClickListener {
      override def onClick(v: View): Unit = {
        val intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        activity.startActivityForResult(intent, Constants.IMAGE_RESULT)
      }
    })

    refreshAvatar(view.findViewById(R.id.avatar_image).asInstanceOf[ImageView])
    if (mDialog.get.isShowing) close()
    mDialog.get.show()
  }

  def close(): Unit = {
    mDialog.foreach(_.cancel())
  }
}
