package im.tox.antox.tox

import android.util.Log
import java.sql.Timestamp
import im.tox.antox.data.State
import im.tox.antox.utils.Friend
import im.tox.antox.utils.FriendInfo
import im.tox.antox.utils.FriendRequest
import rx.lang.scala.Observable
import rx.lang.scala.Observer
import rx.lang.scala.Subscriber
import rx.lang.scala.Subscription
import rx.lang.scala.Subject
import rx.lang.scala.subjects.BehaviorSubject
import rx.lang.scala.schedulers.IOScheduler
import rx.lang.scala.schedulers.AndroidMainThreadScheduler

object Reactive {
  val chatActive = BehaviorSubject[Boolean](false)
  val chatActiveSub = chatActive.subscribe(x => State.chatActive(x))
  val activeKey = BehaviorSubject[Option[String]](None)
  val activeKeySub = activeKey.subscribe(x => State.activeKey(x))
  val friendList = BehaviorSubject[Array[Friend]](new Array[Friend](0))
  val friendRequests = BehaviorSubject[Array[FriendRequest]](new Array[FriendRequest](0))
  val lastMessages = BehaviorSubject[Map[String, (String, Timestamp)]](Map.empty[String, (String, Timestamp)])
  val unreadCounts = BehaviorSubject[Map[String, Integer]](Map.empty[String, Integer])
  val typing = BehaviorSubject[Boolean](false)
  val updatedMessages = BehaviorSubject[Boolean](true)
  val updatedProgress = BehaviorSubject[Boolean](true)
  val friendInfoList = friendList
                        .combineLatestWith(lastMessages)((fl, lm) => (fl, lm))
                        .combineLatestWith(unreadCounts)((tup, uc) => {
    tup match {
      case (fl, lm) => {
          fl.map(f => {
              val lastMessageTup: Option[(String, Timestamp)] = lm.get(f.friendKey)
              val unreadCount: Option[Integer] = uc.get(f.friendKey)
              (lastMessageTup, unreadCount) match {
                case (Some((lastMessage, lastMessageTimestamp)), Some(unreadCount)) => {
                  new FriendInfo(f.isOnline, f.friendName, f.friendStatus, f.personalNote, f.friendKey, lastMessage, lastMessageTimestamp, unreadCount, f.alias)
                }
                case _ =>{
              new FriendInfo(f.isOnline, f.friendName, f.friendStatus, f.personalNote, f.friendKey, "", new Timestamp(0, 0, 0, 0, 0, 0, 0), 0, f.alias)
                }
            }
          })
        }
      }
  })
  val friendListAndRequests = friendInfoList.combineLatestWith(friendRequests)((fi, fr) => (fi, fr))
}
