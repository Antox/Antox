package im.tox.antox.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import im.tox.antox.R;

/**
 * Created by soft on 03/04/14.
 */
public class PinDialogFragment extends DialogFragment {
    public interface PinDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog, String pin);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    PinDialogListener mListener;
    EditText pin;

    public PinDialogFragment() {}

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (PinDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement DHTDialogFragment");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = (View) inflater.inflate(R.layout.dialog_pin, null);
        builder.setView(view);

        pin = (EditText) view.findViewById(R.id.pin);

        builder.setMessage(getResources().getString(R.string.dialog_pin))
                .setPositiveButton(getResources().getString(R.string.button_confirm), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogPositiveClick(PinDialogFragment.this,
                                pin.getText().toString());
                    }
                })
                .setNegativeButton(getResources().getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onDialogNegativeClick(PinDialogFragment.this);
                    }
                });
        return builder.create();
    }

    public void onCancel(DialogInterface dialog) {
        mListener.onDialogNegativeClick(PinDialogFragment.this);
    }
}
