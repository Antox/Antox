package chat.tox.antox.av

final case class YuvVideoFrame(width: Int, height: Int, y: Array[Byte], u: Array[Byte], v: Array[Byte], yStride: Int, uStride: Int, vStride: Int) {
  /*def pack(packedYuv: Array[Int]): Unit = {
    var i = 0
    while (i < height) {
      var j = 0
      while (j < width) {
        val yx: Int = y((i * yStride) + j) & 0xff
        val ux: Int = u(((i / 2) * uStride) + (j / 2)) & 0xff
        val vx: Int = v(((i / 2) * vStride) + (j / 2)) & 0xff

        val currPos = (i * width) + j

        val r = FormatConversions.YUVtoR(yx, ux, vx)
        val g = FormatConversions.YUVtoG(yx, ux, vx)
        val b = FormatConversions.YUVtoB(yx, ux, vx)
        argb(currPos) = Color.argb(255, r, g, b)

        j += 1
      }

      i += 1
    }
  } */

  def pack(packedYuv: Array[Int]): Unit = {
    var i = 0
    while (i < height) {
      var j = 0
      while (j < width) {
        val yx: Int = y((i * yStride) + j) & 0xff
        val ux: Int = u(((i / 2) * uStride) + (j / 2)) & 0xff
        val vx: Int = v(((i / 2) * vStride) + (j / 2)) & 0xff

        val currPos = (i * width) + j

        packedYuv(currPos) = (yx << 24) | (ux << 16) | (vx << 8)
        j += 1
      }

      i += 1
    }
  }

}
