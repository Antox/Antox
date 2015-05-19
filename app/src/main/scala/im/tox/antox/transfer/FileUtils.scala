package im.tox.antox.transfer

import java.io._

import android.graphics.Bitmap

import scala.util.Try

object FileUtils {

  def copy(source: File, destination: File): Unit = {
    val inStream = new FileInputStream(source)
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
      if (f.length() > Integer.MAX_VALUE) return None

      val data = new Array[Byte](f.length().asInstanceOf[Int])
      f.readFully(data)
      Some(data)
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
}