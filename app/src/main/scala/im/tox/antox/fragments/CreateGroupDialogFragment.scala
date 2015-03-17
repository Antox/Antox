package im.tox.antox.fragments

import android.app.{AlertDialog, Dialog}
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import im.tox.antox.R
import im.tox.antox.tox.ToxSingleton

class CreateGroupDialogFragment extends DialogFragment {

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {
    // Use the Builder class for convenient dialog construction
    val builder = new AlertDialog.Builder(getActivity)

    builder.setView(R.layout.fragment_create_group)

    builder.setMessage(R.string.create_group_dialog_message)
      .setPositiveButton(R.string.create_group_dialog_create_group, new DialogInterface.OnClickListener() {
      def onClick(dialog: DialogInterface, id: Int): Unit = {
        //ToxSingleton.tox.newGroup()
      }
    })
      .setNegativeButton(R.string.create_group_dialog_cancel, new DialogInterface.OnClickListener() {
      def onClick(dialog: DialogInterface, id: Int): Unit = {

      }
    })

    builder.create()
  }
}