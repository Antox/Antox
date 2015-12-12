package chat.tox.antox.av

object FormatConversions {
  private def clip(n: Int) = if (n > 255) 255 else if (n < 0) 0 else n

  private def C(y: Int) = y - 16

  private def D(u: Int) = u - 128

  private def E(v: Int) = v - 128

  def YUVtoR(y: Int, u: Int, v: Int): Int = {
    clip((298 * C(y) + 409 * E(v) + 128) >> 8)
  }

  def YUVtoG(y: Int, u: Int, v: Int): Int = {
    clip((298 * C(y) - 100 * D(u) - 208 * E(v) + 128) >> 8)
  }

  def YUVtoB(y: Int, u: Int, v: Int): Int = {
    clip((298 * C(y) + 516 * D(u) + 128) >> 8)
  }

  def RGBtoYUV(r: Int, g: Int, b: Int): (Int, Int, Int) = {
    val y = clip((( 66 * r + 129 * g +  25 * b + 128) >> 8) +  16)
    val u = clip(((-38 * r -  74 * g + 112 * b + 128) >> 8) + 128)
    val v = clip(((112 * r -  94 * g -  18 * b + 128) >> 8) + 128)

    (y, u, v)
  }
}
