package chat.tox.antox.av

object CameraFacing extends Enumeration {
  type CameraFacing = Value
  val Back, Front = Value

  def swap(cameraFacing: CameraFacing): CameraFacing = {
    if (cameraFacing == Back) Front else Back
  }
}
