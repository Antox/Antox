package chat.tox.antox.viewholders

import java.io.File

import android.app.AlertDialog
import android.content._
import android.graphics.{Color, PorterDuff}
import android.net.Uri
import android.os.Environment
import android.text.format.Formatter
import android.view.View.{OnClickListener, OnLongClickListener, OnTouchListener}
import android.view.{MotionEvent, View}
import android.webkit.MimeTypeMap._
import android.widget._
import chat.tox.antox.R
import chat.tox.antox.data.State
import chat.tox.antox.tox.ToxSingleton
import chat.tox.antox.transfer.{FileStatus, FileTransfer}
import chat.tox.antox.utils.{AntoxLog, BitmapManager, Constants}
import chat.tox.antox.wrapper.FriendKey
import im.tox.tox4j.core.enums.ToxFileControl
import org.scaloid.common.LoggerTag
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, IOScheduler}
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.duration._

class FileMessageHolder(val view: View) extends GenericMessageHolder(view) with OnClickListener with OnLongClickListener {

  private val TAG = LoggerTag(getClass.getSimpleName)

  protected val imageMessage = view.findViewById(R.id.message_sent_photo).asInstanceOf[ImageView]

  protected val fileButtons = view.findViewById(R.id.file_buttons).asInstanceOf[LinearLayout]

  protected val filetransfer_cancel_button_view = view.findViewById(R.id.filetransfer_cancel_button_view).asInstanceOf[LinearLayout]

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

  def hideCancelButton() = {
    filetransfer_cancel_button_view.setVisibility(View.GONE)
  }


  def setImage(file: File, isImage: Boolean): Unit = {


    try {
    }
    catch {
      case e: Exception => {
        e.printStackTrace()
      }
    }

    this.file = file

    if (file.length > 0) {
      fileSize.setText(Formatter.formatFileSize(context, file.length))
      fileSize.setVisibility(View.VISIBLE)
    }
    else {
      fileSize.setVisibility(View.GONE)
    }

    if (isImage) {
      imageLoadingSub.foreach(_.unsubscribe())

      // Start a loading indicator in case the bitmap needs to be loaded from disk
      imageMessage.setImageBitmap(null)
      imageLoading.setVisibility(View.VISIBLE)

      imageLoadingSub = Some(BitmapManager.load(file, isAvatar = false).subscribe(image => {
        imageLoading.setVisibility(View.GONE)
        imageMessage.setImageBitmap(image)
      }))
    }
    else {
      imageLoading.setVisibility(View.GONE)
      imageMessage.setScaleType(ImageView.ScaleType.CENTER_INSIDE)
      imageMessage.setImageResource(R.drawable.ic_action_attachment_2)
    }

    imageMessage.setOnClickListener(this)
    imageMessage.setOnLongClickListener(this)
    imageMessage.setVisibility(View.VISIBLE)
  }

  def showCancelButton(): Unit = {
    val cancel = filetransfer_cancel_button_view.findViewById(R.id.file_cancel_button)

    cancel.asInstanceOf[ImageView].getDrawable.clearColorFilter()
    cancel.asInstanceOf[ImageView].setBackgroundColor(Color.TRANSPARENT)

    cancel.setOnTouchListener(new OnTouchListener() {
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

    cancel.setOnClickListener(new View.OnClickListener() {

      override def onClick(view: View) {
        view.asInstanceOf[ImageView].getDrawable.setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP)
        view.asInstanceOf[ImageView].setBackgroundColor(Color.parseColor("#959595"))

        val key = msg.key.asInstanceOf[FriendKey]
        var thisFt: FileTransfer = null
        try {
          thisFt = State.transfers.get(key, msg.messageId).get
        }
        catch {
          case e: Exception => // e.printStackTrace()
        }
        if ((thisFt != null) && ((thisFt.status == FileStatus.IN_PROGRESS) || (thisFt.status == FileStatus.PAUSED))) {
          hideCancelButton()
          ToxSingleton.tox.fileControl(key, msg.messageId, ToxFileControl.CANCEL)
          State.transfers.cancelFile(key, msg.messageId, context)
        }
      }
    })

    filetransfer_cancel_button_view.setVisibility(View.VISIBLE)
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
    fileProgressText.setVisibility(View.VISIBLE)

    showCancelButton()

    if (progressSub == null || progressSub.isUnsubscribed) {
      AntoxLog.debug("observer subscribing", TAG)
      progressSub = Observable.interval(1000 milliseconds)
        .observeOn(AndroidMainThreadScheduler())
        .subscribe(x => {

          updateProgressBar()
          State.setLastFileTransferAction()
        })
    }

    imageMessage.setVisibility(View.GONE)
    fileButtons.setVisibility(View.GONE)
  }

  def updateProgressBar(): Unit = {
    val updateRate = 100
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

    try {
      progressSub.unsubscribe()
      AntoxLog.debug("observer unsubscribed", TAG)
    }
    catch {
      case e: Exception => e.printStackTrace()
    }

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
          i.setDataAndType(Uri.fromFile(file), mime)
          context.startActivity(i)
        }
        catch {
          case e: Exception => e.printStackTrace()
        }

    }
  }

  override def onLongClick(view: View): Boolean = {

    val key = msg.key.asInstanceOf[FriendKey]
    var thisFt: FileTransfer = null
    try {
      thisFt = State.transfers.get(key, msg.messageId).get
    }
    catch {
      case e: Exception => // e.printStackTrace()
    }
    var items: Array[CharSequence] = null
    if ((thisFt != null) && ((thisFt.status == FileStatus.IN_PROGRESS) || (thisFt.status == FileStatus.PAUSED))) {
      items = Array[CharSequence](context.getResources.getString(R.string.message_delete),
        context.getResources.getString(R.string.file_delete), context.getResources.getString(R.string.file_open),
        context.getResources.getString(R.string.filetransfer_cancel))
    }
    else {
      items = Array[CharSequence](context.getResources.getString(R.string.message_delete),
        context.getResources.getString(R.string.file_delete), context.getResources.getString(R.string.file_open))
    }

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
              i.setDataAndType(Uri.fromFile(file), mime)
              context.startActivity(i)
            }
            catch {
              case e: Exception => e.printStackTrace()
            }
            subscriber.onCompleted()
          }).subscribeOn(IOScheduler()).subscribe()
        case 3 =>
          Observable[Boolean](subscriber => {
            ToxSingleton.tox.fileControl(key, msg.messageId, ToxFileControl.CANCEL)
            State.transfers.cancelFile(key, msg.messageId, context)
            hideCancelButton()
            subscriber.onCompleted()
          }).subscribeOn(IOScheduler()).subscribe()
      }
    }
    ).create().show()

    true
  }

}