package chat.tox.antox.av

import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera._
import android.view.SurfaceHolder

class AntoxCamera(val id: Int, private val camera: Camera) {

  def getInfo: CameraInfo = CameraUtils.getCameraInfo(id)

  def setFaceDetectionListener(listener: FaceDetectionListener): Unit = camera.setFaceDetectionListener(listener)

  def setErrorCallback(cb: ErrorCallback): Unit = camera.setErrorCallback(cb)

  def startPreview(): Unit = camera.startPreview()

  def reconnect(): Unit = camera.reconnect()

  def setPreviewTexture(surfaceTexture: SurfaceTexture): Unit = camera.setPreviewTexture(surfaceTexture)

  def getParameters: Camera#Parameters = camera.getParameters

  def takePicture(shutter: ShutterCallback, raw: PictureCallback, jpeg: PictureCallback): Unit = camera.takePicture(shutter, raw, jpeg)

  def cancelAutoFocus(): Unit = camera.cancelAutoFocus()

  def setOneShotPreviewCallback(cb: PreviewCallback): Unit = camera.setOneShotPreviewCallback(cb)

  def stopPreview(): Unit = camera.stopPreview()

  def autoFocus(cb: AutoFocusCallback): Unit = camera.autoFocus(cb)

  def setAutoFocusMoveCallback(cb: AutoFocusMoveCallback): Unit = camera.setAutoFocusMoveCallback(cb)

  def setZoomChangeListener(listener: OnZoomChangeListener): Unit = camera.setZoomChangeListener(listener)

  def enableShutterSound(enabled: Boolean): Boolean = camera.enableShutterSound(enabled)

  def addCallbackBuffer(callbackBuffer: Array[Byte]): Unit = camera.addCallbackBuffer(callbackBuffer)

  def startFaceDetection(): Unit = camera.startFaceDetection()

  def startSmoothZoom(value: Int): Unit = camera.startSmoothZoom(value)

  def setDisplayOrientation(degrees: Int): Unit = camera.setDisplayOrientation(degrees)

  def setPreviewCallbackWithBuffer(cb: PreviewCallback): Unit = camera.setPreviewCallbackWithBuffer(cb)

  def release(): Unit = camera.release()

  def setPreviewDisplay(holder: SurfaceHolder): Unit = camera.setPreviewDisplay(holder)

  def setParameters(params: Camera#Parameters): Unit = camera.setParameters(params)

  def unlock(): Unit = camera.unlock()

  def lock(): Unit = camera.lock()

  def stopFaceDetection(): Unit = camera.stopFaceDetection()

  def takePicture(shutter: ShutterCallback, raw: PictureCallback, postview: PictureCallback, jpeg: PictureCallback): Unit = camera.takePicture(shutter, raw, postview, jpeg)

  def setPreviewCallback(cb: PreviewCallback): Unit = camera.setPreviewCallback(cb)

  def stopSmoothZoom(): Unit = camera.stopSmoothZoom()
}
