package chat.tox.antox.av

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.view.Surface
import chat.tox.antox.av.CameraFacing.CameraFacing

import scala.util.Try

object CameraUtils {

  def getCameraInstance(facing: CameraFacing): Option[AntoxCamera] = {
    val cameraCount = Camera.getNumberOfCameras
    val camera =
      (0 until cameraCount)
        .map(i => (i, getCameraInfo(i)))
        .find(_._2.facing == facing.id)
        .flatMap(t => Try(new AntoxCamera(t._1, Camera.open(t._1))).toOption)

    camera
  }

  def setCameraDisplayOrientation(activity: Activity, camera: AntoxCamera): Unit = {
    val cameraInfo = getCameraInfo(camera.id)
    val rotation = activity.getWindowManager.getDefaultDisplay.getRotation

    val degrees = rotation match {
      case Surface.ROTATION_0 => 0
      case Surface.ROTATION_90 => 90
      case Surface.ROTATION_180 => 180
      case Surface.ROTATION_270 => 270
    }

    val result =
      if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
        (360 - ((cameraInfo.orientation + degrees) % 360)) % 360; // compensate the mirror
      } else {
        // back-facing
        (cameraInfo.orientation - degrees + 360) % 360
      }

    camera.setDisplayOrientation(result)
  }

  def getCameraInfo(cameraId: Int): CameraInfo = {
    val cameraInfo = new CameraInfo()
    Camera.getCameraInfo(cameraId, cameraInfo)
    cameraInfo
  }

  def deviceHasCamera(context: Context): Boolean = {
    context.getPackageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
  }
}
