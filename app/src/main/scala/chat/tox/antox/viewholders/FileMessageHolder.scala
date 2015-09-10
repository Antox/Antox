package chat.tox.antox.viewholders

import java.io.File

import android.app.AlertDialog
import android.content._
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.View.{OnClickListener, OnLongClickListener}
import android.widget._
import chat.tox.antox.R
import chat.tox.antox.data.State
import chat.tox.antox.utils.BitmapManager
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.duration._


class FileMessageHolder(val view: View) extends GenericMessageHolder(view) with OnClickListener with OnLongClickListener {

  protected val imageMessage = view.findViewById(R.id.message_sent_photo).asInstanceOf[ImageView]

  protected val fileButtons = view.findViewById(R.id.file_buttons).asInstanceOf[LinearLayout]

  protected val progressLayout = view.findViewById(R.id.progress_layout).asInstanceOf[LinearLayout]

  protected val messageTitle = view.findViewById(R.id.message_title).asInstanceOf[TextView]

  protected val fileSize = view.findViewById(R.id.file_size).asInstanceOf[TextView]

  protected val fileProgressText = view.findViewById(R.id.file_transfer_progress_text).asInstanceOf[TextView]

  protected val fileProgressBar = view.findViewById(R.id.file_transfer_progress).asInstanceOf[ProgressBar]

  private var file: File = _

  private var progressSub: Subscription = _

  def setImage(file: File): Unit = {
    this.file = file
    // Set a placeholder in the image in case bitmap needs to be loaded from disk
    if (message.isMine) {
      imageMessage.setImageResource(R.drawable.sent)
    } else {
      imageMessage.setImageResource(R.drawable.received)
    }

    BitmapManager.load(file, imageMessage, isAvatar = false)
    imageMessage.setOnClickListener(this)
    imageMessage.setOnLongClickListener(this)
    imageMessage.setVisibility(View.VISIBLE)

    //TODO would be better to find a way where we didn't have to toggle all these
    messageText.setVisibility(View.GONE)
    fileSize.setVisibility(View.GONE)
    progressLayout.setVisibility(View.GONE)
    fileButtons.setVisibility(View.GONE)
    messageTitle.setVisibility(View.GONE)
    messageText.setVisibility(View.GONE)
  }

  def showFileButtons(): Unit = {
    val accept = fileButtons.findViewById(R.id.file_accept_button)
    val reject = fileButtons.findViewById(R.id.file_reject_button)

    accept.setOnClickListener(new View.OnClickListener() {

      override def onClick(view: View) {
        State.transfers.acceptFile(message.key, message.messageId, context)
      }
    })
    reject.setOnClickListener(new View.OnClickListener() {

      override def onClick(view: View) {
        State.transfers.rejectFile(message.key, message.messageId, context)
      }
    })
    fileButtons.setVisibility(View.VISIBLE)

    fileSize.setVisibility(View.GONE)
    progressLayout.setVisibility(View.GONE)
    imageMessage.setVisibility(View.GONE)
  }

  def showProgressBar(): Unit = {
    fileProgressBar.setMax(message.size)
    fileProgressBar.setVisibility(View.VISIBLE)
    progressLayout.setVisibility(View.VISIBLE)

    if (progressSub == null || progressSub.isUnsubscribed) {
      Log.d("FileProgressSub", "observer subscribing")
      progressSub = Observable.interval(500 milliseconds)
        .observeOn(AndroidMainThreadScheduler())
        .subscribe(x => {
        updateProgressBar()
      })
    }

    imageMessage.setVisibility(View.GONE)
    fileButtons.setVisibility(View.GONE)
  }

  def updateProgressBar(): Unit = {
    val updateRate = 500
    val mProgress = State.transfers.getProgressSinceXAgo(message.id, updateRate)
    val bytesPerSecond = mProgress match {
      case Some(p) => ((p._1 * 1000) / p._2).toInt
      case None => 0
    }

    if (bytesPerSecond != 0) {
      val secondsToComplete = message.size / bytesPerSecond
      fileProgressText.setText(java.lang.Integer.toString(bytesPerSecond / 1024) + " KiB/s, " +
        context.getResources.getString(R.string.file_time_remaining, secondsToComplete.toString))
    } else {
      fileProgressText.setText(java.lang.Integer.toString(bytesPerSecond / 1024) + " KiB/s")
    }
    fileProgressBar.setProgress(State.transfers.getProgress(message.id).toInt)
    if (fileProgressBar.getProgress >= message.size) {
      progressSub.unsubscribe()
      Log.d("FileProgressSub", "observer unsubscribed")
    }
  }

  def setProgressText(resID: Int): Unit = {
    fileProgressText.setText(context.getResources.getString(resID))
    fileProgressText.setVisibility(View.VISIBLE)

    bubble.setOnLongClickListener(this)
    progressLayout.setVisibility(View.VISIBLE)

    fileProgressBar.setVisibility(View.GONE)
    fileButtons.setVisibility(View.GONE)
    imageMessage.setVisibility(View.GONE)
  }

  def setFileText(text: String): Unit = {
    messageText.setText(text)
    messageTitle.setVisibility(View.VISIBLE)
    messageText.setVisibility(View.VISIBLE)
  }

  override def onClick(view: View) {
    view match {
      case _: ImageView =>
        val i = new Intent()
        i.setAction(android.content.Intent.ACTION_VIEW)
        i.setDataAndType(Uri.fromFile(file), "image/*")
        context.startActivity(i)

      case _ =>
    }
  }

  override def onLongClick(view: View): Boolean = {
    val items = Array[CharSequence](context.getResources.getString(R.string.message_delete))
    new AlertDialog.Builder(context).setCancelable(true).setItems(items, new DialogInterface.OnClickListener() {

      def onClick(dialog: DialogInterface, index: Int): Unit = index match {
        case 0 =>
          Observable[Boolean](subscriber => {
            val db = State.db
            db.deleteMessage(message.id)
            subscriber.onCompleted()
          }).subscribeOn(IOScheduler()).subscribe()

      }
    }).create().show()

    true
  }
}
