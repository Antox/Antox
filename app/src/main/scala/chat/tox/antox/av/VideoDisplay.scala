package chat.tox.antox.av

import android.app.Activity
import android.graphics._
import android.renderscript._
import android.view.{Surface, TextureView, View}
import chat.tox.antox.ScriptC_yuvToRgb
import chat.tox.antox.utils.{AntoxLog, UiUtils}
import org.apache.commons.collections4.queue.CircularFifoQueue
import rx.lang.scala.schedulers.AndroidMainThreadScheduler
import rx.lang.scala.{Observable, Subscription}

class VideoDisplay(activity: Activity, videoFrameObservable: Observable[StridedYuvFrame], videoView: TextureView, minBufferLength: Int) {

  val logging = false

  var mRenderer: Option[Renderer] = None
  var bitmap: Bitmap = _

  val videoBuffer = new CircularFifoQueue[StridedYuvFrame](minBufferLength * 2)

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
        if (logging) AntoxLog.debug(s"got a new frame at ${System.currentTimeMillis()}")
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
               videoBuffer: CircularFifoQueue[StridedYuvFrame],
               minBufferLength: Int) extends Runnable with TextureView.SurfaceTextureListener {

  val logging = false

  val lock = new Object()
  var mSurfaceTexture: Option[SurfaceTexture] = None

  var active: Boolean = false
  var dirty = true

  var width: Int = 0
  var height: Int = 0

  // arrays are kept for performance reasons
  def genColorArray = new Array[Int](width * height)

  var packedYuv: Array[Int] = _

  var rs: RenderScript = _
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

    if (logging) AntoxLog.debug("Got surface texture.")

    for (surfaceTexture <- mSurfaceTexture) {

      if (logging) AntoxLog.debug("Initialised window surface.")

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

      if (logging) AntoxLog.debug("Renderer thread exiting.")
    }

  }

  def recreate(surfaceTexture: SurfaceTexture, yStride: Int, uStride: Int, vStride: Int): Unit = {
    packedYuv = genColorArray

    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    UiUtils.adjustAspectRatio(activity, textureView, width, height)
    createScript(surfaceTexture, yStride, uStride, vStride)

    dirty = false
  }

  def createScript(surfaceTexture: SurfaceTexture, yStride: Int, uStride: Int, vStride: Int): Unit = {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
      rs = RenderScript.create(activity, RenderScript.ContextType.DEBUG)
    } else {
      rs = RenderScript.create(activity)
    }

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
      if (logging) println(s"current fps $framesRendered")
      framesRendered = 0
    }
  }

  def renderVideoFrame(surfaceTexture: SurfaceTexture, videoFrame: StridedYuvFrame): Unit = {
    val startRecreateTime = System.currentTimeMillis()
    if (videoFrame.yuvData.width != width || videoFrame.yuvData.height != height) {
      width = videoFrame.yuvData.width
      height = videoFrame.yuvData.height

      dirty = true
    }


    if (dirty) recreate(surfaceTexture, videoFrame.yStride, videoFrame.uStride, videoFrame.vStride)

    if (logging) println(s"recreation took ${System.currentTimeMillis() - startRecreateTime}")

    if (logging) println("rendering to the surface")

    printFps()

    val startConversionTime = System.currentTimeMillis()

    val startPackingTime = System.currentTimeMillis()
    yAllocation.copyFrom(videoFrame.yuvData.y)
    uAllocation.copyFrom(videoFrame.yuvData.u)
    vAllocation.copyFrom(videoFrame.yuvData.v)
    if (logging) AntoxLog.debug(s"packing took ${System.currentTimeMillis() - startPackingTime}")

    yuvToRgbScript.forEach_yuvToRgb(emptyAllocation, outAllocation)
    outAllocation.ioSend()

    if (logging) AntoxLog.debug(s"conversion took ${System.currentTimeMillis() - startConversionTime}")

    framesRendered += 1
  }

  def stop(): Unit = {
    if (logging) println("renderer stopped")
    videoBuffer.clear()
    active = false
  }

  override def onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int): Unit = {
    UiUtils.adjustAspectRatio(activity, textureView, this.width, this.height)
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
