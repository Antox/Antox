package chat.tox.antox.av

import android.app.Activity
import android.content.Context
import android.graphics.{RectF, Matrix, Color, Bitmap}
import android.view.SurfaceHolder.Callback
import android.view.{SurfaceHolder, SurfaceView}
import rx.lang.scala.JavaConversions._
import rx.lang.scala.schedulers.AndroidMainThreadScheduler

class VideoDisplay(call: Call, surfaceView: SurfaceView) {

  var bitmap: Bitmap = _

  surfaceView.getHolder.addCallback(new Callback {
    override def surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int): Unit = {}

    override def surfaceCreated(holder: SurfaceHolder): Unit = {
      bitmap = Bitmap.createBitmap(surfaceView.getWidth, surfaceView.getHeight, Bitmap.Config.ARGB_8888)
      bitmap.eraseColor(Color.argb(255, 0, 0, 0))
    }

    override def surfaceDestroyed(holder: SurfaceHolder): Unit = {}
  })

  var dirty = true

  var width: Int = 0
  var height: Int = 0

  val callVideoFrameSubscription =
    call.videoFrameObservable
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(frame => onVideoFrame(frame))

  def recreate(): Unit = {
    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    dirty = false
  }

  def onVideoFrame(videoFrame: VideoFrame): Unit = {
    if (videoFrame.width != width || videoFrame.height != height) {
      width = videoFrame.width
      height = videoFrame.height

      dirty = true
    }

    if (dirty) recreate()

    val holder = surfaceView.getHolder
    Option(holder.lockCanvas()).foreach(canvas => {
      bitmap.setPixels(videoFrame.toArgbArray(), 0, videoFrame.width, 0, 0, videoFrame.width, videoFrame.height)
      val matrix = new Matrix()
      val currentRect = new RectF(0, 0, videoFrame.width, videoFrame.height)
      val desiredRect = new RectF(0, 0, canvas.getWidth, canvas.getHeight)
      matrix.setRectToRect(currentRect, desiredRect, Matrix.ScaleToFit.CENTER)

      println("rendering to the surface")
      canvas.drawBitmap(bitmap, matrix, null)

      holder.unlockCanvasAndPost(canvas)
    })
  }

  def destroy(): Unit = {
    callVideoFrameSubscription.unsubscribe()
  }
}
