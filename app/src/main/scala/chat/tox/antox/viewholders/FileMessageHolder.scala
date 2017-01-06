package chat.tox.antox.viewholders

import java.io.File

import android.app.AlertDialog
import android.content._
import android.graphics.{Color, PorterDuff}
import android.net.Uri
import android.os.Environment
import android.text.format.Formatter
import android.view.{MotionEvent, View}
import android.view.View.{OnClickListener, OnLongClickListener, OnTouchListener}
import android.webkit.MimeTypeMap
import android.webkit.MimeTypeMap._
import android.widget._
import chat.tox.antox.R
import chat.tox.antox.data.State
import chat.tox.antox.utils.{AntoxLog, BitmapManager, Constants}
import chat.tox.antox.wrapper.FriendKey
import org.scaloid.common.LoggerTag
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.duration._

class FileMessageHolder(val view: View) extends GenericMessageHolder(view) with OnClickListener with OnLongClickListener {

  private val TAG = LoggerTag(getClass.getSimpleName)

  protected val imageMessage = view.findViewById(R.id.message_sent_photo).asInstanceOf[ImageView]

  protected val fileButtons = view.findViewById(R.id.file_buttons).asInstanceOf[LinearLayout]

  protected val progressLayout = view.findViewById(R.id.progress_layout).asInstanceOf[LinearLayout]

  protected val messageTitle = view.findViewById(R.id.message_title).asInstanceOf[TextView]

  protected val fileSize = view.findViewById(R.id.file_size).asInstanceOf[TextView]

  protected val fileProgressText = view.findViewById(R.id.file_transfer_progress_text).asInstanceOf[TextView]

  protected val fileProgressBar = view.findViewById(R.id.file_transfer_progress).asInstanceOf[ProgressBar]

  protected val imageLoading = view.findViewById(R.id.image_loading).asInstanceOf[ProgressBar]

  private var file: File = _

  private var progressSub: Subscription = _

  private var imageLoadingSub: Option[Subscription] = None

  def render(): Unit = {
    imageLoading.setVisibility(View.GONE)
  }

  def setImage(file: File, isImage: Boolean): Unit = {
    this.file = file
    // Start a loading indicator in case the bitmap needs to be loaded from disk
    imageMessage.setImageBitmap(null)
    imageLoading.setVisibility(View.VISIBLE)

    if (isImage) {
      imageLoadingSub.foreach(_.unsubscribe())
    }

    if (isImage) {
      imageLoadingSub = Some(BitmapManager.load(file, isAvatar = false).subscribe(image => {
        imageLoading.setVisibility(View.GONE)
        imageMessage.setImageBitmap(image)
      }))
    }
    else {
      imageLoading.setVisibility(View.GONE)
      imageMessage.setScaleType(ImageView.ScaleType.CENTER_INSIDE)
      imageMessage.setImageResource(R.drawable.ic_action_attachment_2)

      if (file.length > 0) {
        fileSize.setText(Formatter.formatFileSize(context, file.length))
        fileSize.setVisibility(View.VISIBLE)
      }
      else {
        fileSize.setVisibility(View.GONE)
      }
    }

    imageMessage.setOnClickListener(this)
    imageMessage.setOnLongClickListener(this)
    imageMessage.setVisibility(View.VISIBLE)

    //TODO would be better to find a way where we didn't have to toggle all these
    progressLayout.setVisibility(View.GONE)
    fileButtons.setVisibility(View.GONE)
    messageTitle.setVisibility(View.GONE)
    if (isImage) {
      fileSize.setVisibility(View.GONE)
      messageText.setVisibility(View.GONE)
    }
  }

  def showFileButtons(): Unit = {
    val accept = fileButtons.findViewById(R.id.file_accept_button)
    val reject = fileButtons.findViewById(R.id.file_reject_button)
    val key = msg.key.asInstanceOf[FriendKey]


    accept.asInstanceOf[ImageView].getDrawable.clearColorFilter()
    accept.asInstanceOf[ImageView].setBackgroundColor(Color.TRANSPARENT)

    reject.asInstanceOf[ImageView].getDrawable.clearColorFilter()
    reject.asInstanceOf[ImageView].setBackgroundColor(Color.TRANSPARENT)


    accept.setOnTouchListener(new OnTouchListener() {
      override def onTouch(view: View, event: MotionEvent): Boolean = {
        event.getAction match {
          case MotionEvent.ACTION_DOWN =>

            view.asInstanceOf[ImageView].getDrawable.setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP)
            view.asInstanceOf[ImageView].setBackgroundColor(Color.parseColor("#9ea1a2"))

          case MotionEvent.ACTION_CANCEL | MotionEvent.ACTION_OUTSIDE =>
            view.asInstanceOf[ImageView].getDrawable.clearColorFilter()
            view.asInstanceOf[ImageView].setBackgroundColor(Color.TRANSPARENT)

          case MotionEvent.ACTION_UP =>

            view.asInstanceOf[ImageView].getDrawable.setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP)
            view.asInstanceOf[ImageView].setBackgroundColor(Color.parseColor("#959595"))

          case _ => // do nothing
        }

        false
      }
    })

    accept.setOnClickListener(new View.OnClickListener() {
      override def onClick(view: View) {
        view.asInstanceOf[ImageView].getDrawable.setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP)
        view.asInstanceOf[ImageView].setBackgroundColor(Color.parseColor("#959595"))
        State.transfers.acceptFile(key, msg.messageId, context)
      }
    })

    reject.setOnTouchListener(new OnTouchListener() {
      override def onTouch(view: View, event: MotionEvent): Boolean = {

        event.getAction match {
          case MotionEvent.ACTION_DOWN =>

            view.asInstanceOf[ImageView].getDrawable.setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP)
            view.asInstanceOf[ImageView].setBackgroundColor(Color.parseColor("#9ea1a2"))

          case MotionEvent.ACTION_CANCEL | MotionEvent.ACTION_OUTSIDE =>
            view.asInstanceOf[ImageView].getDrawable.clearColorFilter()
            view.asInstanceOf[ImageView].setBackgroundColor(Color.TRANSPARENT)

          case MotionEvent.ACTION_UP =>

            view.asInstanceOf[ImageView].getDrawable.setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP)
            view.asInstanceOf[ImageView].setBackgroundColor(Color.parseColor("#959595"))

          case _ => // do nothing
        }

        false
      }
    })

    reject.setOnClickListener(new View.OnClickListener() {

      override def onClick(view: View) {
        view.asInstanceOf[ImageView].getDrawable.setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP)
        view.asInstanceOf[ImageView].setBackgroundColor(Color.parseColor("#959595"))
        State.transfers.rejectFile(key, msg.messageId, context)
      }
    })

    fileButtons.setVisibility(View.VISIBLE)
    if (msg.size > 0) {
      fileSize.setText(Formatter.formatFileSize(context, msg.size))
      fileSize.setVisibility(View.VISIBLE)
    }

    progressLayout.setVisibility(View.GONE)
    imageMessage.setVisibility(View.GONE)
  }

  def showProgressBar(): Unit = {
    fileProgressBar.setMax(msg.size)
    fileProgressBar.setVisibility(View.VISIBLE)
    progressLayout.setVisibility(View.VISIBLE)

    if (progressSub == null || progressSub.isUnsubscribed) {
      AntoxLog.debug("observer subscribing", TAG)
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
    val mProgress = State.transfers.getProgressSinceXAgo(msg.id, updateRate)
    val bytesPerSecond = mProgress match {
      case Some(p) => ((p._1 * 1000) / p._2).toInt
      case None => 0
    }

    if (bytesPerSecond != 0) {
      val secondsToComplete = msg.size / bytesPerSecond
      fileProgressText.setText(java.lang.Integer.toString(bytesPerSecond / 1024) + " KiB/s, " +
        context.getResources.getString(R.string.file_time_remaining, secondsToComplete.toString))
    } else {
      fileProgressText.setText(java.lang.Integer.toString(bytesPerSecond / 1024) + " KiB/s")
    }
    fileProgressBar.setProgress(State.transfers.getProgress(msg.id).toInt)
    if (fileProgressBar.getProgress >= msg.size) {
      progressSub.unsubscribe()
      AntoxLog.debug("observer unsubscribed", TAG)
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
    if (msg.isMine) {
      messageText.setTextColor(context.getResources.getColor(R.color.white))
    } else {
      messageText.setTextColor(context.getResources.getColor(R.color.black))
    }
    messageTitle.setVisibility(View.VISIBLE)
    messageText.setVisibility(View.VISIBLE)
  }

  override def toggleReceived(): Unit = {
    // do nothing
  }

  override def onClick(view: View) {
    view match {
      case _: ImageView =>
        val i = new Intent()
        i.setAction(android.content.Intent.ACTION_VIEW)

        val file =
          if (msg.message.contains("/")) {
            new File(msg.message)
          } else {
            val f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
              Constants.DOWNLOAD_DIRECTORY)
            new File(f.getAbsolutePath + "/" + msg.message)
          }

        val extension = getFileExtensionFromUrl(file.getAbsolutePath())
        var mime: String = "image/*"
        if (extension != null) {
          mime = getSingleton().getMimeTypeFromExtension(extension)
        }

        if (mime == null) {
          mime = "image/*"
        }
        System.out.println("file open:3i:" + mime)
        i.setDataAndType(Uri.fromFile(file), mime)
        context.startActivity(i)

      case _ =>
        // open any file by clicking on it
        val file =
          if (msg.message.contains("/")) {
            new File(msg.message)
          } else {
            val f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
              Constants.DOWNLOAD_DIRECTORY)
            new File(f.getAbsolutePath + "/" + msg.message)
          }

        try {

          val extension = getFileExtensionFromUrl(file.getAbsolutePath())
          var mime: String = null
          if (extension != null) {
            mime = getSingleton().getMimeTypeFromExtension(extension)
          }
          val i = new Intent()
          i.setAction(android.content.Intent.ACTION_VIEW)
          System.out.println("file open:3:" + mime)
          i.setDataAndType(Uri.fromFile(file), mime)
          context.startActivity(i)
        }
        catch {
          case e: Exception => e.printStackTrace()
        }

    }
  }

  override def onLongClick(view: View): Boolean = {

    // add selection to cancel filetransfer??

    val items = Array[CharSequence](context.getResources.getString(R.string.message_delete),
      context.getResources.getString(R.string.file_delete), "open file")
    new AlertDialog.Builder(context).setCancelable(true).setItems(items, new DialogInterface.OnClickListener() {

      def onClick(dialog: DialogInterface, index: Int): Unit = index match {
        case 0 =>
          Observable[Boolean](subscriber => {
            val db = State.db
            db.deleteMessage(msg.id)
            subscriber.onCompleted()
          }).subscribeOn(IOScheduler()).subscribe()
        case 1 =>
          Observable[Boolean](subscriber => {
            val db = State.db
            db.deleteMessage(msg.id)

            val file =
              if (msg.message.contains("/")) {
                new File(msg.message)
              } else {
                val f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                  Constants.DOWNLOAD_DIRECTORY)
                new File(f.getAbsolutePath + "/" + msg.message)
              }
            file.delete()

            subscriber.onCompleted()
          }).subscribeOn(IOScheduler()).subscribe()
        case 2 =>
          Observable[Boolean](subscriber => {
            val db = State.db

            val file =
              if (msg.message.contains("/")) {
                new File(msg.message)
              } else {
                val f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                  Constants.DOWNLOAD_DIRECTORY)
                new File(f.getAbsolutePath + "/" + msg.message)
              }

            try {

              val extension = getFileExtensionFromUrl(file.getAbsolutePath())
              var mime: String = null
              if (extension != null) {
                mime = getSingleton().getMimeTypeFromExtension(extension)
              }
              val i = new Intent()
              i.setAction(android.content.Intent.ACTION_VIEW)
              System.out.println("file open:3:" + mime)
              i.setDataAndType(Uri.fromFile(file), mime)
              context.startActivity(i)
            }
            catch {
              case e: Exception => e.printStackTrace()
            }
            subscriber.onCompleted()
          }).subscribeOn(IOScheduler()).subscribe()
      }
    }).create().show()

    true
  }
}