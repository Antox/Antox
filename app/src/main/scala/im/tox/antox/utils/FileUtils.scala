package im.tox.antox.utils

import java.io.{FileOutputStream, FileInputStream, File}
import java.nio.channels.FileChannel

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
}