package chat.tox.antox.av

import android.graphics.{Bitmap, Color, Matrix, RectF}
import android.view.SurfaceHolder.Callback
import android.view.{SurfaceHolder, SurfaceView}
import rx.lang.scala.schedulers.AndroidMainThreadScheduler
import rx.lang.scala.{Observable, Subscription}

import scala.collection.mutable

class VideoDisplay(videoFrameObservable: Observable[YuvVideoFrame], surfaceView: SurfaceView, minBufferLength: Int) {

  var active: Boolean = false
  var bitmap: Bitmap = _

  val videoBuffer = new mutable.Queue[YuvVideoFrame]()

  var callVideoFrameSubscription: Option[Subscription] = None

  var dirty = true

  var width: Int = 0
  var height: Int = 0

  def start(): Unit = {
    callVideoFrameSubscription = Some(videoFrameObservable
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(frame => videoBuffer.enqueue(frame)))

    surfaceView.getHolder.addCallback(new Callback {
      override def surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int): Unit = {}

      override def surfaceCreated(holder: SurfaceHolder): Unit = {
        bitmap = Bitmap.createBitmap(surfaceView.getWidth, surfaceView.getHeight, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.argb(255, 0, 0, 0))
      }

      override def surfaceDestroyed(holder: SurfaceHolder): Unit = {}
    })

    active = true

    new Thread(new Runnable {
      override def run(): Unit = {
        while (active) {
          if (videoBuffer.length > minBufferLength) {
            val frame = videoBuffer.dequeue()
            processVideoFrame(frame)
          }
        }
      }
    }, "VideoDisplayThread").start()
  }

  def recreate(): Unit = {
    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    dirty = false
  }

  def processVideoFrame(videoFrame: YuvVideoFrame): Unit = {
    if (videoFrame.width != width || videoFrame.height != height) {
      width = videoFrame.width
      height = videoFrame.height

      dirty = true
    }

    if (dirty) recreate()

    val rgbFrame = videoFrame.toRgb()

    val holder = surfaceView.getHolder
    Option(holder.lockCanvas()).foreach(canvas => {
      bitmap.setPixels(rgbFrame.toArgbArray, 0, videoFrame.width, 0, 0, videoFrame.width, videoFrame.height)
      val matrix = new Matrix()
      val currentRect = new RectF(0, 0, videoFrame.width, videoFrame.height)
      val desiredRect = new RectF(0, 0, canvas.getWidth, canvas.getHeight)
      matrix.setRectToRect(currentRect, desiredRect, Matrix.ScaleToFit.CENTER)

      println("rendering to the surface")
      canvas.drawBitmap(bitmap, matrix, null)

      holder.unlockCanvasAndPost(canvas)
    })
  }

  def stop(): Unit = {
    active = false
    callVideoFrameSubscription.foreach(_.unsubscribe())
  }
}
