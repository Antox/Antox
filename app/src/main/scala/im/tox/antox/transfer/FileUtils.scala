package im.tox.antox.transfer

import java.io.{File, FileInputStream, FileOutputStream}

import android.graphics.Bitmap

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

  def writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int, destination: File): Unit = {
    val outStream = new FileOutputStream(destination)
    bitmap.compress(format, quality, outStream)
    outStream.flush()
    outStream.close()
  }
}