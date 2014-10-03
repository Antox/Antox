package im.tox.antox.fragments

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import im.tox.antox.R
import PinDialogFragment._
//remove if not needed
import scala.collection.JavaConversions._

object PinDialogFragment {

  trait PinDialogListener {

    def onDialogPositiveClick(dialog: DialogFragment, pin: String): Unit

    def onDialogNegativeClick(dialog: DialogFragment): Unit
  }
}

class PinDialogFragment extends DialogFragment {

  var mListener: PinDialogListener = _

  var pin: EditText = _

  override def onAttach(activity: Activity) {
    super.onAttach(activity)
    mListener = activity.asInstanceOf[PinDialogListener]
  }

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {
    val builder = new AlertDialog.Builder(getActivity)
    val inflater = getActivity.getLayoutInflater
    val view = inflater.inflate(R.layout.dialog_pin, null).asInstanceOf[View]
    builder.setView(view)
    pin = view.findViewById(R.id.pin).asInstanceOf[EditText]
    builder.setMessage(getResources.getString(R.string.dialog_pin))
      .setPositiveButton(getResources.getString(R.string.button_confirm), new DialogInterface.OnClickListener() {

        def onClick(dialog: DialogInterface, id: Int) {
          mListener.onDialogPositiveClick(PinDialogFragment.this, pin.getText.toString)
        }
      })
      .setNegativeButton(getResources.getString(R.string.button_cancel), new DialogInterface.OnClickListener() {

        def onClick(dialog: DialogInterface, id: Int) {
          mListener.onDialogNegativeClick(PinDialogFragment.this)
        }
      })
    builder.create()
  }

  override def onCancel(dialog: DialogInterface) {
    mListener.onDialogNegativeClick(PinDialogFragment.this)
  }
}
