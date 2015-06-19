package im.tox.antox.tox

import java.sql.Timestamp

import im.tox.antox.data.State
import im.tox.antox.utils.TimestampUtils
import im.tox.antox.wrapper._
import rx.lang.scala.subjects.BehaviorSubject

object Reactive {
  val chatActive = BehaviorSubject[Boolean](false)
  val chatActiveSub = chatActive.subscribe(x => State.chatActive(x))
  val activeKey = BehaviorSubject[Option[String]](None)
  val activeKeySub = activeKey.subscribe(x => State.activeKey(x))
  val friendList = BehaviorSubject[Array[FriendInfo]](new Array[FriendInfo](0))
  val groupList = BehaviorSubject[Array[GroupInfo]](new Array[GroupInfo](0))
  val friendRequests = BehaviorSubject[Array[FriendRequest]](new Array[FriendRequest](0))
  val groupInvites = BehaviorSubject[Array[GroupInvite]](new Array[GroupInvite](0))
  val lastMessages = BehaviorSubject[Map[String, (String, Timestamp)]](Map.empty[String, (String, Timestamp)])
  val unreadCounts = BehaviorSubject[Map[String, Integer]](Map.empty[String, Integer])
  val typing = BehaviorSubject[Boolean](false)
  val updatedMessages = BehaviorSubject[Boolean](true)
  val friendInfoList = friendList
    .combineLatestWith(lastMessages)((fl, lm) => (fl, lm))
    .combineLatestWith(unreadCounts)((tup, uc) => {
    tup match {
      case (fl, lm) =>
        fl.map(f => {
          val lastMessageTup: Option[(String, Timestamp)] = lm.get(f.key)
          val unreadCount: Option[Integer] = uc.get(f.key)
          (lastMessageTup, unreadCount) match {
            case (Some((lastMessage, lastMessageTimestamp)), Some(unreadCount)) =>
              f.copy(lastMessage = lastMessage, lastMessageTimestamp = lastMessageTimestamp, unreadCount = unreadCount)
            case (Some((lastMessage, lastMessageTimestamp)), None) =>
              f.copy(lastMessage = lastMessage, lastMessageTimestamp = lastMessageTimestamp, unreadCount = 0)
            case _ =>
              f.copy(lastMessage = "", lastMessageTimestamp = TimestampUtils.emptyTimestamp(), unreadCount = 0)
          }
        })
    }
  })

  val groupInfoList = groupList
    .combineLatestWith(lastMessages)((gl, lm) => (gl, lm))
    .combineLatestWith(unreadCounts)((tup, uc) => {
    tup match {
      case (gl, lm) =>
        gl.map(g => {
          val lastMessageTup: Option[(String, Timestamp)] = lm.get(g.key)
          val unreadCount: Option[Integer] = uc.get(g.key)
          (lastMessageTup, uc) match {
            case (Some((lastMessage, lastMessageTimestamp)), _) =>
              g.copy(lastMessage = lastMessage, lastMessageTimestamp = lastMessageTimestamp, unreadCount = 0)
            case _ =>
              g.copy(lastMessage = "", lastMessageTimestamp = TimestampUtils.emptyTimestamp(), unreadCount = 0)
          }
        })
    }
  })

  //this is bad FIXME
  val contactListElements = friendInfoList
    .combineLatestWith(friendRequests)((friendInfos, friendRequests) => (friendInfos, friendRequests)) //combine friendinfolist and friend requests and return them in a tuple
    .combineLatestWith(groupInvites)((a, gil) => (a._1, a._2, gil)) //return friendinfolist, friendrequests (a) and groupinvites (gi) in a tuple
    .combineLatestWith(groupInfoList)((a, gil) => (a._1, a._2, a._3, gil)) //return friendinfolist, friendrequests and groupinvites (a), and groupInfoList (gl) in a tuple
}