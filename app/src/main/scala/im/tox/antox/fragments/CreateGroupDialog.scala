package im.tox.antox.fragments

import java.util

import android.content.Context
import android.text.{Editable, TextWatcher}
import android.widget.EditText
import com.afollestad.materialdialogs.{DialogAction, MaterialDialog}
import im.tox.antox.fragments.CreateGroupDialog.CreateGroupListener
import im.tox.antoxnightly.R

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

  private val dialog = new MaterialDialog.Builder(context)
    .title(R.string.create_group_dialog_message)
    .customView(R.layout.fragment_create_group, wrapInScrollView)
    .positiveText(R.string.create_group_dialog_create_group)
    .negativeText(R.string.create_group_dialog_cancel).callback(
      new MaterialDialog.ButtonCallback() {
        override def onPositive(dialog: MaterialDialog): Unit = {
          fireCreateGroupEvent(nameInput.getText.toString)
        }

        override def onNegative(dialog: MaterialDialog): Unit = {}
      })
    .build()

  nameInput = dialog.getCustomView.findViewById(R.id.group_name).asInstanceOf[EditText]
  val positiveAction = dialog.getActionButton(DialogAction.POSITIVE)

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

  def fireCreateGroupEvent(groupName: String): Unit = {
    for (listener: CreateGroupDialog.CreateGroupListener <- createGroupListenerList) {
      listener.groupCreationConfimed(groupName)
    }
  }
}