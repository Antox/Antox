package chat.tox.antox.callbacks

import android.content.Context
import android.util.Log
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.transfer.FileStatus
import chat.tox.antox.utils.AntoxLog
import im.tox.tox4j.core.callbacks.FileRecvControlCallback
import im.tox.tox4j.core.enums.ToxFileControl

class AntoxOnFileRecvControlCallback(private var ctx: Context) extends FileRecvControlCallback[Unit] {
  
  override def fileRecvControl(friendNumber: Int, fileNumber: Int, control: ToxFileControl)(state: Unit): Unit = {
      AntoxLog.debug("control type: " + control.name())
      val mTransfer = State.transfers.get(ToxSingleton.getAntoxFriend(friendNumber).get.key, fileNumber)
      mTransfer match {
        case Some(t) =>
          (control, t.status) match {
            case (ToxFileControl.RESUME, FileStatus.REQUESTSENT) =>
              State.transfers.fileTransferStarted(t.key, t.fileNumber, ctx)
            case (ToxFileControl.RESUME, FileStatus.PAUSED) =>
              State.transfers.fileTransferStarted(t.key, t.fileNumber, ctx)
            case (ToxFileControl.PAUSE, _) =>
              State.transfers.pauseFile(t.id, ctx)
            case (ToxFileControl.CANCEL, _) =>
              State.transfers.cancelFile(t.key, t.fileNumber, ctx)
            case _ =>
              AntoxLog.debug("not matched: " + control + ", " + t.status)

          }
        case None => AntoxLog.debug("Transfer not found")
      }
    }
}
