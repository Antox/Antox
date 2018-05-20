package chat.tox.antox.transfer

import java.io._

import chat.tox.antox.transfer.FileStatus.FileStatus
import chat.tox.antox.utils.Constants
import chat.tox.antox.wrapper.{ContactKey, FileKind}
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter

class FileTransfer(val key: ContactKey,
                   val file: File,
                   val fileNumber: Integer,
                   val size: Long,
                   val initialProgress: Long,
                   val sending: Boolean,
                   val initialStatus: FileStatus,
                   val id: Long,
                   val fileKind: FileKind,
                   val stripExifData: Boolean = false) {

  private var progressHistory: Array[(Long, Long)] = Array[(Long, Long)]((System.currentTimeMillis, initialProgress))

  private var _status: FileStatus = initialStatus

  def status: FileStatus = _status

  def status_=(x: FileStatus): Unit = {
    _status = x
  }

  private var _outputStream: Option[FileOutputStream] = None

  private var _bOutputStream: Option[BufferedOutputStream] = None

  private var _inputStream: Option[FileInputStream] = None

  private var _bInputStream: Option[BufferedInputStream] = None
  // private var _bInputStream: Option[InputStream] = None

  def progress: Long = {
    progressHistory.last._2
  }

  def addToProgress(prog: Long): Unit = {
    progressHistory = progressHistory :+(System.currentTimeMillis, prog)
  }

  def getProgressSinceXAgo(ms: Long): Option[(Long, Long)] = {
    val current = progressHistory.last
    val currentTime = current._1
    val currentProgress = current._2
    val mProgress = progressHistory.filter(x => x._1 < currentTime - ms).reverse.headOption
    mProgress match {
      case Some((time, progress)) => Some((currentProgress - progress, currentTime - time))
      case None => None
    }
  }

  private def createOutputStream() = {
    if ((!sending) && _outputStream.isEmpty) {
      try {
        val output = new FileOutputStream(file, true)
        if (output != null) {
          _outputStream = Some(output)
          try {
            val bOutput = new BufferedOutputStream(output)
            if (bOutput != null) {
              _bOutputStream = Some(bOutput)
            }
          } catch {
            case e: Exception => e.printStackTrace()
          }
        }
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
  }

  private def createInputStream() = {
    if (sending && _inputStream.isEmpty) {
      try {
        val input = new FileInputStream(file)
        if (input != null) {
          _inputStream = Some(input)
          // -----------------------------
          // -----------------------------
          try {
            val splitFileName = file.getName.split("\\.")
            val extension = splitFileName(splitFileName.length - 1)
            val bInput = if (stripExifData && Constants.EXIF_FORMATS.contains(extension.toLowerCase)) {
              val bFile = new Array[Byte](file.length().toInt)
              input.read(bFile)
              val baos = new ByteArrayOutputStream()
              val rw = new ExifRewriter()
              rw.removeExifMetadata(bFile, baos)
              baos.close()
              new BufferedInputStream(new ByteArrayInputStream(baos.toByteArray))
            }
            else {
              new BufferedInputStream(input)
            }
            // -----------------------------
            // -----------------------------
            if (bInput != null) {
              _bInputStream = Some(bInput)
            }
          } catch {
            case e: Exception => e.printStackTrace()
          }
        } else {
          _inputStream = None
        }
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
  }

  def resumeInputStream(pos: Long) = {
    if (sending) {

      try {
        _bInputStream match {
          case Some(s) =>
            s.close()
          case None =>
        }

        _inputStream match {
          case Some(s) =>
            s.close()
          case None =>
        }

        val input = new FileInputStream(file)
        if (input != null) {
          _inputStream = Some(input)
          // -----------------------------
          // -----------------------------
          try {
            val splitFileName = file.getName.split("\\.")
            val extension = splitFileName(splitFileName.length - 1)
            val bInput = if (stripExifData && Constants.EXIF_FORMATS.contains(extension.toLowerCase)) {
              val bFile = new Array[Byte](file.length().toInt)
              input.read(bFile)
              val baos = new ByteArrayOutputStream()
              val rw = new ExifRewriter()
              rw.removeExifMetadata(bFile, baos)
              baos.close()
              val bis = new BufferedInputStream(new ByteArrayInputStream(baos.toByteArray))
              var skipped: Long = 0
              skipped = bis.skip(pos - 1)
              while (skipped < (pos - 1)) {
                // skip until wanted position is reached
                skipped = skipped + bis.skip((pos - 1) - skipped)
              }
              bis
            }
            else {
              val bis = new BufferedInputStream(input)
              var skipped: Long = 0
              skipped = bis.skip(pos - 1)
              while (skipped < (pos - 1)) {
                // skip until wanted position is reached
                skipped = skipped + bis.skip((pos - 1) - skipped)
              }
              bis
            }
            // -----------------------------
            // -----------------------------
            if (bInput != null) {
              _bInputStream = Some(bInput)
            }
          } catch {
            case e: Exception => e.printStackTrace()
          }
        } else {
          _inputStream = None
        }
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
  }


  //def readData(reset: Boolean, chunkSize: Integer): Option[Array[Byte]] = {
  def readData(pos: Long, progress: Long, chunkSize: Integer): Option[Array[Byte]] = {
    //Log.d(TAG, "reading data from " + file.getPath)

    if (pos != progress) {
      // we need to seek. close inputstream, open it and skip some bytes
      resumeInputStream(pos)
    }
    else {
      createInputStream()
    }
    _bInputStream match {
      case Some(s) =>
        val data = new Array[Byte](chunkSize)
        s.read(data, 0, chunkSize)
        Some(data)
      case None =>
        None
    }
  }

  def writeData(data: Array[Byte]): Boolean = {
    //returns true if finished
    //Log.d(TAG, "writing data to " + file.getPath)
    createOutputStream()
    _bOutputStream match {
      case Some(s) =>
        try {
          s.write(data, 0, data.length)
          addToProgress(this.progress + data.length)
          if (progress == size) {
            s.flush()
            s.close()
            _outputStream = None
            _bOutputStream = None
            true
          } else {
            false
          }
        } catch {
          case e: Exception => e.printStackTrace()
            false
        }
      case None =>
        false
    }
  }

}
