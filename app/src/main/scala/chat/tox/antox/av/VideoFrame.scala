package chat.tox.antox.av

import android.graphics.Color

final case class VideoFrame(width: Int, height: Int, r: Array[Int], g: Array[Int], b: Array[Int]) {
  def toArgbArray(): Array[Int] = {
    val argbArray: Array[Int] = new Array(width * height)

    for (i <- 0 until height; j <- 0 until width) {
      val pos = (i * width) + j
      argbArray(pos) = Color.argb(255, r(pos), g(pos), b(pos))
    }

    argbArray
  }
}
