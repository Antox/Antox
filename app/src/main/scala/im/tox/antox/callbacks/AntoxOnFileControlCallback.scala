package im.tox.antox.callbacks

import android.content.Context
import android.util.Log
import java.nio.ByteBuffer
import im.tox.antox.data.{AntoxDB, State}
import im.tox.antox.tox.ToxSingleton
import im.tox.antox.tox.Reactive
import im.tox.antox.utils.AntoxFriend
import im.tox.antox.utils.FileTransfer
import im.tox.antox.utils.FileStatus
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
    Log.d(TAG, "control type: " + control_type.name() + ", sending: " + sending)
    val mTransfer = State.transfers.get(friend.getId, fileNumber)
    mTransfer match {
      case Some(t) => {
        (control_type, t.status, sending) match {
          case (ToxFileControl.TOX_FILECONTROL_ACCEPT, FileStatus.REQUESTSENT, true) => 
            Log.d(TAG, "fileTransferStarted")
            ToxSingleton.fileTransferStarted(t.key, t.fileNumber, ctx)
          case (ToxFileControl.TOX_FILECONTROL_ACCEPT, FileStatus.PAUSED, true) => 
            Log.d(TAG, "fileTransferStarted")
            ToxSingleton.fileTransferStarted(t.key, t.fileNumber, ctx)
          case (ToxFileControl.TOX_FILECONTROL_FINISHED, _, false) => 
            Log.d(TAG, "fileFinished")
            ToxSingleton.fileFinished(t.key, t.fileNumber, sending, ctx)
          case (ToxFileControl.TOX_FILECONTROL_PAUSE, _, true) => 
            Log.d(TAG, "pauseFile")
            ToxSingleton.pauseFile(t.id, ctx)
          case (ToxFileControl.TOX_FILECONTROL_KILL, _, true) => 
            Log.d(TAG, "cancelFile")
            ToxSingleton.cancelFile(t.key, t.fileNumber, ctx)
          case _ =>
            Log.d(TAG, "not matched: " + control_type + ", " + t.status + ", " + sending)

        }
      }
      case None => Log.d(TAG, "Transfer not found")
    }
    Reactive.updatedMessages.onNext(true)
  }
}
