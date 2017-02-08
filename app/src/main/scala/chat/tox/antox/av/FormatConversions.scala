package chat.tox.antox.av

object FormatConversions {

  object frameSettings {
    //Vars below are default for 640x480 resolution and 12bits/pixel
    var bytesNum = 460800
    var uvDataNum = (bytesNum / 3).toInt
    var yLength = (bytesNum / 1.5).toInt
    var y = new Array[Byte](yLength)
    var u = new Array[Byte](bytesNum / 6)
    var v = new Array[Byte](bytesNum / 6)
  }

  def updateFrameSettings(bytesNum: Int): Unit = {
    frameSettings.bytesNum = bytesNum
    frameSettings.uvDataNum = (bytesNum / 3).toInt
    frameSettings.yLength = (bytesNum / 1.5).toInt
    frameSettings.y = new Array[Byte](frameSettings.yLength)
    frameSettings.u = new Array[Byte](bytesNum / 6)
    frameSettings.v = new Array[Byte](bytesNum / 6)
  }

  def nv21toYuv420(nv21: NV21Frame): YuvFrame = {
    //No need to create new array here - we do not change data elements, if don't use the Convert.rotateNV21 below
    //when we have group chats - perhaps will need to create different arrays
    val data = nv21.data

    //TODO: uncomment the rotation when rotation function becomes faster OR the rotation can be
    //performed on the video receiver tox client - it could be much efficient:
    //now this operation costs 700ms per frame per 640x480 resolution on LG L65, causing FPS < 1
    //Convert.rotateNV21(nv21.data, data, nv21.width, nv21.height, nv21.rotation)

    //Array slicing was almost the heaviest operation here.
    //much faster is to initialize the array in reinit() only once,
    //and use arraycopy to fill it correctly
    val y = frameSettings.y
    System.arraycopy(data, 0, y, 0, frameSettings.yLength)
    val u = frameSettings.u
    val v = frameSettings.v

    var i = 0
    //to achieve FPS better than 25, we have to avoid even 1ms costs per frame,
    //so better not to count the / operation over 150000 times per frame in while condition
    while (i < frameSettings.uvDataNum) {
      if (i % 2 == 0) {
        v(i >> 1) = data(frameSettings.yLength + i)
      } else {
        u(i >> 1) = data(frameSettings.yLength + i)
      }

      i += 1
    }

    YuvFrame(nv21.width, nv21.height, y, u, v)
  }
}
