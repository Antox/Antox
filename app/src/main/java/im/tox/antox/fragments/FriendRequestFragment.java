package im.tox.antox.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import im.tox.antox.R;
import im.tox.antox.data.AntoxDB;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.Tuple;
import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by ollie on 28/02/14.
 */
public class FriendRequestFragment extends Fragment {

    private String key;
    private String message;
    Subscription activeKeySub;
    private TextView k;
    private TextView m;
    private Button accept;
    private Button reject;

    ToxSingleton toxSingleton = ToxSingleton.getInstance();

    public FriendRequestFragment() {

    }

    public FriendRequestFragment(String key) {
        this.key = key;
    }

    public FriendRequestFragment(String key, String message) {
        this.key = key;
        this.message = message;
    }

    private String SplitKey(String key) {
        return key.substring(0, 38) + "\n" + key.substring(38);
    }

    @Override
    public void onResume() {
        super.onResume();
        activeKeySub = toxSingleton.activeKeyAndIsFriendSubject
                .subscribe(new Action1<Tuple<String, Boolean>>() {
                    @Override
                    public void call(Tuple<String, Boolean> activeKeyAndIfFriend) {
                        String activeKey = activeKeyAndIfFriend.x;
                        boolean isFriend = activeKeyAndIfFriend.y;
                        if (activeKey != null && !activeKey.equals("")) {
                            if (!isFriend) {
                                key = activeKey;
                                changeKey(activeKey);
                            }
                        }
                    }
                });
    }

    private void changeKey(String activeKey) {
        AntoxDB db = new AntoxDB(getActivity().getApplicationContext());
        message = db.getFriendRequestMessage(key);
        db.close();
        k.setText(key);
        m.setText(message);
        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                class AcceptFriendRequest extends AsyncTask<Void, Void, Void> {
                    @Override
                    protected Void doInBackground(Void... params) {
                        AntoxDB db = new AntoxDB(getActivity().getApplicationContext());
                        db.addFriend(key, "Friend Accepted", "", "");
                        db.close();
                        try {
                            toxSingleton.jTox.confirmRequest(key);
                            toxSingleton.jTox.save();
                        } catch (Exception e) {
                        }
                        db = new AntoxDB(getActivity().getApplicationContext());
                        db.deleteFriendRequest(key);
                        db.close();

                        return null;
                    }
                    @Override
                    protected void onPostExecute(Void result) {
                        toxSingleton.updateFriendsList(getActivity());
                        toxSingleton.updateFriendRequests(getActivity());
                        toxSingleton.clearActiveKey();
                    }
                }

                new AcceptFriendRequest().execute();
            }
        });
        reject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                class RejectFriendRequest extends AsyncTask<Void, Void, Void> {
                    @Override
                    protected Void doInBackground(Void... params) {
                        AntoxDB antoxDB = new AntoxDB(getActivity().getApplicationContext());
                        antoxDB.deleteFriendRequest(key);
                        antoxDB.close();
                        return null;
                    }
                    @Override
                    protected void onPostExecute(Void result) {
                        toxSingleton.updateFriendRequests(getActivity());
                        toxSingleton.clearActiveKey();
                    }

                }

                new RejectFriendRequest().execute();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_friendrequest, container, false);
        k = (TextView) rootView.findViewById(R.id.requestfragment_key);
        m = (TextView) rootView.findViewById(R.id.requestfragment_message);
        accept = (Button) rootView.findViewById(R.id.acceptFriendRequest);
        reject = (Button) rootView.findViewById(R.id.rejectFriendRequest);


        return rootView;
    }
}
