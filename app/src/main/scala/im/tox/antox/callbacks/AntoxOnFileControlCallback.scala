package im.tox.antox.callbacks

import android.content.Context
import android.util.Log
import java.nio.ByteBuffer
import im.tox.antox.data.AntoxDB
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.tox.Reactive
import im.tox.antox.utils.AntoxFriend
import im.tox.jtoxcore.ToxFileControl
import im.tox.jtoxcore.callbacks.OnFileControlCallback
import AntoxOnFileControlCallback._
//remove if not needed
import scala.collection.JavaConversions._

object AntoxOnFileControlCallback {

  private val TAG = "OnFileControlCallback"
}

class AntoxOnFileControlCallback(private var ctx: Context) extends OnFileControlCallback[AntoxFriend] {

  def execute(friend: AntoxFriend, 
      sending: Boolean, 
      fileNumber: Int, 
      control_type: ToxFileControl, 
      data: Array[Byte]) {
    Log.d(TAG, "execute, control type: " + control_type.name() + " sending: " + 
      sending)
    if (control_type == ToxFileControl.TOX_FILECONTROL_FINISHED && 
      !sending) {
      Log.d(TAG, "TOX_FILECONTROL_FINISHED")
      ToxSingleton.fileFinished(friend.getId, fileNumber, ctx)
    }
    if (control_type == ToxFileControl.TOX_FILECONTROL_ACCEPT && 
      sending) {
      val antoxDB = new AntoxDB(ctx)
      val id = antoxDB.getFileId(friend.getId, fileNumber)
      if (id != -1) {
        if (!ToxSingleton.fileStatusMap.containsKey(id) || 
          ToxSingleton.fileStatusMap.get(id) == ToxSingleton.FileStatus.REQUESTSENT) {
          antoxDB.fileTransferStarted(friend.getId, fileNumber)
        }
      }
      antoxDB.close()
      if (id != -1) {
        if (!ToxSingleton.fileStatusMap.containsKey(id) || 
          ToxSingleton.fileStatusMap.get(id) == ToxSingleton.FileStatus.REQUESTSENT) {
          antoxDB.fileTransferStarted(friend.getId, fileNumber)
          Reactive.updatedMessages.onNext(true)
          ToxSingleton.sendFileData(friend.getId, fileNumber, 0, ctx)
        } else if (ToxSingleton.fileStatusMap.get(id) == ToxSingleton.FileStatus.PAUSED) {
          ToxSingleton.sendFileData(friend.getId, fileNumber, ToxSingleton.getProgress(id), ctx)
        }
      }
    }
    if (control_type == ToxFileControl.TOX_FILECONTROL_RESUME_BROKEN && 
      sending) {
      try {
        ToxSingleton.jTox.fileSendControl(friend.getFriendnumber, true, fileNumber, ToxFileControl.TOX_FILECONTROL_ACCEPT.ordinal(), 
          Array.ofDim[Byte](0))
      } catch {
        case e: Exception => e.printStackTrace()
      }
      ToxSingleton.sendFileData(friend.getId, fileNumber, ByteBuffer.wrap(data).getLong.toInt, ctx)
    }
    if (control_type == ToxFileControl.TOX_FILECONTROL_PAUSE && 
      sending) {
      val antoxDB = new AntoxDB(ctx)
      val id = antoxDB.getFileId(friend.getId, fileNumber)
      antoxDB.close()
      if (id != -1) {
        ToxSingleton.fileStatusMap.put(id, ToxSingleton.FileStatus.PAUSED)
      }
    }
    if (control_type == ToxFileControl.TOX_FILECONTROL_KILL && 
      sending) {
      ToxSingleton.cancelFile(friend.getId, fileNumber, ctx)
    }
  }
}
