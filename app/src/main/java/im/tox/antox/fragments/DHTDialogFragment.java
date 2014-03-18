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
 * Created by Garrett on 2/27/14.
 *
 * Settings Dialog for user-set DHT node settings
 * Called by SettingsActivity
 */
public class DHTDialogFragment extends DialogFragment{
    /* The activity that creates an instance of this dialog fragment must
    * implement this interface in order to receive event callbacks.
    * Each method passes the DialogFragment in case the host needs to query it. */
    public interface DHTDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog,
                                          String dhtIP, String dhtPort, String dhtKey);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    DHTDialogListener mListener;

    //Initial DHT settings
    String ip, port, key;

    EditText dhtIP, dhtPort, dhtKey;

    public DHTDialogFragment() {

    }
    // Override the Fragment.onAttach() method to instantiate the DHTDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the DHTDialogListener so we can send events to the host
            mListener = (DHTDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement DHTDialogFragment");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Build the dialog and set up the button click handlers
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = (View) inflater.inflate(R.layout.dialog_settings_dht, null);
        builder.setView(view);

        // Initialize the EditText fields
        dhtIP = (EditText) view.findViewById(R.id.settings_dht_ip);
        dhtPort = (EditText) view.findViewById(R.id.settings_dht_port);
        dhtKey = (EditText) view.findViewById(R.id.settings_dht_key);

        /* If the preferences aren't blank, then add them to text fields
         * otherwise it will display the predefined hints in strings.xml
         */
        if (ip != null && !ip.equals(""))
            dhtIP.setText(ip);
        if (port != null && !port.equals(""))
            dhtPort.setText(port);
        if (key != null && !key.equals(""))
            dhtKey.setText(key);

        // Configure the dialog and set up button Listeners
        builder.setMessage(getResources().getString(R.string.settings_custom_dht))
                .setPositiveButton(getResources().getString(R.string.button_confirm), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Send the positive button event and text entry back to the host activity
                        mListener.onDialogPositiveClick(DHTDialogFragment.this,
                                dhtIP.getText().toString(),
                                dhtPort.getText().toString(),
                                dhtKey.getText().toString());
                    }
                })
                .setNegativeButton(getResources().getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Send the negative button event back to the host activity
                        mListener.onDialogNegativeClick(DHTDialogFragment.this);
                    }
                });
        return builder.create();
    }

    // Called if the dialog is canceled for any other reason
    public void onCancel(DialogInterface dialog) {
        // Send the negative feedback to the host activity
        mListener.onDialogNegativeClick(DHTDialogFragment.this);
    }

}
