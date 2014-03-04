package im.tox.antox;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

/**
 * Created by ollie on 28/02/14.
 */
public class ContactsFragment extends Fragment {
    /**
     * List View for displaying all the friends in a scrollable list
     */
    private ListView friendListView;
    private ListView friendRequestsView;
    /**
     * Adapter for the friendListView
     */
    private FriendsListAdapter contactsAdapter;
    private FriendRequestsAdapter friendRequestsAdapter;

    public ContactsFragment() {

    }
    public interface ContactListener {
        public void onChangeContact(int position, String contactName);
    }

    public void updateFriends() {
        contactsAdapter = main_act.contactsAdapter;
        friendListView.setAdapter(contactsAdapter);
        System.out.println("updated friends");
    }

    public void updateFriendRequests() {
        friendRequestsAdapter = main_act.friendRequestsAdapter;
        friendRequestsView.setAdapter(friendRequestsAdapter);
        System.out.println("updated friend requests");
    }

    private MainActivity main_act;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        /**
         * Stores a 2 dimensional string array holding friend details. Will be populated
         * by a tox function once implemented
         */

        main_act = (MainActivity) getActivity();



        View rootView = inflater.inflate(R.layout.fragment_contacts, container, false);
        friendListView = (ListView) rootView.findViewById(R.id.contacts);
        friendRequestsView = (ListView) rootView.findViewById(R.id.friend_requests);

        updateFriends();

        friendListView
                .setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position,
	                    long id) {
                        String friendName = parent.getItemAtPosition(position)
                                .toString();
                        ( (ContactListener) getActivity()).onChangeContact( position, friendName );
                    }
                });



        return rootView;
    }
}
