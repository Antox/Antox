package chat.tox.antox.viewholders

import java.io.File

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.View
import android.view.View.OnClickListener
import android.widget.{ImageView, LinearLayout, TextView}
import chat.tox.antox.R
import chat.tox.antox.utils.{BitmapManager, Constants}

class FileMessageHolder(val view: View) extends GenericMessageHolder(view) with OnClickListener {

  protected val imageMessage = view.findViewById(R.id.message_sent_photo).asInstanceOf[ImageView]

  protected val fileButtons = view.findViewById(R.id.file_buttons).asInstanceOf[LinearLayout]

  protected val fileSize = view.findViewById(R.id.file_size).asInstanceOf[TextView]

  protected var file: File = _

  def setImage(): Unit = {
    file =
      if (message.message.contains("/")) {
        new File(message.message)
      } else {
        val f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
          Constants.DOWNLOAD_DIRECTORY)
        new File(f.getAbsolutePath + "/" + message.message)
      }

    if (file.exists()) {
      val okFileExtensions = Array("jpg", "png", "gif", "jpeg")
      for (extension <- okFileExtensions) {
        if (file.getName.toLowerCase.endsWith(extension)) {
          // Set a placeholder in the image in case bitmap needs to be loaded from disk
          if (message.isMine) {
            imageMessage.setImageResource(R.drawable.sent)
          } else {
            imageMessage.setImageResource(R.drawable.received)
          }

          BitmapManager.load(file, imageMessage, isAvatar = false)
          imageMessage.setVisibility(View.VISIBLE)
          imageMessage.setOnClickListener(this)
          fileButtons.setVisibility(View.GONE)
          fileSize.setVisibility(View.GONE)
        }
      }
    }
  }

  override def onClick(view: View) {
    val i = new Intent()
    val context = view.getContext
    i.setAction(android.content.Intent.ACTION_VIEW)
    i.setDataAndType(Uri.fromFile(file), "image/*")
    context.startActivity(i)
  }
}
