package chat.tox.antox.av

import android.app.Activity
import android.graphics._
import android.renderscript._
import android.view.{Surface, TextureView, View}
import chat.tox.antox.ScriptC_yuvToRgb
import chat.tox.antox.utils.AntoxLog
import org.apache.commons.collections4.queue.CircularFifoQueue
import rx.lang.scala.schedulers.AndroidMainThreadScheduler
import rx.lang.scala.{Observable, Subscription}

class VideoDisplay(activity: Activity, videoFrameObservable: Observable[YuvVideoFrame], videoView: TextureView, minBufferLength: Int) {

  var mRenderer: Option[Renderer] = None
  var bitmap: Bitmap = _

  val videoBuffer = new CircularFifoQueue[YuvVideoFrame](minBufferLength * 4)

  var callVideoFrameSubscription: Option[Subscription] = None

  def start(): Unit = {
    videoView.setVisibility(View.VISIBLE)
    videoView.setOpaque(false)

    for (renderer <- Option(new Renderer(activity, videoView, videoBuffer, minBufferLength))) {
      new Thread(renderer, "VideoDisplayThread").start()
      videoView.setSurfaceTextureListener(renderer)
      mRenderer = Some(renderer)
    }

    callVideoFrameSubscription = Some(videoFrameObservable
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(frame => {
        AntoxLog.debug(s"got a new frame at ${System.currentTimeMillis()}")
        videoBuffer.add(frame)
      }))
  }


  def stop(): Unit = {
    callVideoFrameSubscription.foreach(_.unsubscribe())
    mRenderer.foreach(_.stop())
  }
}

object Renderer {
  val releaseInCallback = false
}

class Renderer(activity: Activity,
               textureView: TextureView,
               videoBuffer: CircularFifoQueue[YuvVideoFrame],
               minBufferLength: Int) extends Runnable with TextureView.SurfaceTextureListener {

  val lock = new Object()
  var mSurfaceTexture: Option[SurfaceTexture] = None

  var active: Boolean = false
  var dirty = true

  var width: Int = 0
  var height: Int = 0

  // arrays are kept for performance reasons
  def genColorArray = new Array[Int](width * height)

  var packedYuv: Array[Int] = _

  var rs: RenderScript = RenderScript.create(activity, RenderScript.ContextType.DEBUG)
  var emptyAllocation: Allocation = _
  var yAllocation: Allocation = _
  var uAllocation: Allocation = _
  var vAllocation: Allocation = _

  var outAllocation: Allocation = _
  var yuvToRgbScript: ScriptC_yuvToRgb = _

  var bitmap: Bitmap = _

  var framesRendered: Long = 0
  var lastFrameTime: Long = 0

  override def run(): Unit = {
    active = true
    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY)

    lock synchronized {
      while (active && mSurfaceTexture.isEmpty) {
        lock.wait()
      }

      if (!active) {
        return
      }
    }

    AntoxLog.debug("Got surface texture.")

    for (surfaceTexture <- mSurfaceTexture) {

      AntoxLog.debug("Initialised window surface.")

      while (active) {
        try {
          if (videoBuffer.size() > minBufferLength) {
            val frame = videoBuffer.poll()
            renderVideoFrame(surfaceTexture, frame)
          }
        } catch {
          case e: Exception =>
            e.printStackTrace()
        }
      }

      surfaceTexture.release()

      AntoxLog.debug("Renderer thread exiting.")
    }

  }

  def recreate(surfaceTexture: SurfaceTexture, yStride: Int, uStride: Int, vStride: Int): Unit = {
    packedYuv = genColorArray

    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    adjustAspectRatio(width, height)
    createScript(surfaceTexture, yStride, uStride, vStride)

    dirty = false
  }

  def createScript(surfaceTexture: SurfaceTexture, yStride: Int, uStride: Int, vStride: Int): Unit = {
    yuvToRgbScript = new ScriptC_yuvToRgb(rs)

    val emptyType = new Type.Builder(rs, Element.U8(rs)).setX(width).setY(height)
    val yPlaneType = new Type.Builder(rs, Element.U8(rs)).setX(Math.max(width, Math.abs(yStride)) * height)
    val uPlaneType = new Type.Builder(rs, Element.U8(rs)).setX(Math.max(width / 2, Math.abs(uStride)) * (height / 2))
    val vPlaneType = new Type.Builder(rs, Element.U8(rs)).setX(Math.max(width / 2, Math.abs(vStride)) * (height / 2))

    emptyAllocation = Allocation.createTyped(rs, emptyType.create(), Allocation.USAGE_SCRIPT)
    yAllocation = Allocation.createTyped(rs, yPlaneType.create(), Allocation.USAGE_SCRIPT)
    uAllocation = Allocation.createTyped(rs, uPlaneType.create(), Allocation.USAGE_SCRIPT)
    vAllocation = Allocation.createTyped(rs, vPlaneType.create(), Allocation.USAGE_SCRIPT)

    yuvToRgbScript.set_y_stride(yStride)
    yuvToRgbScript.set_u_stride(uStride)
    yuvToRgbScript.set_v_stride(vStride)
    yuvToRgbScript.set_y_data(yAllocation)
    yuvToRgbScript.set_u_data(uAllocation)
    yuvToRgbScript.set_v_data(vAllocation)

    val rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height)
    outAllocation = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_IO_OUTPUT | Allocation.USAGE_SCRIPT)
    outAllocation.setSurface(new Surface(surfaceTexture))
  }

  def printFps(): Unit = {
    if (System.currentTimeMillis() - lastFrameTime >= 1000) {
      lastFrameTime = System.currentTimeMillis()
      println(s"current fps $framesRendered")
      framesRendered = 0
    }
  }

  def renderVideoFrame(surfaceTexture: SurfaceTexture, videoFrame: YuvVideoFrame): Unit = {
    val startRecreateTime = System.currentTimeMillis()
    if (videoFrame.width != width || videoFrame.height != height) {
      width = videoFrame.width
      height = videoFrame.height

      dirty = true
    }


    if (dirty) recreate(surfaceTexture, videoFrame.yStride, videoFrame.uStride, videoFrame.vStride)

    println(s"recreation took ${System.currentTimeMillis() - startRecreateTime}")

    println("rendering to the surface")
    printFps()

    val startConversionTime = System.currentTimeMillis()

    val startPackingTime = System.currentTimeMillis()
    yAllocation.copyFrom(videoFrame.y)
    uAllocation.copyFrom(videoFrame.u)
    vAllocation.copyFrom(videoFrame.v)
    AntoxLog.debug(s"packing took ${System.currentTimeMillis() - startPackingTime}")

    yuvToRgbScript.forEach_yuvToRgb(emptyAllocation, outAllocation)
    outAllocation.ioSend()

    AntoxLog.debug(s"conversion took ${System.currentTimeMillis() - startConversionTime}")

    framesRendered += 1
  }

  /**
   * Sets the TextureView transform to preserve the aspect ratio of the video.
   */
  private def adjustAspectRatio(videoWidth: Int, videoHeight: Int) {
    val viewWidth: Int = textureView.getWidth
    val viewHeight: Int = textureView.getHeight
    val aspectRatio: Double = videoHeight.toDouble / videoWidth
    var newWidth: Int = 0
    var newHeight: Int = 0
    if (viewHeight > (viewWidth * aspectRatio).toInt) {
      newWidth = viewWidth
      newHeight = (viewWidth * aspectRatio).toInt
    }
    else {
      newWidth = (viewHeight / aspectRatio).toInt
      newHeight = viewHeight
    }
    val xoff: Int = (viewWidth - newWidth) / 2
    val yoff: Int = (viewHeight - newHeight) / 2
    AntoxLog.debug(s"video=$videoWidth x $videoHeight view=$viewWidth x $viewHeight newView=$newWidth x $newHeight off=$xoff,$yoff")
    val txform: android.graphics.Matrix = new android.graphics.Matrix

    activity.runOnUiThread(new Runnable {
      override def run(): Unit = {
        textureView.getTransform(txform)
        txform.setScale(newWidth.toFloat / viewWidth, newHeight.toFloat / viewHeight)
        txform.postTranslate(xoff, yoff)
        textureView.setTransform(txform)
      }
    })
  }

  def stop(): Unit = {
    println("renderer stopped")
    videoBuffer.clear()
    active = false
  }

  override def onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int): Unit = {
    adjustAspectRatio(this.width, this.height)
  }

  override def onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int): Unit = {
    lock synchronized {
      mSurfaceTexture = Some(surface)
      lock.notify()
    }
  }

  override def onSurfaceTextureUpdated(surface: SurfaceTexture): Unit = {}

  override def onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = {
    lock synchronized {
      mSurfaceTexture = None
      active = false
    }

    false
  }
}
