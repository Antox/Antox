package im.tox.antox;

import android.support.v4.app.Fragment;
import android.content.Intent;
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
    /**
     * Adapter for the friendListView
     */
    private FriendsListAdapter adapter;

    public ContactsFragment() {

    }
    public interface ContactListener {
        public void onChangeContact(int position, String contactName);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        /**
         * Stores a 2 dimensional string array holding friend details. Will be populated
         * by a tox function once implemented
         */
        String[][] friends = {
                // 0 - offline, 1 - online, 2 - away, 3 - busy
                {"1", "astonex", "status"}, {"0", "irungentoo", "status"},
                {"2", "nurupo", "status"}, {"3", "sonOfRa", "status"}
        };

        /* Go through status strings and set appropriate resource image */
        FriendsList friends_list[] = new FriendsList[friends.length];

        for (int i = 0; i < friends.length; i++) {
            if (friends[i][0].equals("1"))
                friends_list[i] = new FriendsList(R.drawable.ic_status_online,
                        friends[i][1], friends[i][2]);
            else if (friends[i][0].equals("0"))
                friends_list[i] = new FriendsList(R.drawable.ic_status_offline,
                        friends[i][1], friends[i][2]);
            else if (friends[i][0].equals("2"))
                friends_list[i] = new FriendsList(R.drawable.ic_status_away,
                        friends[i][1], friends[i][2]);
            else if (friends[i][0].equals("3"))
                friends_list[i] = new FriendsList(R.drawable.ic_status_busy,
                        friends[i][1], friends[i][2]);
        }

        adapter = new FriendsListAdapter(getActivity(), R.layout.main_list_item,
                friends_list);

        View rootView = inflater.inflate(R.layout.fragment_contacts, container, false);
        friendListView = (ListView) rootView.findViewById(R.id.contacts);


        friendListView.setAdapter(adapter);
        final Intent chatIntent = new Intent(getActivity(), ChatActivity.class);

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
