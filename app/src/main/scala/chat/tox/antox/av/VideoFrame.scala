package chat.tox.antox.av

final case class StridedYuvFrame(yuvData: YuvFrame, yStride: Int, uStride: Int, vStride: Int)

final case class YuvFrame(width: Int, height: Int, y: Array[Byte], u: Array[Byte], v: Array[Byte])

final case class NV21Frame(width: Int, height: Int, data: Array[Byte], rotation: Int)