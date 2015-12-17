package chat.tox.antox.av

import android.graphics.{Bitmap, Matrix, RectF}
import android.view.SurfaceHolder.Callback
import android.view.{SurfaceHolder, SurfaceView, View}
import org.apache.commons.collections4.queue.CircularFifoQueue
import rx.lang.scala.schedulers.AndroidMainThreadScheduler
import rx.lang.scala.{Observable, Subscription}

class VideoDisplay(videoFrameObservable: Observable[YuvVideoFrame], surfaceView: SurfaceView, minBufferLength: Int) {

  var active: Boolean = false
  var visible: Boolean = false
  var created: Boolean = false

  var bitmap: Bitmap = _

  val videoBuffer = new CircularFifoQueue[YuvVideoFrame](minBufferLength * 4)

  var callVideoFrameSubscription: Option[Subscription] = None

  var dirty = true

  var width: Int = 0
  var height: Int = 0

  // arrays are kept for performance reasons
  def genColorArray = new Array[Int](width * height)

  var r: Array[Int] = _
  var g: Array[Int] = _
  var b: Array[Int] = _

  def start(): Unit = {
    surfaceView.setVisibility(View.VISIBLE)

    new Thread(new Runnable {
      override def run(): Unit = {
        surfaceView.getHolder.addCallback(new Callback {
          override def surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int): Unit = {}

          override def surfaceCreated(holder: SurfaceHolder): Unit = {
            created = true
          }

          override def surfaceDestroyed(holder: SurfaceHolder): Unit = {}
        })

        while (active) {
          try {
            if (videoBuffer.size() > minBufferLength) {
              val frame = videoBuffer.poll()
              processVideoFrame(frame)
            }
          } catch {
            case e: Exception =>
              e.printStackTrace()
          }
        }
      }

    }, "VideoDisplayThread").start()

    callVideoFrameSubscription = Some(videoFrameObservable
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(frame => videoBuffer.add(frame)))

    active = true
  }

  def recreate(): Unit = {
    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    r = genColorArray
    g = genColorArray
    b = genColorArray

    dirty = false
  }

  def processVideoFrame(videoFrame: YuvVideoFrame): Unit = {
    if (videoFrame.width != width || videoFrame.height != height) {
      width = videoFrame.width
      height = videoFrame.height

      dirty = true
    }

    if (dirty) recreate()

    videoFrame.asRgb(r, g, b) // does an in-place conversion using existing arrays

    val holder = surfaceView.getHolder
    Option(holder.lockCanvas()).foreach(canvas => {
      bitmap.setPixels(FormatConversions.RgbToArgbArray(r, g, b), 0, videoFrame.width, 0, 0, videoFrame.width, videoFrame.height)
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
