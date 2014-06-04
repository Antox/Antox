package im.tox.antox.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import im.tox.antox.R;
import im.tox.antox.activities.MainActivity;
import im.tox.antox.data.AntoxDB;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.AntoxFriend;
import im.tox.antox.utils.Friend;

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

        Button accept = (Button) rootView.findViewById(R.id.acceptFriendRequest);
        Button reject = (Button) rootView.findViewById(R.id.rejectFriendRequest);
        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                class AcceptFriendRequest extends AsyncTask<Void, Void, Void> {
                    protected Void doInBackground(Void... params) {
                        AntoxDB db = new AntoxDB(getActivity().getApplicationContext());
                        db.addFriend(key, "Friend Accepted", "", "");
                        db.close();
                        try {
                            toxSingleton.jTox.confirmRequest(key);

                            //This is so wasteful. Should pass the info in the intent with the key
                            db = new AntoxDB(getActivity().getApplicationContext());
                            ArrayList<Friend> friends = db.getFriendList();
                            //Long statement but just getting size of friends list and adding one for the friend number
                            AntoxFriend friend = toxSingleton.friendsList.addFriend(toxSingleton.friendsList.all().size()+1);
                            int pos = -1;
                            for(int i = 0; i < friends.size(); i++) {
                                if(friends.get(i).friendKey.equals(key)) {
                                    pos = i;
                                    break;
                                }
                            }
                            if(pos != -1) {
                                friend.setId(key);
                                friend.setName(friends.get(pos).friendName);
                                friend.setStatusMessage(friends.get(pos).personalNote);
                            }

                            toxSingleton.jTox.save();
                        } catch (Exception e) {

                        }

                        if (toxSingleton.friend_requests.size() != 0) {

                            for (int i = 0; i < toxSingleton.friend_requests.size(); i++) {
                                if (key.equalsIgnoreCase(toxSingleton.friend_requests.get(i).requestKey)) {
                                    toxSingleton.friend_requests.remove(i);
                                    break;
                                }
                            }

                            db = new AntoxDB(getActivity().getApplicationContext());
                            db.deleteFriendRequest(key);
                            db.close();
                        }

                        return null;
                    }

                    protected void onPostExecute() {
                        ((MainActivity) getActivity()).pane.openPane();
                        ((MainActivity) getActivity()).updateLeftPane();
                    }
                }

                new AcceptFriendRequest().execute();
            }
        });

        reject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) getActivity()).pane.openPane();

                class RejectFriendRequest extends AsyncTask<Void, Void, Void> {
                    protected Void doInBackground(Void... params) {
                        AntoxDB antoxDB = new AntoxDB(getActivity().getApplicationContext());
                        antoxDB.deleteFriendRequest(key);
                        antoxDB.close();

                        if (toxSingleton.friend_requests.size() != 0) {
                            for (int j = 0; j < toxSingleton.friend_requests.size(); j++) {
                                for (int i = 0; i < toxSingleton.friend_requests.size(); i++) {
                                    if (key.equalsIgnoreCase(toxSingleton.friend_requests.get(i).requestKey)) {
                                        toxSingleton.friend_requests.remove(i);
                                        break;
                                    }
                                }
                            }
                        }

                        return null;
                    }

                    protected void onPostExecute(Long result) {
                        ((MainActivity) getActivity()).updateLeftPane();
                    }

                }

                new RejectFriendRequest().execute();
            }
        });

        return rootView;
    }
}
