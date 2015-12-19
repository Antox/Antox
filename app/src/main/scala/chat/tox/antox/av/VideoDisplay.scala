package chat.tox.antox.av

import android.content.Context
import android.graphics._
import android.support.v8.renderscript._
import android.view.{TextureView, View}
import chat.tox.antox.ScriptC_yuvToRgb
import chat.tox.antox.utils.AntoxLog
import org.apache.commons.collections4.queue.CircularFifoQueue
import rx.lang.scala.schedulers.AndroidMainThreadScheduler
import rx.lang.scala.{Observable, Subscription}

class VideoDisplay(videoFrameObservable: Observable[YuvVideoFrame], context: Context, videoView: TextureView, minBufferLength: Int) {

  var renderer: Renderer = _
  var bitmap: Bitmap = _

  val videoBuffer = new CircularFifoQueue[YuvVideoFrame](minBufferLength * 4)

  var callVideoFrameSubscription: Option[Subscription] = None

  def start(): Unit = {
    videoView.setVisibility(View.VISIBLE)
    videoView.setOpaque(false)

    renderer = new Renderer(videoView, context, videoBuffer, minBufferLength)
    new Thread(renderer, "VideoDisplayThread").start()
    videoView.setSurfaceTextureListener(renderer)

    callVideoFrameSubscription = Some(videoFrameObservable
      .observeOn(AndroidMainThreadScheduler())
      .subscribe(frame => videoBuffer.add(frame)))
  }


  def stop(): Unit = {
    renderer.stop()

    callVideoFrameSubscription.foreach(_.unsubscribe())
  }
}

object Renderer {
  val releaseInCallback = false
}

class Renderer(textureView: TextureView, context: Context, videoBuffer: CircularFifoQueue[YuvVideoFrame], minBufferLength: Int) extends Runnable with TextureView.SurfaceTextureListener {

  val lock = new Object()
  var mSurfaceTexture: Option[SurfaceTexture] = None
  var mEglCore: Option[EglCore] = None

  var active: Boolean = false
  var dirty = true

  var width: Int = 0
  var height: Int = 0

  // arrays are kept for performance reasons
  def genColorArray = new Array[Int](width * height)

  var packedYuv: Array[Int] = _

  //var rs: RenderScript = RenderScript.create(context, RenderScript.ContextType.DEBUG)
  var inAllocation: Allocation = _
  var outAllocation: Allocation = _
  var yuvToRgbScript: ScriptC_yuvToRgb = _


  override def run(): Unit = {
    active = true

    lock synchronized {
      while (active && mSurfaceTexture.isEmpty) {
        lock.wait()
      }

      if (!active) {
        return
      }
    }

    AntoxLog.debug("Got surface texture.")

    mEglCore = Some(new EglCore(null, EglCore.FLAG_TRY_GLES3))
    for (
      eglCore <- mEglCore;
      surfaceTexture <- mSurfaceTexture;
      windowSurface <- Option(new WindowSurface(eglCore, surfaceTexture))) {

      AntoxLog.debug("Initialised window surface.")

      windowSurface.makeCurrent()
      //createScript(surfaceTexture)

      while (active) {
        try {
          if (videoBuffer.size() > minBufferLength) {
            val frame = videoBuffer.poll()
            renderVideoFrame(windowSurface, frame)
          }
        } catch {
          case e: Exception =>
            e.printStackTrace()
        }
      }

      windowSurface.release()
      eglCore.release()
      surfaceTexture.release()

      AntoxLog.debug("Renderer thread exiting.")
    }

  }

  def recreate(): Unit = {
    packedYuv = genColorArray

    adjustAspectRatio(width, height)

    dirty = false
  }

//  def createScript(surfaceTexture: SurfaceTexture): Unit = {
//    yuvToRgbScript = new ScriptC_yuvToRgb(rs)
//
//    val yuvType = new Type.Builder(rs, Element.I32(rs)).setX(width).setY(height)
//    inAllocation = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT)
//
//    val rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height)
//    outAllocation = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_IO_OUTPUT | Allocation.USAGE_SCRIPT)
//    outAllocation.setSurface(new Surface(surfaceTexture))
//  }


  def renderVideoFrame(eglSurface: WindowSurface, videoFrame: YuvVideoFrame): Unit = {
    if (videoFrame.width != width || videoFrame.height != height) {
      width = videoFrame.width
      height = videoFrame.height

      dirty = true
    }

    if (dirty) recreate()

    println("rendering to the surface")

    val startConversionTime = System.currentTimeMillis()

    videoFrame.pack(packedYuv) // does an in-place conversion using existing arrays
    inAllocation.copyFrom(packedYuv)
    yuvToRgbScript.forEach_yuvToRgb(inAllocation, outAllocation)
    outAllocation.ioSend()

    AntoxLog.debug(s"conversion took ${System.currentTimeMillis() - startConversionTime}")
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
    textureView.getTransform(txform)
    txform.setScale(newWidth.toFloat / viewWidth, newHeight.toFloat / viewHeight)
    txform.postTranslate(xoff, yoff)
    textureView.setTransform(txform)
  }

  def stop(): Unit = {
    active = false
  }

  override def onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int): Unit = {

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

    Renderer.releaseInCallback
  }
}
