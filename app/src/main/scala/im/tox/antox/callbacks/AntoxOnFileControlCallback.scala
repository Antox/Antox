package im.tox.antox.callbacks

import android.content.Context
import android.util.Log
import im.tox.antox.callbacks.AntoxOnFileControlCallback._
import im.tox.antox.data.State
import im.tox.antox.tox.{Reactive, ToxSingleton}
import im.tox.antox.transfer.FileStatus
import im.tox.tox4j.core.callbacks.FileControlCallback
import im.tox.tox4j.core.enums.ToxFileControl

object AntoxOnFileControlCallback {

  private val TAG = "OnFileControlCallback"
}

class AntoxOnFileControlCallback(private var ctx: Context) extends FileControlCallback {
  
  override def fileControl(friendNumber: Int, fileNumber: Int, control: ToxFileControl): Unit = {
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
      Reactive.updatedMessages.onNext(true)
    }
}
