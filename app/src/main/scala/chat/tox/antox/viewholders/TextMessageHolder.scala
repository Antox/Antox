package chat.tox.antox.viewholders

import android.app.AlertDialog
import android.content.{ClipData, ClipboardManager, Context, DialogInterface}
import android.view.View
import android.view.View.OnLongClickListener
import chat.tox.antox.R
import chat.tox.antox.data.State
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.IOScheduler

class TextMessageHolder(val view: View) extends GenericMessageHolder(view) with OnLongClickListener {

  def setText(s: String): Unit = {
    messageText.setText(s)
    messageText.setOnLongClickListener(this)

    val context = view.getContext
    if (message.isMine) {
      if (shouldGreentext(s)) {
        messageText.setTextColor(context.getResources.getColor(R.color.green_light))
      } else {
        messageText.setTextColor(context.getResources.getColor(R.color.white))
      }
    } else {
      if (shouldGreentext(s)) {
        messageText.setTextColor(context.getResources.getColor(R.color.green))
      } else {
        messageText.setTextColor(context.getResources.getColor(R.color.black))
      }
    }
  }

  private def shouldGreentext(message: String): Boolean = {
    message.startsWith(">")
  }

  override def onLongClick(view: View): Boolean = {
    val context = view.getContext
    val items = Array[CharSequence](context.getResources.getString(R.string.message_copy), context.getResources.getString(R.string.message_delete))
    new AlertDialog.Builder(context).setCancelable(true).setItems(items, new DialogInterface.OnClickListener() {

      def onClick(dialog: DialogInterface, index: Int): Unit = index match {
        case 0 =>
          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE).asInstanceOf[ClipboardManager]
          clipboard.setPrimaryClip(ClipData.newPlainText(null, message.message))

        case 1 =>
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
