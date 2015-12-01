package chat.tox.antox.tox

import java.io._

import android.content.Context
import chat.tox.antox.utils.{AntoxLog, FileUtils}
import im.tox.tox4j.core.options.SaveDataOptions
import im.tox.tox4j.core.options.SaveDataOptions.ToxSave
import org.scaloid.common.LoggerTag

class ToxDataFile(ctx: Context, fileName: String) {

  private val TAG = LoggerTag(getClass.getName)

  def doesFileExist(): Boolean = {
    AntoxLog.debug("fileName: " + fileName, TAG)
    val selfFile = ctx.getFileStreamPath(fileName)
    if (selfFile == null) {
      AntoxLog.debug("selfFile is null!", TAG)
    }
    selfFile.exists()
  }

  def exportFile(dest: File): Unit = {
    if (!dest.exists()) {
      throw new IllegalArgumentException("dest must exist")
    }

    FileUtils.copy(ctx.getFileStreamPath(fileName), new File(dest + "/" + fileName + ".tox"))
  }

  def deleteFile() {
    ctx.deleteFile(fileName)
  }

  def loadFile(): Array[Byte] = {
    var fin: FileInputStream = null
    val file = ctx.getFileStreamPath(fileName)
    var data: Array[Byte] = null
    try {
      fin = new FileInputStream(file)
      data = Array.ofDim[Byte](file.length.toInt)
      fin.read(data)
    } catch {
      case e: FileNotFoundException => e.printStackTrace()
      case e: IOException => e.printStackTrace()
    } finally {
      try {
        if (fin != null) {
          fin.close()
        }
      } catch {
        case ioe: IOException => ioe.printStackTrace()
      }
    }
    data
  }

  def loadAsSaveType(): SaveDataOptions = {
    if (doesFileExist()) {
      ToxSave(loadFile())
    } else {
      SaveDataOptions.None
    }
  }

  def saveFile(dataToBeSaved: Array[Byte]) {
    val myFile = ctx.getFileStreamPath(fileName)
    try {
      myFile.createNewFile()
    } catch {
      case e1: IOException => e1.printStackTrace()
    }
    try {
      val output = new FileOutputStream(myFile)
      output.write(dataToBeSaved, 0, dataToBeSaved.length)
      output.close()
    } catch {
      case e: IOException => e.printStackTrace()
    }
  }
}
