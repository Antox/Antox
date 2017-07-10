package chat.tox.antox.utils

import java.io._

import android.content.Context
import android.graphics.Bitmap
import chat.tox.antox.utils.StorageType._

object FileUtils {

  val imageExtensions = List("jpg", "jpeg", "gif", "png")

  /**
    * Gets the directory designated by 'path' from the appropriate place based on 'storageType'
    */
  def getDirectory(path: String, storageType: StorageType, context: Context): File = {
    if (storageType == StorageType.EXTERNAL) {
      new File(path)
    } else {
      new File(context.getFilesDir, path)
    }
  }

  def copy(source: File, destination: File): Unit = {
    val inStream = new FileInputStream(source)
    copy(inStream, destination)
  }

  def copy(inStream: FileInputStream, destination: File): Unit = {
    val outStream = new FileOutputStream(destination)
    val inChannel = inStream.getChannel
    val outChannel = outStream.getChannel
    inChannel.transferTo(0, inChannel.size(), outChannel)
    inStream.close()
    outStream.close()
  }

  def readToBytes(source: File): Option[Array[Byte]] = {
    val f = new RandomAccessFile(source, "r")
    try {
      if (f.length() <= Integer.MAX_VALUE) {
        val data = new Array[Byte](f.length().asInstanceOf[Int])
        f.readFully(data)
        Some(data)
      } else {
        None
      }
    } finally {
      f.close()
    }
  }

  def writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int, destination: File): Unit = {
    val outStream = new FileOutputStream(destination)
    bitmap.compress(format, quality, outStream)
    outStream.flush()
    outStream.close()
  }

  def writePrivateFile(fileName: String, write: String, context: Context): Unit = {
    try {
      val outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
      outputStream.write(write.getBytes)
      outputStream.close()
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
  }
}