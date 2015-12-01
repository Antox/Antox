package chat.tox.antox.fragments

import java.util

import android.app.AlertDialog
import android.content.DialogInterface.OnClickListener
import android.content.{Context, DialogInterface}
import android.text.{Editable, TextWatcher}
import android.widget.EditText
import chat.tox.antox.R
import chat.tox.antox.fragments.CreateGroupDialog.CreateGroupListener

import scala.collection.JavaConversions._

object CreateGroupDialog {
  trait CreateGroupListener {
    def groupCreationConfimed(name: String): Unit
  }
}

class CreateGroupDialog (var context: Context) {

  private val createGroupListenerList = new util.ArrayList[CreateGroupListener]()
  val wrapInScrollView = true
  var nameInput: EditText = null

  private val dialog = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle)
    .setTitle(R.string.create_group_dialog_message)
    .setView(R.layout.fragment_create_group)
    .setPositiveButton(R.string.create_group_dialog_create_group, null)
    .setNegativeButton(R.string.create_group_dialog_cancel, new OnClickListener {
    override def onClick(dialogInterface: DialogInterface, i: Int): Unit = {
      triggerCreateGroupEvent(nameInput.getText.toString)
    }
  }).create()

  nameInput = dialog.findViewById(R.id.group_name).asInstanceOf[EditText]
  val positiveAction = dialog.getButton(DialogInterface.BUTTON_POSITIVE)

  nameInput.addTextChangedListener(new TextWatcher {
    override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = {}

    override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = {
      positiveAction.setEnabled(s.toString.trim().length > 0)
    }

    override def afterTextChanged(s: Editable): Unit = {}
  })
  positiveAction.setEnabled(false)


  def showDialog(): Unit = {
    dialog.show()
  }

  def addCreateGroupListener(listener: CreateGroupListener) {
    createGroupListenerList.add(listener)
  }

  def triggerCreateGroupEvent(groupName: String): Unit = {
    for (listener: CreateGroupDialog.CreateGroupListener <- createGroupListenerList) {
      listener.groupCreationConfimed(groupName)
    }
  }
}