package chat.tox.antox.callbacks

import android.content.Context
import android.util.Log
import chat.tox.antox.callbacks.AntoxOnFileRecvControlCallback._
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.transfer.FileStatus
import im.tox.tox4j.core.callbacks.FileRecvControlCallback
import im.tox.tox4j.core.enums.ToxFileControl

object AntoxOnFileRecvControlCallback {

  private val TAG = "OnFileControlCallback"
}

class AntoxOnFileRecvControlCallback(private var ctx: Context) extends FileRecvControlCallback[Unit] {
  
  override def fileRecvControl(friendNumber: Int, fileNumber: Int, control: ToxFileControl)(state: Unit): Unit = {
      Log.d(TAG, "control type: " + control.name())
      val mTransfer = State.transfers.get(ToxSingleton.getAntoxFriend(friendNumber).get.getKey, fileNumber)
      mTransfer match {
        case Some(t) =>
          (control, t.status) match {
            case (ToxFileControl.RESUME, FileStatus.REQUESTSENT) =>
              Log.d(TAG, "fileTransferStarted")
              State.transfers.fileTransferStarted(t.key, t.fileNumber, ctx)
            case (ToxFileControl.RESUME, FileStatus.PAUSED) =>
              Log.d(TAG, "fileTransferResumed")
              State.transfers.fileTransferStarted(t.key, t.fileNumber, ctx)
            case (ToxFileControl.PAUSE, _) =>
              Log.d(TAG, "pauseFile")
              State.transfers.pauseFile(t.id, ctx)
            case (ToxFileControl.CANCEL, _) =>
              Log.d(TAG, "cancelFile")
              State.transfers.cancelFile(t.key, t.fileNumber, ctx)
            case _ =>
              Log.d(TAG, "not matched: " + control + ", " + t.status)

          }
        case None => Log.d(TAG, "Transfer not found")
      }
    }
}
