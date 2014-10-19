package im.tox.antox.tox

import android.content.Context
import android.util.Log
import im.tox.antox.data.AntoxDB
import im.tox.antox.data.State
import im.tox.antox.utils.AntoxFriend
import im.tox.antox.utils.CaptureAudio
import im.tox.antox.utils.Call
import im.tox.antox.utils.CallManager
import im.tox.jtoxcore.ToxCodecSettings
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.IOScheduler

object Methods {

  val TAG = "im.tox.antox.tox.Methods"

  def sendMessage(ctx: Context, key: String, msg: String, mId: Option[Integer]) = {
    Observable[Boolean](subscriber => {
      val mFriend = ToxSingleton.getAntoxFriend(key)
      mFriend match {
        case None =>
        case Some(friend) => {
          // NB: substring includes from start up to but not including the end position
          // Max message length in tox is 1368 bytes
          // jToxCore seems to append a null byte so split around 1367
          val utf8Bytes: Array[Byte] = msg.getBytes("UTF-8")
          val numOfMessages: Int = (utf8Bytes.length / 1367) + 1

          if (numOfMessages > 1) {

            val OneByte = 0xFFFFFF80
            val TwoByte = 0xFFFFF800
            val ThreeByte = 0xFFFF0000

            var total = 0
            var previous = 0
            var numberOfMessagesSent = 0
            for (i <- 0 until msg.length) {
              if ((msg.charAt(i) & OneByte) == 0) total += 1 else if ((msg.charAt(i) & TwoByte) == 0) total += 2 else if ((msg.charAt(i) & ThreeByte) == 0) total += 3 else total += 4
              if (numberOfMessagesSent == numOfMessages - 1) {
                sendMessageHelper(ctx, key, friend, msg.substring(previous), mId)
                //break
              } else if (total >= 1366) {
                sendMessageHelper(ctx, key, friend, msg.substring(previous, i), mId)
                numberOfMessagesSent += 1
                previous = i
                total = 0
              }
            }
          } else {
            sendMessageHelper(ctx, key, friend, msg, mId)
          }
        }
      }
      subscriber.onCompleted()
    }).subscribeOn(IOScheduler()).subscribe()
  }

  private def sendMessageHelper(ctx: Context, key: String, friend: AntoxFriend, msg: String, mDbId: Option[Integer]) = {
    Observable[Boolean](subscriber => {
      val mId = try {
        Some(ToxSingleton.jTox.sendMessage(friend, msg))
      } catch {
        case e: Exception => {
          None
        }
      }
      var db = new AntoxDB(ctx).open(true)
      mId match {
        case Some(id) => {
          mDbId match {
            case Some(dbId) => db.updateUnsentMessage(id, dbId)
            case None => db.addMessage(id, key, msg, false, false, true, 1)
          }
        }
        case None => db.addMessage(-1, key, msg, false, false, false, 1)
      }
      db.close()
      ToxSingleton.updateMessages(ctx)
      subscriber.onCompleted()
    }).subscribeOn(IOScheduler()).subscribe()
  }

  def sendUnsentMessages(ctx: Context) {
    val db = new AntoxDB(ctx).open(false)
    val unsentMessageList = db.getUnsentMessageList
    db.close()
    for (unsentMessage <- unsentMessageList) {
      val mFriend = ToxSingleton.getAntoxFriend(unsentMessage.key)
      mFriend.foreach(friend => {
        if (friend.isOnline && ToxSingleton.jTox != null) {
          sendMessage(ctx, unsentMessage.key, unsentMessage.message, Some(unsentMessage.id))
        }
      })
    }
  }

  def avAnswer(callID: Integer, toxCodecSettings: ToxCodecSettings) = {
    ToxSingleton.jTox.avAnswer(callID, toxCodecSettings)
    Log.d(TAG, "avAnswer audio sample rate: " + toxCodecSettings.audio_sample_rate)
    val call = new Call(callID, toxCodecSettings, CaptureAudio.makeObservable(callID, toxCodecSettings).subscribeOn(IOScheduler()).subscribe())
    State.calls.add(call)
  }

  def avEnd(callID: Integer) = {
    State.calls.remove(callID)
  }
}
