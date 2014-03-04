package im.tox.antox;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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
    private ListView leftPaneListView;
    /**
     * Adapter for the friendListView
     */
    private LeftPaneAdapter leftPaneAdapter;


    public ContactsFragment() {

    }

    public void onChangeFriendRequest(int position, String key, String message) {
        Fragment newFragment = new FriendRequestFragment(key, message);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.right_pane, newFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void onChangeContact(int position, String name) {
        Fragment newFragment = new ChatFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.right_pane, newFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void updateLeftPane() {
        leftPaneAdapter = main_act.leftPaneAdapter;
        leftPaneListView.setAdapter(leftPaneAdapter);
        System.out.println("updated left pane");
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



        View rootView = inflater.inflate(R.layout.fragment_leftpane, container, false);
        leftPaneListView = (ListView) rootView.findViewById(R.id.left_pane_list);

        updateLeftPane();

        leftPaneListView
                .setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position,
	                    long id) {
                        LeftPaneItem item = (LeftPaneItem) parent.getAdapter().getItem(position);
                        int type = item.viewType();
                        if (type == Constants.TYPE_CONTACT) {
                            onChangeContact(position, item.first());
                            main_act.activeTitle = item.first();
                            main_act.pane.closePane();
                        } else if (type == Constants.TYPE_FRIEND_REQUEST) {
                            String key = item.first();
                            String message = item.second();
                            onChangeFriendRequest(position, key, message);
                            main_act.activeTitle = "Friend Request";
                            main_act.pane.closePane();
                        }
                    }
                });

        return rootView;
    }
}
