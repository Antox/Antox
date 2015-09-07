package chat.tox.antox.viewholders

import android.view.View
import android.widget.TextView
import chat.tox.antox.R

class TextMessageHolder(var view: View) extends GenericMessageHolder(view) {

  protected val messageText = view.findViewById(R.id.message_text).asInstanceOf[TextView]

  def setText(s: String): Unit = {
    messageText.setText(s)

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

}
