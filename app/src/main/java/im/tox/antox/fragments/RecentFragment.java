package im.tox.antox.fragments;

import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
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
import im.tox.antox.data.AntoxDB;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.FriendInfo;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

/**
 * Created by ollie on 28/02/14.
 */
public class RecentFragment extends Fragment {

    ToxSingleton toxSingleton = ToxSingleton.getInstance();

    private ListView conversationListView;
    private ArrayAdapter<FriendInfo> conversationAdapter;
    private Subscription sub;
    private String activeKey;
    private Subscription keySub;
    private AntoxDB antoxDB;
    private RecentAdapter adapter;

    public RecentFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_recent, container, false);
        this.antoxDB = new AntoxDB(getActivity());
        Cursor cursor = this.antoxDB.getRecentCursor();
        adapter = new RecentAdapter(getActivity(), cursor);
        conversationListView = (ListView) rootView.findViewById(R.id.conversations_list);
        conversationListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        conversationListView.setAdapter(adapter);
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
                        //setSelectionToKey(activeKey);
                    }
                });
        keySub = toxSingleton.activeKeySubject.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        Log.d("RecentFragment", "key subject");
                        activeKey = s;
                        //setSelectionToKey(activeKey);
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
        keySub.unsubscribe();
    }

    private Cursor getCursor() {
        Cursor cursor = this.antoxDB.getRecentCursor();
        return cursor;
    }
    public void updateRecentConversations(ArrayList<FriendInfo> friendsList) {

        //If you have no recent conversations, display  message
        LinearLayout noConversations = (LinearLayout) getView().findViewById(R.id.recent_no_conversations);
        if (friendsList.size() == 0) {
            noConversations.setVisibility(View.VISIBLE);
        } else {
            noConversations.setVisibility(View.GONE);
        }

        Observable.create(new Observable.OnSubscribeFunc<Cursor>() {
            @Override
            public Subscription onSubscribe(Observer<? super Cursor> observer) {
                try {
                    Cursor cursor = getCursor();
                    observer.onNext(cursor);
                    observer.onCompleted();
                } catch (Exception e) {
                    observer.onError(e);
                }

                return Subscriptions.empty();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Cursor>() {
                    @Override
                    public void call(Cursor cursor) {
                        adapter.changeCursor(cursor);
                    }
                });
        System.out.println("updated recent fragment");
    }
}
