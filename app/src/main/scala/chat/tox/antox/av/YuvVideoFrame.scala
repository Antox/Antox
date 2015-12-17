package chat.tox.antox.av

final case class YuvVideoFrame(width: Int, height: Int, y: Array[Byte], u: Array[Byte], v: Array[Byte], yStride: Int, uStride: Int, vStride: Int) {
  def asRgb(r: Array[Int], g: Array[Int], b: Array[Int]): Unit = {
    
    for (i <- 0 until height; j <- 0 until width) {
      val yx: Int = y((i * yStride) + j) & 0xff
      val ux: Int = u(((i / 2) * uStride) + (j / 2)) & 0xff
      val vx: Int = v(((i / 2) * vStride) + (j / 2)) & 0xff

      val currPos = (i * width) + j

      r(currPos) = FormatConversions.YUVtoR(yx, ux, vx)
      g(currPos) = FormatConversions.YUVtoG(yx, ux, vx)
      b(currPos) = FormatConversions.YUVtoB(yx, ux, vx)
    }
  }

}
