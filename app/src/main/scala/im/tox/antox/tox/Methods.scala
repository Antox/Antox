package im.tox.antox.tox

import java.nio.{ByteBuffer, ByteOrder}
import java.util

import android.content.Context
import android.util.Log
import im.tox.antox.data.{AntoxDB, State}
import im.tox.antox.utils.{AntoxFriend, Call, CaptureAudio}
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.IOScheduler
import collection.JavaConverters._

object Methods {

  val TAG = "im.tox.antox.tox.Methods"
  val MAX_MESSAGE_LENGTH = 1367

  def sendMessage(ctx: Context, key: String, msg: String, mDbId: Option[Integer]) = {
    Observable[Boolean](subscriber => {
      val mFriend = ToxSingleton.getAntoxFriend(key)
      mFriend match {
        case None =>
        case Some(friend) => {
          val db = new AntoxDB(ctx).open(writeable = true)
          for (message <- splitMessage(msg)) {
            val mId = try {
              Some(ToxSingleton.tox.sendMessage(friend.getFriendnumber, msg))
            } catch {
              case e: Exception => {
                None
              }
            }

            mId match {
              case Some(id) => {
                mDbId match {
                  case Some(dbId) => db.updateUnsentMessage(id, dbId)
                  case None => db.addMessage(id, key, msg, has_been_received = false, has_been_read = false, successfully_sent = true, 1)
                }
              }
              case None => db.addMessage(-1, key, msg, has_been_received = false, has_been_read = false, successfully_sent = false, 1)
            }
          }
          db.close()
          ToxSingleton.updateMessages(ctx)
          subscriber.onCompleted()
        }
      }
      subscriber.onCompleted()
    }).subscribeOn(IOScheduler()).subscribe()
  }

  private def splitMessage(msg: String): Array[String] = {
    var currSplitPos = 0
    val result: util.ArrayList[String] = new util.ArrayList[String]()

    while (msg.length() - currSplitPos > MAX_MESSAGE_LENGTH) {
      val str = msg.substring(currSplitPos, currSplitPos + MAX_MESSAGE_LENGTH)
      val spacePos = str.lastIndexOf(' ')

      if (spacePos == -1) {
        result.add(str)
        currSplitPos += MAX_MESSAGE_LENGTH
      } else {
        result.add(str.substring(currSplitPos, currSplitPos + spacePos))
        currSplitPos += spacePos
      }
    }
    if (msg.length - currSplitPos > 0) {
      result.add(msg.substring(currSplitPos, msg.length))
    }

    result.asScala.toArray
  }

  def sendUnsentMessages(ctx: Context) {
    val db = new AntoxDB(ctx).open(writeable = false)
    val unsentMessageList = db.getUnsentMessageList
    db.close()
    for (unsentMessage <- unsentMessageList) {
      val mFriend = ToxSingleton.getAntoxFriend(unsentMessage.key)
      mFriend.foreach(friend => {
        if (friend.isOnline && ToxSingleton.tox != null) {
          sendMessage(ctx, unsentMessage.key, unsentMessage.message, Some(unsentMessage.id))
        }
      })
    }
  }
}
