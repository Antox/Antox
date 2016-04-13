package chat.tox.antox.utils

import java.util.Random

import android.app.Activity
import android.graphics.{Color, Matrix}
import android.util.DisplayMetrics
import android.view.{TextureView, View}
import chat.tox.antox.wrapper.ToxKey

object UiUtils {

  val trimedIdLength = 8

  //Trims an ID so that it can be displayed to the user
  def trimId(id: ToxKey): String = {
    id.toString.substring(0, trimedIdLength - 1)
  }

  def sanitizeAddress(address: String): String = {
    //remove start-of-file unicode char and spaces
    address.replaceAll("\uFEFF", "").replace(" ", "")
  }

  def removeNewlines(str: String): String = {
    str.replace("\n", "").replace("\r", "")
  }

  def generateColor(hash: Int): Int = {
    val goldenRatio = 0.618033988749895
    val hue: Double = (new Random(hash).nextFloat() + goldenRatio) % 1
    Color.HSVToColor(Array(hue.asInstanceOf[Float] * 360, 0.5f, 0.7f))
  }

  def toggleViewVisibility(visibleView: View, goneViews: View*): Unit = {
    visibleView.setVisibility(View.VISIBLE)
    goneViews.foreach(_.setVisibility(View.GONE))
  }

  def getScreenWidth(activity: Activity): Int = {
    val metrics = new DisplayMetrics()
    activity.getWindowManager.getDefaultDisplay.getMetrics(metrics)

    metrics.widthPixels
  }

  def getScreenHeight(activity: Activity): Int = {
    val metrics = new DisplayMetrics()
    activity.getWindowManager.getDefaultDisplay.getMetrics(metrics)

    metrics.heightPixels
  }

  /**
    * Sets the TextureView transform to preserve the aspect ratio of the video.
    */
  def adjustAspectRatio(activity: Activity, textureView: TextureView, videoWidth: Int, videoHeight: Int) {
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

    val txform: Matrix = new Matrix()

    activity.runOnUiThread(new Runnable {
      override def run(): Unit = {
        textureView.getTransform(txform)
        txform.setScale(newWidth.toFloat / viewWidth, newHeight.toFloat / viewHeight)
        txform.postTranslate(xoff, yoff)
        textureView.setTransform(txform)
      }
    })
  }
}