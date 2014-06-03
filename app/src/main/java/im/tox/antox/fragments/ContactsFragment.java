package im.tox.antox.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;

import im.tox.antox.R;
import im.tox.antox.activities.AddFriendActivity;
import im.tox.antox.activities.FriendProfileActivity;
import im.tox.antox.activities.MainActivity;
import im.tox.antox.adapters.LeftPaneAdapter;
import im.tox.antox.data.AntoxDB;
import im.tox.antox.tox.ToxService;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.Constants;
import im.tox.antox.utils.Friend;
import im.tox.antox.utils.FriendInfo;
import im.tox.antox.utils.LeftPaneItem;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by ollie on 28/02/14.
 */
public class ContactsFragment extends Fragment {
    /**
     * List View for displaying all the friends in a scrollable list
     */
    private ListView contactsListView;
    /**
     * Adapter for the friendListView
     */
    private LeftPaneAdapter leftPaneAdapter;

    ToxSingleton toxSingleton = ToxSingleton.getInstance();

    private Subscription friendInfoSub;

    public ContactsFragment() {
    }

    public void updateContacts(ArrayList<FriendInfo> friendsList) {

        //If you have no friends, display the no friends message
        LinearLayout noFriends = (LinearLayout) getView().findViewById(R.id.contacts_no_friends);
        if (friendsList.size() == 0) {
            noFriends.setVisibility(View.VISIBLE);
        } else {
            noFriends.setVisibility(View.GONE);
        }


        leftPaneAdapter = new LeftPaneAdapter(getActivity());

        FriendInfo friends_list[] = new FriendInfo[friendsList.size()];
        friends_list = friendsList.toArray(friends_list);
        if (friends_list.length > 0) {
            //add the header corresponding to the option: All Online Offline Blocked
            LeftPaneItem friends_header = new LeftPaneItem("Contacts");
            leftPaneAdapter.addItem(friends_header);
            for (int i = 0; i < friends_list.length; i++) {
                LeftPaneItem friend = new LeftPaneItem(friends_list[i].friendKey, friends_list[i].friendName, friends_list[i].lastMessage, friends_list[i].icon, friends_list[i].unreadCount, friends_list[i].lastMessageTimestamp);
                leftPaneAdapter.addItem(friend);
            }
        }

        contactsListView.setAdapter(leftPaneAdapter);
        System.out.println("updated contacts");
    }

    private MainActivity main_act;

    @Override
    public void onResume(){
        super.onResume();
        friendInfoSub = toxSingleton.friendInfoListSubject.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ArrayList<FriendInfo>>() {
                    @Override
                    public void call(ArrayList<FriendInfo> friends_list) {
                        updateContacts(friends_list);
                    }
                });
    }

    @Override
    public void onPause(){
        super.onPause();
        friendInfoSub.unsubscribe();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        /**
         * Stores a 2 dimensional string array holding friend details. Will be populated
         * by a tox function once implemented
         */

        main_act = (MainActivity) getActivity();

        View rootView = inflater.inflate(R.layout.fragment_contacts, container, false);
        contactsListView = (ListView) rootView.findViewById(R.id.contacts_list);


        contactsListView
                .setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position,
                                            long id) {
                        LeftPaneItem item = (LeftPaneItem) parent.getAdapter().getItem(position);
                        int type = item.viewType;
                        String key = item.key;
                        if (key != "") {
                            toxSingleton.activeKeySubject.onNext(key);
                        }
                    }
                });

        contactsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View itemView, int index, long id) {
                final LeftPaneItem item = (LeftPaneItem) parent.getAdapter().getItem(index);
                final AlertDialog.Builder builder = new AlertDialog.Builder(main_act);
                final boolean isFriendRequest = item.viewType == Constants.TYPE_FRIEND_REQUEST;
                final CharSequence items[];

                SharedPreferences settingsPref = getActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);

                if (isFriendRequest) {
                    items = new CharSequence[]{
                            getResources().getString(R.string.friendrequest_accept),
                            getResources().getString(R.string.friendrequest_reject),
                            getResources().getString(R.string.friend_action_block)
                    };
                }else {
                    items = new CharSequence[]{
                            getResources().getString(R.string.friend_action_profile),
                            getResources().getString(R.string.friend_action_delete),
                            getResources().getString(R.string.friend_action_deletechat),
                            getResources().getString(R.string.friend_action_block)
                    };
                }
                builder.setTitle(main_act.getString(R.string.contacts_actions_on) + " " + item.first)
                        .setCancelable(true)
                        .setItems(items, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int index) {
                                //item.first equals the key
                                if (isFriendRequest) {
                                    switch (index) {
                                        case 0:
                                            AntoxDB db = new AntoxDB(getActivity().getApplicationContext());
                                            db.addFriend(item.first, "Friend Accepted", "", "");
                                            db.close();
                                            main_act.updateLeftPane();
                                            Intent acceptRequestIntent = new Intent(getActivity(), ToxService.class);
                                            acceptRequestIntent.setAction(Constants.ACCEPT_FRIEND_REQUEST);
                                            acceptRequestIntent.putExtra("key", item.first);
                                            main_act.startService(acceptRequestIntent);
                                            main_act.updateLeftPane();
                                            break;
                                        case 1:
                                            AntoxDB antoxDB = new AntoxDB(getActivity().getApplicationContext());
                                            antoxDB.deleteFriendRequest(item.first);
                                            antoxDB.close();
                                            main_act.updateLeftPane();
                                            Intent rejectRequestIntent = new Intent(main_act, ToxService.class);
                                            rejectRequestIntent.setAction(Constants.REJECT_FRIEND_REQUEST);
                                            rejectRequestIntent.putExtra("key", item.first);
                                            main_act.startService(rejectRequestIntent);
                                            main_act.updateLeftPane();
                                            //rejectRequest(item.first);
                                            break;
                                        case 2:
                                            showBlockDialog(getActivity(), item.first);
                                            break;
                                    }
                                } else {
                                    ArrayList<Friend> tmp = ((MainActivity) getActivity()).friendList;
                                    //Get friend key
                                    String key = "";
                                    for (int i = 0; i < tmp.size(); i++) {
                                        if (item.first.equals(tmp.get(i).friendName)) {
                                            key = tmp.get(i).friendKey;
                                            break;
                                        }
                                    }
                                    switch (index) {
                                        case 0:
                                            if (!key.equals("")) {
                                                Intent profile = new Intent(main_act, FriendProfileActivity.class);
                                                profile.putExtra("key", key);
                                                startActivity(profile);
                                            }
                                            break;

                                        case 1:
                                            //Delete friend
                                            if (!key.equals("")) {
                                                showAlertDialog(getActivity(), key);
                                            }
                                            break;
                                        case 2:
                                            AntoxDB db = new AntoxDB(getActivity());
                                            db.deleteChat(key);
                                            db.close();
                                            main_act.updateLeftPane();
                                            clearChat(key);
                                            break;
                                        case 3:
                                            showBlockDialog(getActivity(), key);
                                            break;
                                    }
                                }
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                if (item.viewType != Constants.TYPE_HEADER) {
                    alert.show();
                }
                return true;
            }
        });

        return rootView;
    }

    public void showBlockDialog(Context context, String fkey) {
        final String key = fkey;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(getResources().getString(R.string.friend_action_block_confirmation))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.button_yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                AntoxDB dbBlock = new AntoxDB(getActivity());
                                dbBlock.blockUser(key);
                                dbBlock.close();
                                clearChat(key);
                                main_act.updateLeftPane();
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.button_no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                            }
                        }
                );
        builder.show();
    }

    public void showAlertDialog(Context context, String fkey) {
        final String key= fkey;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(getResources().getString(R.string.contacts_clear_saved_logs))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.button_yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                AntoxDB db = new AntoxDB(getActivity());
                                db.deleteChat(key);
                                db.deleteFriend(key);
                                db.close();
                                clearChat(key);
                                main_act.updateLeftPane();
                                Intent intent = new Intent(getActivity(), ToxService.class);
                                intent.setAction(Constants.DELETE_FRIEND_AND_CHAT);
                                intent.putExtra("key", key);
                                getActivity().startService(intent);
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.button_no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                AntoxDB db = new AntoxDB(getActivity());
                                db.deleteFriend(key);
                                db.close();
                                clearChat(key);
                                main_act.updateLeftPane();
                                Intent intent = new Intent(getActivity(), ToxService.class);
                                intent.setAction(Constants.DELETE_FRIEND);
                                intent.putExtra("key", key);
                                getActivity().startService(intent);
                            }
                        }
                );
        builder.show();
    }
    public void clearChat(String key)
    {
        if(key.equals(toxSingleton.activeFriendKey))//check if the deleted friend was the active friend
        {
            toxSingleton.activeFriendKey=null;
            getFragmentManager().beginTransaction().
                    remove(getFragmentManager().findFragmentById(R.id.right_pane)).commit();
            main_act.activeTitle="Antox";
        }
    }
}
