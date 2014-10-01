package im.tox.antox.fragments

import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import java.util.ArrayList
import im.tox.antox.R
import im.tox.antox.adapters.RecentAdapter
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.utils.FriendInfo
import rx.Observable
import rx.Observer
import rx.Subscriber
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Action1
import rx.schedulers.Schedulers
import rx.subscriptions.Subscriptions
//remove if not needed
import scala.collection.JavaConversions._

class RecentFragment extends Fragment {

  private var conversationListView: ListView = _

  private var conversationAdapter: ArrayAdapter[FriendInfo] = _

  private var sub: Subscription = _

  private var antoxDB: AntoxDB = _

  private var adapter: RecentAdapter = _

  private var rootView: View = _

  var noConversations: LinearLayout = _

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    if (rootView == null) {
      rootView = inflater.inflate(R.layout.fragment_recent, container, false)
      conversationListView = rootView.findViewById(R.id.conversations_list).asInstanceOf[ListView]
      conversationListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE)
      noConversations = rootView.findViewById(R.id.recent_no_conversations).asInstanceOf[LinearLayout]
    } else {
      rootView.getParent.asInstanceOf[ViewGroup].removeView(rootView)
    }
    Observable.create(new Observable.OnSubscribe[Cursor]() {

      override def call(observer: Subscriber[_ >: Cursor]) {
        try {
          val cursor = getCursor
          observer.onNext(cursor)
          observer.onCompleted()
        } catch {
          case e: Exception => observer.onError(e)
        }
      }
    })
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(new Action1[Cursor]() {

      override def call(cursor: Cursor) {
        if (adapter == null) {
          adapter = new RecentAdapter(getActivity, cursor)
          conversationListView.setAdapter(adapter)
        } else {
          adapter.changeCursor(cursor)
        }
      }
    })
    rootView
  }

  override def onResume() {
    super.onResume()
    sub = ToxSingleton.friendInfoListSubject
      .observeOn(AndroidSchedulers.mainThread())
      .distinctUntilChanged()
      .subscribe(new Action1[ArrayList[FriendInfo]]() {

      override def call(friends_list: ArrayList[FriendInfo]) {
          updateRecentConversations(friends_list)
      }
    })
  }

  override def onPause() {
    super.onPause()
    sub.unsubscribe()
  }

  private def getCursor(): Cursor = {
    if (this.antoxDB == null) {
      this.antoxDB = new AntoxDB(getActivity)
    }
    val cursor = this.antoxDB.getRecentCursor
    cursor
  }

  def updateRecentConversations(friendsList: ArrayList[FriendInfo]) {
    if (friendsList.size == 0) {
      noConversations.setVisibility(View.VISIBLE)
    } else {
      noConversations.setVisibility(View.GONE)
    }
    Observable.create(new Observable.OnSubscribeFunc[Cursor]() {

      override def onSubscribe(observer: Observer[_ >: Cursor]): Subscription = {
        try {
          val cursor = getCursor
          observer.onNext(cursor)
          observer.onCompleted()
        } catch {
          case e: Exception => observer.onError(e)
        }
        return Subscriptions.empty()
      }
    })
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(new Action1[Cursor]() {

      override def call(cursor: Cursor) {
        adapter.changeCursor(cursor)
      }
    })
    println("updated recent fragment")
  }
}
