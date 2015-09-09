package chat.tox.antox.viewholders

import android.view.View

class ActionMessageHolder(val view: View) extends GenericMessageHolder(view) {

  def setText(name: String, msg: String): Unit = {
    messageText.setText(name + " " + msg)
  }

}
