package im.tox.antox.utils

import android.util.Log
import im.tox.antox.utils.FileTransferManager._

object FileTransferManager {
  private val TAG = "im.tox.antox.utils.FileTransferManager"
}

class FileTransferManager () {
  private var _transfers: Map[Long, FileTransfer] = Map[Long, FileTransfer]()
  private var _keyAndFileNumberToId: Map[(String, Integer), Long] = Map[(String, Integer), Long]()

  def add(t: FileTransfer) = {
    Log.d(TAG, "Adding file transfer")
    _transfers = _transfers + (t.id -> t)
    _keyAndFileNumberToId = _keyAndFileNumberToId + ((t.key, t.fileNumber) -> t.id)
  }

  def remove(id: Long): Unit = {
    Log.d(TAG, "Removing file transfer")
    val mTransfer = this.get(id)
    mTransfer match {
      case Some(t) => 
    _transfers = _transfers - id
    _keyAndFileNumberToId = _keyAndFileNumberToId - ((t.key, t.fileNumber))
      case None =>
    }
  }

  def remove(key: String, fileNumber: Integer): Unit = {
    val mId = _keyAndFileNumberToId.get(key, fileNumber)
    mId match {
      case Some(id) => this.remove(id)
      case None => 
    }
  }

  def get(id: Long): Option[FileTransfer] = {
    _transfers.get(id)
  }

  def get(address: String, fileNumber: Integer): Option[FileTransfer] = {
    val mId = _keyAndFileNumberToId.get(address, fileNumber)
    mId match {
      case Some(address) => this.get(address)
      case None => None
    }
  }
}
