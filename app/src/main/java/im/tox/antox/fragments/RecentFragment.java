package im.tox.antox.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import im.tox.antox.R;
import im.tox.antox.adapters.RecentAdapter;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.FriendInfo;
import im.tox.antox.utils.FriendRequest;
import im.tox.antox.utils.LeftPaneItem;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by ollie on 28/02/14.
 */
public class RecentFragment extends Fragment {

    ToxSingleton toxSingleton = ToxSingleton.getInstance();

    private ListView conversationListView;
    private ArrayAdapter<FriendInfo> conversationAdapter;
    private Subscription sub;

    public RecentFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_recent, container, false);
        conversationListView = (ListView) rootView.findViewById(R.id.conversations_list);


        conversationListView
                .setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position,
                                            long id) {
                        FriendInfo item = (FriendInfo) parent.getAdapter().getItem(position);
                        String key = item.friendKey;
                        toxSingleton.activeKeySubject.onNext(key);
                    }
                });
        return rootView;
    }

    @Override
    public void onResume(){
        super.onResume();
        sub = toxSingleton.friendInfoListSubject.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ArrayList<FriendInfo>>() {
                    @Override
                    public void call(ArrayList<FriendInfo> friends_list) {
                        updateRecentConversations(filterSortRecent(friends_list));
                    }
                });
    }

    private ArrayList<FriendInfo> filterSortRecent(ArrayList<FriendInfo> input) {
        ArrayList<FriendInfo> temp = new ArrayList<FriendInfo>();
        for (FriendInfo f: input) {
            if (!f.lastMessageTimestamp.equals(new Timestamp(0,0,0,0,0,0,0))) {
                temp.add(f);
            }
        }
        class CustomComparator implements Comparator<FriendInfo> {
            @Override
            public int compare(FriendInfo o1, FriendInfo o2) {
                return o2.lastMessageTimestamp.compareTo(o1.lastMessageTimestamp);
            }
        }

        Collections.sort(temp, new CustomComparator());

        return temp;
    }

    @Override
    public void onPause(){
        super.onPause();
        sub.unsubscribe();
    }

    public void updateRecentConversations(ArrayList<FriendInfo> friendsList) {

        //If you have no recent conversations, display  message
        LinearLayout noConversations = (LinearLayout) getView().findViewById(R.id.recent_no_conversations);
        if (friendsList.size() == 0) {
            noConversations.setVisibility(View.VISIBLE);
        } else {
            noConversations.setVisibility(View.GONE);
        }

        conversationAdapter = new RecentAdapter(getActivity(), R.layout.contact_list_item, friendsList);
        conversationListView.setAdapter(conversationAdapter);
        System.out.println("updated recent fragment");
    }
}
