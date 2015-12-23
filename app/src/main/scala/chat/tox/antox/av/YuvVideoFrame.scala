package chat.tox.antox.av

final case class YuvVideoFrame(width: Int, height: Int, y: Array[Byte], u: Array[Byte], v: Array[Byte], yStride: Int, uStride: Int, vStride: Int)