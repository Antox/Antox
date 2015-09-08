package chat.tox.antox.viewholders

import android.view.View
import android.widget.TextView
import chat.tox.antox.R

class ActionMessageHolder(val view: View) extends GenericMessageHolder(view) {

  protected val messageText = view.findViewById(R.id.message_text).asInstanceOf[TextView]

  def setText(name: String, msg: String): Unit = {
    messageText.setText(name + " " + msg)
  }

}
