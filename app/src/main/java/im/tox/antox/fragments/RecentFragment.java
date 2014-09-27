package im.tox.antox.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;

import im.tox.antox.R;
import im.tox.antox.adapters.RecentAdapter;
import im.tox.antox.data.AntoxDB;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.FriendInfo;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
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
    private AntoxDB antoxDB;
    private RecentAdapter adapter;
    private View rootView;
    private boolean paused;
    LinearLayout noConversations;

    public RecentFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_recent, container, false);
            conversationListView = (ListView) rootView.findViewById(R.id.conversations_list);
            conversationListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            noConversations = (LinearLayout) rootView.findViewById(R.id.recent_no_conversations);
        } else {
            ((ViewGroup) rootView.getParent()).removeView(rootView);
        }
        Observable.create(new Observable.OnSubscribe<Cursor>() {
            @Override
            public void call(Subscriber<? super Cursor> observer) {
                try {
                    Cursor cursor = getCursor();
                    observer.onNext(cursor);
                    observer.onCompleted();
                } catch (Exception e) {
                    observer.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Cursor>() {
            @Override
            public void call(Cursor cursor) {
                if (adapter == null) {
                    adapter = new RecentAdapter(getActivity(), cursor);
                    conversationListView.setAdapter(adapter);
                } else {
                    adapter.changeCursor(cursor);
                }
            }
        });
        return rootView;
    }

    @Override
    public void onResume(){
        super.onResume();
        sub = toxSingleton.friendInfoListSubject.observeOn(AndroidSchedulers.mainThread()).distinctUntilChanged()
                .subscribe(new Action1<ArrayList<FriendInfo>>() {
                    @Override
                    public void call(ArrayList<FriendInfo> friends_list) {
                        if (!paused) {
                            updateRecentConversations(friends_list);
                        }
                        paused = false;
                    }
                });
    }

    @Override
    public void onPause(){
        super.onPause();
        paused = true;
        sub.unsubscribe();
    }

    private Cursor getCursor() {
        if (this.antoxDB == null) {
            this.antoxDB = new AntoxDB(getActivity());
        }
        Cursor cursor = this.antoxDB.getRecentCursor();
        return cursor;
    }
    public void updateRecentConversations(ArrayList<FriendInfo> friendsList) {

        //If you have no recent conversations, display  message
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
