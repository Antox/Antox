package chat.tox.antox.viewholders

import android.view.View
import android.widget.TextView
import chat.tox.antox.R

class TextMessageHolder(var view: View) extends GenericMessageHolder(view) {

  protected val title = view.findViewById(R.id.message_title).asInstanceOf[TextView]

  protected val messageText = view.findViewById(R.id.message_text).asInstanceOf[TextView]

  def setTitle(s: String): Unit = {
    title.setText(s)
  }

  def setText(s: String): Unit = {
    messageText.setText(s)

    val context = view.getContext
    if (shouldGreentext(s)) {
      if (message.isMine) {
        messageText.setTextColor(context.getResources.getColor(R.color.green_light))
      } else {
        messageText.setTextColor(context.getResources.getColor(R.color.green))
      }
    }
  }

  private def shouldGreentext(message: String): Boolean = {
    message.startsWith(">")
  }

}
