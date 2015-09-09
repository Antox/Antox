package chat.tox.antox.viewholders

import java.io.File

import android.app.AlertDialog
import android.content._
import android.net.Uri
import android.view.View
import android.view.View.{OnClickListener, OnLongClickListener}
import android.widget.{ImageView, LinearLayout, TextView}
import chat.tox.antox.R
import chat.tox.antox.data.State
import chat.tox.antox.utils.BitmapManager
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.IOScheduler

class FileMessageHolder(val view: View) extends GenericMessageHolder(view) with OnClickListener with OnLongClickListener {

  protected val imageMessage = view.findViewById(R.id.message_sent_photo).asInstanceOf[ImageView]

  protected val fileButtons = view.findViewById(R.id.file_buttons).asInstanceOf[LinearLayout]

  protected val progressLayout = view.findViewById(R.id.progress_layout).asInstanceOf[LinearLayout]

  protected val fileSize = view.findViewById(R.id.file_size).asInstanceOf[TextView]

  protected var file: File = _

  def setImage(file: File): Unit = {
    if (file.exists()) {
      this.file = file
      if (file.getName.toLowerCase.matches("^.+?\\.(jpg|jpeg|png|gif)$")) {
        // Set a placeholder in the image in case bitmap needs to be loaded from disk
        if (message.isMine) {
          imageMessage.setImageResource(R.drawable.sent)
        } else {
          imageMessage.setImageResource(R.drawable.received)
        }

        BitmapManager.load(file, imageMessage, isAvatar = false)
        imageMessage.setVisibility(View.VISIBLE)
        imageMessage.setOnClickListener(this)
        imageMessage.setOnLongClickListener(this)

        //TODO would be better to find a way where we didn't have to toggle all these
        messageText.setVisibility(View.GONE)
        fileSize.setVisibility(View.GONE)
        progressLayout.setVisibility(View.GONE)
        fileButtons.setVisibility(View.GONE)
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

  override def onLongClick(view: View): Boolean = {
    val context = view.getContext
    val items = Array[CharSequence](context.getResources.getString(R.string.message_delete))
    new AlertDialog.Builder(context).setCancelable(true).setItems(items, new DialogInterface.OnClickListener() {

      def onClick(dialog: DialogInterface, index: Int): Unit = index match {
        case 0 =>
          Observable[Boolean](subscriber => {
            val db = State.db
            db.deleteMessage(message.id)
            subscriber.onCompleted()
          }).subscribeOn(IOScheduler()).subscribe()

      }
    }).create().show()

    true
  }
}
