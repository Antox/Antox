package im.tox.antox.tox

import java.io._

import android.content.Context
import android.os.Environment
import android.preference.PreferenceManager
import android.util.Log
import im.tox.antox.utils.{Constants, FileUtils}

class ToxDataFile(ctx: Context, fileName: String) {

  def this(context: Context) = this(context, {
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    preferences.getString("active_account", "")
  })

  def doesFileExist(): Boolean = {
    if (ctx == null) {
      Log.d("ToxDataFile", "Context is null!")
    }
    Log.d("ToxDataFile", "fileName: " + fileName)
    val myFile = ctx.getFileStreamPath(fileName)
    if (myFile == null) {
      Log.d("ToxDataFile", "myFile is null!")
    }
    myFile.exists()
  }

  def exportFile(): Unit = {
    var saveFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
      Constants.PROFILE_EXPORT_DIRECTORY)
    if (!saveFile.exists()) {
      saveFile.mkdirs()
    }
    saveFile = new File(saveFile.getAbsolutePath + "/" + fileName + ".tox")
    FileUtils.copy(ctx.getFileStreamPath(fileName), saveFile)
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
