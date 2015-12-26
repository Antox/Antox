package chat.tox.antox.av

object FormatConversions {
  def nv21toYuv420(nv21: NV21Frame): YuvFrame = {
    val data = new Array[Byte](nv21.data.length)
    Convert.rotateNV21(nv21.data, data, nv21.width, nv21.height, nv21.rotation)

    val yLength = (data.length / 1.5).toInt
    val y = data.slice(0, yLength)
    val u = new Array[Byte](data.length / 6)
    val v = new Array[Byte](data.length / 6)

    var i = 0
    while (i < data.length / 3) {
      if (i % 2 == 0) {
        v(i / 2) = data(yLength + i)
      } else {
        u(i / 2) = data(yLength + i)
      }

      i += 1
    }

    YuvFrame(nv21.width, nv21.height, y, u, v)
  }
}
