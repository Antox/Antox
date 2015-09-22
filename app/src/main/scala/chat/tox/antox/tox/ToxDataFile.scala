package chat.tox.antox.tox

import java.io._

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import chat.tox.antox.utils.FileUtils
import im.tox.tox4j.core.options.SaveDataOptions
import im.tox.tox4j.core.options.SaveDataOptions.ToxSave

class ToxDataFile(ctx: Context, fileName: String) {

  private val TAG = "ToxDataFile"

  def doesFileExist(): Boolean = {
    if (ctx == null) {
      Log.d(TAG, "Context is null!")
    }
    Log.d(TAG, "fileName: " + fileName)
    val myFile = ctx.getFileStreamPath(fileName)
    if (myFile == null) {
      Log.d(TAG, "myFile is null!")
    }
    myFile.exists()
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
      case e: FileNotFoundException => Log.e(TAG, "exception", e)
      case e: IOException => Log.e(TAG, "exception", e)
    } finally {
      try {
        if (fin != null) {
          fin.close()
        }
      } catch {
        case e: IOException => Log.e(TAG, "exception", e)
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
      case e1: IOException => Log.e(TAG, "exception", e1)
    }
    try {
      val output = new FileOutputStream(myFile)
      output.write(dataToBeSaved, 0, dataToBeSaved.length)
      output.close()
    } catch {
      case e: IOException => Log.e(TAG, "exception", e)
    }
  }
}
