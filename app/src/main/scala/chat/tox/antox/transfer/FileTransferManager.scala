package chat.tox.antox.transfer

import java.io.{ByteArrayOutputStream, File, FileInputStream}

import android.content.Context
import android.os.Environment
import android.preference.PreferenceManager
import chat.tox.antox.Utils2
import chat.tox.antox.data.State
import chat.tox.antox.tox.{IntervalLevels, Intervals, ToxSingleton}
import chat.tox.antox.utils.{AntoxLog, BitmapManager, Constants}
import chat.tox.antox.wrapper.FileKind.AVATAR
import chat.tox.antox.wrapper.{ContactKey, FileKind, FriendKey}
import im.tox.tox4j.core.data.{ToxFileId, ToxFilename, ToxNickname}
import im.tox.tox4j.core.enums.ToxFileControl
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter
import org.scaloid.common.LoggerTag

class FileTransferManager extends Intervals {
  private val TAG = LoggerTag(getClass.getSimpleName)

  private var _transfers: Map[Long, FileTransfer] = Map[Long, FileTransfer]()
  private var _keyAndFileNumberToId: Map[(ContactKey, Integer), Long] = Map[(ContactKey, Integer), Long]()

  def isTransferring: Boolean = {
    val ret: Boolean = _transfers.exists(_._2.status == FileStatus.IN_PROGRESS)
    if (ret) {
      State.lastFileTransferAction = System.currentTimeMillis()
    }
    ret
  }

  override def interval: Int = {
    if (isTransferring) {
      IntervalLevels.WORKING.id
    } else {
      IntervalLevels.AWAKE.id
    }
  }

  def add(t: FileTransfer): Unit = {
    _transfers = _transfers + (t.id -> t)
    _keyAndFileNumberToId = _keyAndFileNumberToId + ((t.key, t.fileNumber) -> t.id)
  }

  def remove(id: Long): Unit = {
    val mTransfer = this.get(id)
    mTransfer match {
      case Some(t) =>
        _transfers = _transfers - id
        _keyAndFileNumberToId = _keyAndFileNumberToId - ((t.key, t.fileNumber))
      case None =>
    }
  }

  def remove(key: ContactKey, fileNumber: Integer): Unit = {
    val mId = _keyAndFileNumberToId.get(key, fileNumber)
    mId match {
      case Some(id) => this.remove(id)
      case None =>
    }
  }

  def get(id: Long): Option[FileTransfer] = {
    _transfers.get(id)
  }

  def get(contactKey: ContactKey, fileNumber: Integer): Option[FileTransfer] = {
    val mId = _keyAndFileNumberToId.get(contactKey, fileNumber)
    mId match {
      case Some(key) => this.get(key)
      case None => None
    }
  }


  def sendFileSendRequest(path2: String, key: FriendKey, fileKind: FileKind, fileId2: ToxFileId, context: Context): Unit = {
    val stripExif = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("strip_exif", true)
    val path = new File(path2).getAbsolutePath // use absolutepath, just to be sure
    val file = new File(path)
    val splitPath = path.split("/")
    val fileName = splitPath(splitPath.length - 1)
    val splitFileName = file.getName.split("\\.")
    val extension = splitFileName(splitFileName.length - 1)
    AntoxLog.debug("sendFileSendRequest", TAG)
    var fileId: ToxFileId = fileId2

    if (fileId == ToxFileId.empty) {
      val hash_string: String = Utils2.getMd5Hash(path + "-" + file.length())
      if (hash_string == null) {
        fileId = ToxFileId.empty
      }
      else {
        fileId = ToxFileId.unsafeFromValue(hash_string.getBytes())
      }
    }


    val length = if (stripExif && Constants.EXIF_FORMATS.contains(extension.toLowerCase)) {
      val input = new FileInputStream(file)
      val bFile = new Array[Byte](file.length().toInt)
      input.read(bFile)
      input.close()
      val baos = new ByteArrayOutputStream()
      val rw = new ExifRewriter()
      rw.removeExifMetadata(bFile, baos)
      baos.close()
      baos.toByteArray.length.toLong
    }
    else file.length()

    val mFileNumber: Option[Int] = try {
      AntoxLog.debug("Creating tox file sender", TAG)

      // send normal file, use the same "id" every time to be able to resume FT --------------
      ToxSingleton.tox.fileSend(key, fileKind.kindId, length, fileId, ToxFilename.unsafeFromValue(fileName.getBytes)) match {
        case -1 => None
        case x => Some(x)
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        None
    }

    mFileNumber.foreach(fileNumber => {
      val db = State.db
      AntoxLog.debug("adding File Transfer", TAG)
      val id = db.addFileTransfer(fileNumber, key, ToxSingleton.tox.getSelfKey, ToxSingleton.tox.getName, path, hasBeenRead = true, length.toInt, fileKind)
      State.transfers.add(new FileTransfer(key, file, fileNumber, length, 0, true, FileStatus.REQUEST_SENT, id, fileKind, stripExif))
    })
  }

  def sendFileDeleteRequest(key: FriendKey, fileKind: FileKind, context: Context): Unit = {
    ToxSingleton.tox.fileSend(key, AVATAR.kindId, 0, ToxFileId.empty, ToxFilename.unsafeFromValue("".getBytes))
    if (fileKind == FileKind.AVATAR) {
      onSelfAvatarSendFinished(key, context)
    }
  }

  /**
    * Adds a new incoming file request to the database and selects an unconflicting name for the file if necessary.
    *
    * Called when there is a new request to receive a file sent by a friend.
    * (i.e. in [[chat.tox.antox.callbacks.AntoxOnFileRecvCallback]]
    * This will not start the file transfer until it is accepted using [[acceptFile]]
    *
    * @param key             ContactKey of the friend sending the file.
    * @param senderName      Name of the friend sending the file.
    * @param fileNumber      File number
    * @param fileName        Name of the file to be received.
    *                        This will be changed to avoid conflicts if an existing
    *                        file with the same name exists and 'replaceExisting' is false.
    * @param fileKind        Kind of the incoming file.
    * @param fileSize        Size of the incoming file in bytes.
    * @param replaceExisting Whether or not to replace an existing file with the same name.
    * @param context         Android context
    */
  def fileIncomingRequest(key: ContactKey,
                          senderName: ToxNickname,
                          hasBeenRead: Boolean,
                          fileNumber: Int,
                          fileName: String,
                          fileKind: FileKind,
                          fileSize: Long,
                          replaceExisting: Boolean,
                          context: Context) {
    AntoxLog.debug("fileIncomingRequest", TAG)

    var fileN = fileName
    val fileSplit = fileName.split("\\.")
    var filePre = ""
    val fileExt = fileSplit(fileSplit.length - 1)
    for (j <- 0 until fileSplit.length - 1) {
      filePre = filePre.concat(fileSplit(j))
      if (j < fileSplit.length - 2) {
        filePre = filePre.concat(".")
      }
    }

    var file = new File(fileKind.getStorageDir(context).getPath, fileN)
    if (replaceExisting) file.delete()

    if (file.exists()) {
      var i = 1
      do {
        fileN = filePre + "(" + java.lang.Integer.toString(i) + ")" +
          "." +
          fileExt
        file = new File(fileKind.getStorageDir(context).getPath, fileN)
        i += 1
      } while (file.exists())
    }

    val db = State.db
    val id = db.addFileTransfer(fileNumber, key, key, senderName, fileN, hasBeenRead, fileSize.toInt, fileKind)
    State.transfers.add(new FileTransfer(key, file, fileNumber, fileSize, 0, false, FileStatus.REQUEST_SENT, id, fileKind))
  }

  private def fileAcceptOrReject(friendKey: FriendKey, fileNumber: Integer, context: Context, accept: Boolean) {
    AntoxLog.debug("fileAcceptReject, accepting: " + accept, TAG)
    val id = State.db.getFileId(friendKey, fileNumber)
    if (id != -1) {
      try {
        ToxSingleton.tox.fileControl(friendKey, fileNumber,
          if (accept) ToxFileControl.RESUME else ToxFileControl.CANCEL)

        if (accept) {
          State.db.fileTransferStarted(friendKey, fileNumber)
        } else {
          State.db.clearFileNumber(friendKey, fileNumber)
        }

        val transfer = State.transfers.get(id)
        transfer match {
          case Some(t) =>
            if (accept) t.status = FileStatus.IN_PROGRESS else t.status = FileStatus.CANCELLED
          case None =>
        }
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
  }

  def acceptFile(friendKey: FriendKey, fileNumber: Int, context: Context): Unit =
    fileAcceptOrReject(friendKey, fileNumber, context, accept = true)

  def rejectFile(friendKey: FriendKey, fileNumber: Int, context: Context): Unit =
    fileAcceptOrReject(friendKey, fileNumber, context, accept = false)

  def receiveFileData(key: ContactKey,
                      fileNumber: Int,
                      data: Array[Byte],
                      context: Context) {
    val mTransfer = State.transfers.get(key, fileNumber)
    val state = Environment.getExternalStorageState
    if (Environment.MEDIA_MOUNTED == state) {
      mTransfer match {
        case Some(t) =>
          t.writeData(data)
        case None =>
      }
    }
  }

  def getProgressSinceXAgo(id: Long, ms: Long): Option[(Long, Long)] = {
    val mTransfer = State.transfers.get(id)
    mTransfer match {
      case Some(t) => t.getProgressSinceXAgo(ms)
      case None => None
    }
  }

  def fileFinished(key: ContactKey, fileNumber: Integer, context: Context) {
    AntoxLog.debug("fileFinished", TAG)

    val transfer = State.transfers.get(key, fileNumber)
    transfer match {
      case Some(t) =>
        t.status = FileStatus.FINISHED
        State.db.fileTransferFinished(key, fileNumber)
        State.db.clearFileNumber(key, fileNumber)
        State.setLastFileTransferAction()
        if (t.fileKind == FileKind.AVATAR) {
          if (t.sending) {
            onSelfAvatarSendFinished(key, context)
          } else {
            // Set current avatar as invalid in avatar cache in order to get it updated to new avatar
            BitmapManager.setAvatarInvalid(t.file)
            val db = State.db
            db.updateFriendAvatar(key, Some(t.file.getName))
          }
        }

      case None => AntoxLog.debug("fileFinished: No transfer found", TAG)
    }
  }

  def cancelFile(key: ContactKey, fileNumber: Int, context: Context) {
    AntoxLog.debug("cancelFile", TAG)

    val db = State.db
    State.transfers.remove(key, fileNumber)
    db.clearFileNumber(key, fileNumber)
  }

  def getProgress(id: Long): Long = {
    val mTransfer = State.transfers.get(id)

    mTransfer match {
      case Some(t) => t.progress
      case None => 0
    }
  }

  def fileTransferStarted(key: ContactKey, fileNumber: Integer, ctx: Context) {
    AntoxLog.debug("fileTransferStarted", TAG)

    State.db.fileTransferStarted(key, fileNumber)
  }

  def pauseFile(id: Long, ctx: Context) {

    AntoxLog.debug("pauseFile", TAG)
    val mTransfer = State.transfers.get(id)
    mTransfer match {
      case Some(t) => t.status = FileStatus.PAUSED
      case None =>
    }
  }

  def onSelfAvatarSendFinished(sentTo: ContactKey, context: Context): Unit = {
    val db = State.db
    db.updateContactReceivedAvatar(sentTo, receivedAvatar = true)
    updateSelfAvatar(context, false)
  }

  def updateSelfAvatar(context: Context, updated: Boolean): Unit = {

    // don't send my avatar in battery saving mode, unless we have changed the avatar
    if ((!State.getBatterySavingMode()) || (updated)) {
      val db = State.db
      val friendList = db.friendInfoList.toBlocking.first

      friendList.filter(_.online).find(!_.receivedAvatar) match {
        case Some(friend) =>
          AVATAR.getAvatarFile(PreferenceManager.getDefaultSharedPreferences(context).getString("avatar", ""), context) match {
            case Some(file) =>
              sendFileSendRequest(file.getPath, friend.key, AVATAR,
                fileId2 =
                  ToxSingleton.tox.hash(file)
                    .map(_.getBytes)
                    .map(ToxFileId.unsafeFromValue)
                    .getOrElse(ToxFileId.empty),
                context = context)
            case None =>
              sendFileDeleteRequest(friend.key, AVATAR, context)
          }

        case None =>
        //avatar has been sent to all friends, do nothing
      }
    }
  }
}
