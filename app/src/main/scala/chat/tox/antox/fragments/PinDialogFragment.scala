package chat.tox.antox.fragments

import android.app.{Activity, AlertDialog, Dialog}
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.widget.EditText
import chat.tox.antox.R
import chat.tox.antox.fragments.PinDialogFragment._

object PinDialogFragment {

  trait PinDialogListener {

    def onDialogPositiveClick(dialog: DialogFragment, pin: String): Unit

    def onDialogNegativeClick(dialog: DialogFragment): Unit
  }
}

class PinDialogFragment extends DialogFragment {

  var listener: PinDialogListener = _

  var pin: EditText = _

  override def onAttach(activity: Activity) {
    super.onAttach(activity)
    listener = activity.asInstanceOf[PinDialogListener]
  }

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {
    val builder = new AlertDialog.Builder(getActivity)
    val inflater = getActivity.getLayoutInflater
    val view = inflater.inflate(R.layout.dialog_pin, null)
    builder.setView(view)
    pin = view.findViewById(R.id.pin).asInstanceOf[EditText]
    builder.setMessage(getResources.getString(R.string.dialog_pin))
      .setPositiveButton(getResources.getString(R.string.button_confirm), new DialogInterface.OnClickListener() {

        def onClick(dialog: DialogInterface, id: Int) {
          listener.onDialogPositiveClick(PinDialogFragment.this, pin.getText.toString)
        }
      })
      .setNegativeButton(getResources.getString(R.string.button_cancel), new DialogInterface.OnClickListener() {

        def onClick(dialog: DialogInterface, id: Int) {
          listener.onDialogNegativeClick(PinDialogFragment.this)
        }
      })
    builder.create()
  }

  override def onCancel(dialog: DialogInterface) {
    listener.onDialogNegativeClick(PinDialogFragment.this)
  }
}
