package chat.tox.antox.av

import android.graphics.Color

final case class YuvVideoFrame(width: Int, height: Int, y: Array[Byte], u: Array[Byte], v: Array[Byte], yStride: Int, uStride: Int, vStride: Int) {
  def toRgb(r: Array[Int] = new Array(width * height),
            g: Array[Int] = new Array(width * height),
            b: Array[Int] = new Array(width * height)): RgbVideoFrame = {
    
    for (i <- 0 until height; j <- 0 until width) {
      val yx: Int = y((i * yStride) + j) & 0xff
      val ux: Int = u(((i / 2) * uStride) + (j / 2)) & 0xff
      val vx: Int = v(((i / 2) * vStride) + (j / 2)) & 0xff

      val currPos = (i * width) + j

      r(currPos) = FormatConversions.YUVtoR(yx, ux, vx)
      g(currPos) = FormatConversions.YUVtoG(yx, ux, vx)
      b(currPos) = FormatConversions.YUVtoB(yx, ux, vx)
    }

    RgbVideoFrame(width, height, r, g, b)
  }


}

final case class RgbVideoFrame(width: Int, height: Int, r: Array[Int], g: Array[Int], b: Array[Int]) {
  def toArgbArray: Array[Int] = {
    val argbArray: Array[Int] = new Array(width * height)

    for (i <- 0 until height; j <- 0 until width) {
      val pos = (i * width) + j
      argbArray(pos) = Color.argb(255, r(pos), g(pos), b(pos))
    }

    argbArray
  }
}