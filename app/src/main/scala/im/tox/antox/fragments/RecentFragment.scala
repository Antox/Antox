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
import im.tox.antox.tox.Reactive
import im.tox.antox.utils.FriendInfo
import rx.lang.scala.Observable
import rx.lang.scala.Observer
import rx.lang.scala.Subscriber
import rx.lang.scala.Subscription
import rx.lang.scala.Subject
import rx.lang.scala.subjects.BehaviorSubject
import rx.lang.scala.schedulers.IOScheduler
import rx.lang.scala.schedulers.AndroidMainThreadScheduler
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
    Observable[Cursor](subscriber => {
      try {
        val cursor = getCursor
        subscriber.onNext(cursor)
        subscriber.onCompleted()
      } catch {
        case e: Exception => subscriber.onError(e)
      }
    })
      .subscribeOn(IOScheduler())
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(cursor => {
        if (adapter == null) {
          adapter = new RecentAdapter(getActivity, cursor)
          conversationListView.setAdapter(adapter)
        } else {
          adapter.changeCursor(cursor)
        }
      })
    rootView
  }

  override def onResume() {
    super.onResume()
    sub = Reactive.friendInfoList
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(updateRecentConversations(_))
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

  def updateRecentConversations(friendsList: Array[FriendInfo]) {
    if (friendsList.size == 0) {
      noConversations.setVisibility(View.VISIBLE)
    } else {
      noConversations.setVisibility(View.GONE)
    }
    Observable[Cursor](subscriber => {
      try {
        val cursor = getCursor
        subscriber.onNext(cursor)
        subscriber.onCompleted()
      } catch {
        case e: Exception => subscriber.onError(e)
      }
    })
      .subscribeOn(IOScheduler())
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(cursor => {
        adapter.changeCursor(cursor)
      })
    println("updated recent fragment")
  }
}
