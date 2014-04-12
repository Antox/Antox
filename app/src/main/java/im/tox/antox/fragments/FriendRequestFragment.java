package im.tox.antox.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

import im.tox.antox.data.AntoxDB;
import im.tox.antox.utils.Constants;
import im.tox.antox.R;
import im.tox.antox.tox.ToxService;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.activities.MainActivity;

/**
 * Created by ollie on 28/02/14.
 */
public class FriendRequestFragment extends Fragment {

    private String key;
    private String message;

    ToxSingleton toxSingleton = ToxSingleton.getInstance();

    public FriendRequestFragment() {

    }

    public FriendRequestFragment(String key, String message) {
        this.key = key;
        this.message = message;
    }

    private String SplitKey(String key) {
        return key.substring(0, 38) + "\n" + key.substring(38);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_friendrequest, container, false);
        TextView k = (TextView) rootView.findViewById(R.id.requestfragment_key);
        k.setText(SplitKey(key));
        TextView m = (TextView) rootView.findViewById(R.id.requestfragment_message);
        m.setText(message);
        //set the spinner
        final Spinner friendGroup = (Spinner) rootView.findViewById(R.id.spinner_accept_friend_request_group);
        ArrayList<String> spinnerArray = new ArrayList<String>();
        spinnerArray.add(getResources().getString(R.string.manage_groups_friends));
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("groups", Context.MODE_PRIVATE);
        if (!sharedPreferences.getAll().isEmpty()) {
            Map<String,?> keys = sharedPreferences.getAll();

            for(Map.Entry<String,?> entry : keys.entrySet()){
                String groupName = entry.getValue().toString();
                spinnerArray.add(groupName);
            }
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, spinnerArray);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        friendGroup.setAdapter(spinnerAdapter);
        Button accept = (Button) rootView.findViewById(R.id.acceptFriendRequest);
        Button reject = (Button) rootView.findViewById(R.id.rejectFriendRequest);
        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().popBackStack();
                AntoxDB db = new AntoxDB(getActivity().getApplicationContext());
                String group = friendGroup.getSelectedItem().toString();
                db.addFriend(key, "Friend Accepted", "", "", group);
                db.close();
                ((MainActivity) getActivity()).updateLeftPane();
                ((MainActivity) getActivity()).pane.openPane();
                Intent acceptRequestIntent = new Intent(getActivity(), ToxService.class);
                acceptRequestIntent.setAction(Constants.ACCEPT_FRIEND_REQUEST);
                acceptRequestIntent.putExtra("key", toxSingleton.activeFriendRequestKey);
                getActivity().startService(acceptRequestIntent);
            }
        });

        reject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AntoxDB db = new AntoxDB(getActivity().getApplicationContext());
                db.deleteFriendRequest(key);
                db.close();
                getActivity().getSupportFragmentManager().popBackStack();
                ((MainActivity) getActivity()).pane.openPane();
                Intent rejectRequestIntent = new Intent(getActivity(), ToxService.class);
                rejectRequestIntent.setAction(Constants.REJECT_FRIEND_REQUEST);
                rejectRequestIntent.putExtra("key", toxSingleton.activeFriendRequestKey);
                getActivity().startService(rejectRequestIntent);
            }
        });

        return rootView;
    }
}
