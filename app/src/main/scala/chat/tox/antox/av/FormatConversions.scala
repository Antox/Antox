package chat.tox.antox.av

object FormatConversions {

  object Arrays {
	//Vars below are default for 640x480 resolution for 12bits/pixel
    var bytesNum = 460800
    var uvDataNum = (bytesNum / 3).toInt
    var yLength = (bytesNum / 1.5).toInt
    var y = new Array[Byte](yLength)
    var u = new Array[Byte](bytesNum / 6)
    var v = new Array[Byte](bytesNum / 6)
  }

  def arrays: Arrays
  def reinit(bytesNum:int): Unit = {
	arrays.bytesNum = bytesNum
	arrays.uvDataNum = (bytesNum / 3).toInt
	arrays.yLength = (bytesNum / 1.5).toInt
	arrays.y = new Array[Byte](yLength)
	arrays.u = new Array[Byte](bytesNum / 6)
	arrays.v = new Array[Byte](bytesNum / 6)
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
    val y = arrays.y
    System.arraycopy(data, 0, y, 0, arrays.yLength)
    val u = arrays.u
    val v = arrays.v

    var i = 0

	//to achieve FPS better than 25, we have to avoid even 1ms costs per frame,
	//so better not to count the / operation over 150000 times per frame
    while (i < arrays.uvDataNum) {
      if (i % 2 == 0) {
        v(i >> 1) = data(arrays.yLength + i)
      } else {
        u(i >> 1) = data(arrays.yLength + i)
      }

      i += 1
    }

    YuvFrame(nv21.width, nv21.height, y, u, v)
  }
}
