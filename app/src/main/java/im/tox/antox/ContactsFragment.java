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

    public void updateFriends() {
        adapter = main_act.adapter;
        friendListView.setAdapter(adapter);
        System.out.println("updated friends");
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
