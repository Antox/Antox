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
    camera.setDisplayOrientation(getCameraRotation(activity, camera))
  }

  def getCameraRotation(activity: Activity, antoxCamera: AntoxCamera, hack: Boolean = false /* TODO FIXME :-/ */): Int = {
    val rotation = activity.getWindowManager.getDefaultDisplay.getRotation

    val degrees = rotation match {
      case Surface.ROTATION_0 => 0
      case Surface.ROTATION_90 => 90
      case Surface.ROTATION_180 => 180
      case Surface.ROTATION_270 => 270
    }

    val cameraInfo = antoxCamera.getInfo

    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
      ((360 - ((cameraInfo.orientation + degrees) % 360)) % 360) + (if (hack && (degrees == Surface.ROTATION_0 || degrees == Surface.ROTATION_180)) 180 else 0); // compensate the mirror
    } else {
      // back-facing
      (cameraInfo.orientation - degrees + 360) % 360
    }
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
