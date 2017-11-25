package chat.tox.antox.activities

import android.app.Dialog
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.widget.EditText
import chat.tox.antox.R
import chat.tox.antox.data.State
import chat.tox.antox.wrapper.CallReply

object EditCallReplyDialog {

  val EXTRA_CALL_REPLY_ID = "call_reply_id"
  val EXTRA_CALL_REPLY_REPLY = "call_reply_reply"

  def newInstance(callReply: CallReply): EditCallReplyDialog = {
    val editCallReplyDialog = new EditCallReplyDialog()

    val bundle = new Bundle()
    bundle.putInt(EXTRA_CALL_REPLY_ID, callReply.id)
    bundle.putString(EXTRA_CALL_REPLY_REPLY, callReply.reply)
    editCallReplyDialog.setArguments(bundle)

    editCallReplyDialog
  }
}

class EditCallReplyDialog extends DialogFragment {

  import EditCallReplyDialog._

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {
    super.onCreateDialog(savedInstanceState)

    val callReply = CallReply(getArguments.getInt(EXTRA_CALL_REPLY_ID), getArguments.getString(EXTRA_CALL_REPLY_REPLY))

    val customView = getActivity.getLayoutInflater.inflate(R.layout.dialog_edit_call_replies, null)
    val editText = customView.findViewById(R.id.call_reply_text).asInstanceOf[EditText]
    editText.setText(callReply.reply)

    val builder = new AlertDialog.Builder(getActivity)
      .setTitle(R.string.edit_call_reply_title)
      .setView(customView)

    builder.setPositiveButton(R.string.button_ok, new OnClickListener {
      override def onClick(dialog: DialogInterface, whichButton: Int): Unit = {
        val newReplyString = editText.getText.toString

        //prevent the preset translatable replies from being overwritten
        if (newReplyString != callReply.reply) {
          val newReply = callReply.copy(reply = newReplyString)
          State.userDb(getActivity).updateActiveUserCallReply(newReply)
        }
      }
    })

    builder.setNegativeButton(R.string.button_cancel, new OnClickListener {
      override def onClick(dialog: DialogInterface, which: Int): Unit = {
        dialog.cancel()
      }
    })

    builder.show()
  }
}
