
package chat.tox.antox.transfer

import java.io.{File, FilenameFilter}
import java.util

import android.app.{Activity, AlertDialog, Dialog}
import android.content.DialogInterface
import android.os.Environment
import android.view.{View, ViewGroup}
import android.widget.{ArrayAdapter, ImageView, TextView}
import chat.tox.antox.R
import chat.tox.antox.transfer.FileDialog.{DirectorySelectedListener, FileSelectedListener}
import chat.tox.antox.transfer.ListenerList.FireHandler
import chat.tox.antox.utils.AntoxLog
import org.scaloid.common.LoggerTag

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

object FileDialog {

  val PARENT_DIR_TEXT = ".."

  trait FileSelectedListener {

    def fileSelected(file: File): Unit
  }

  trait DirectorySelectedListener {

    def directorySelected (directory: File): Unit
  }
}

class FileDialog(private val activity: Activity, path: File, selectDirectoryOption: Boolean) {

  private val TAG = LoggerTag(getClass.getSimpleName)

  private var currentPath: File = _

  private val fileListenerList: ListenerList[FileSelectedListener] = new ListenerList[FileDialog.FileSelectedListener]()

  private val dirListenerList: ListenerList[DirectorySelectedListener] = new ListenerList[FileDialog.DirectorySelectedListener]()

  private var fileList: ArrayBuffer[File] = _

  private var fileEndsWith: String = _

  val newPath = if (!path.exists()) {
    Environment.getExternalStorageDirectory
  } else {
    path
  }

  loadFileList(newPath)

  def createFileDialog(): Dialog = {
    var dialog: Dialog = null
    val builder = new AlertDialog.Builder(activity, R.style.AppCompatAlertDialogStyle)
    builder.setTitle(currentPath.getPath)
    if (selectDirectoryOption) {
      builder.setPositiveButton("Select directory", new DialogInterface.OnClickListener() {

        def onClick(dialog: DialogInterface, which: Int) {
          AntoxLog.debug(currentPath.getPath, TAG)
          fireDirectorySelectedEvent(currentPath)
        }
      })
    }

    builder.setAdapter(new FileDialogAdapter(activity, fileList, currentPath), new DialogInterface.OnClickListener() {
      def onClick(dialog: DialogInterface, which: Int) {
        val fileChosen = fileList(which)
        if (fileChosen.isDirectory) {
          loadFileList(fileChosen)
          dialog.cancel()
          dialog.dismiss()
          showDialog()
        } else fireFileSelectedEvent(fileChosen)
      }
    })
    dialog = builder.show()
    dialog
  }

  def addFileListener(listener: FileSelectedListener) {
    fileListenerList.add(listener)
  }

  def addDirectoryListener(listener: DirectorySelectedListener) {
    dirListenerList.add(listener)
  }

  def showDialog() {
    createFileDialog().show()
  }

  private def fireFileSelectedEvent(file: File) {
    fileListenerList.fireEvent(new ListenerList.FireHandler[FileSelectedListener]() {

      def fireEvent(listener: FileSelectedListener) {
        listener.fileSelected(file)
      }
    })
  }

  private def fireDirectorySelectedEvent(directory: File) {
    dirListenerList.fireEvent(new ListenerList.FireHandler[DirectorySelectedListener]() {

      def fireEvent(listener: DirectorySelectedListener) {
        listener.directorySelected(directory)
      }
    })
  }

  private def loadFileList(path: File) {
    this.currentPath = path
    val files = new ArrayBuffer[File]()
    if (path.exists()) {
      //add parent directory to show as '..' at the top of the list
      if (path.getParentFile != null) files += path.getParentFile

      val filenameFilter = new FilenameFilter() {
        def accept(dir: File, filename: String): Boolean = {
          val selection = new File(dir, filename)
          if (!selection.canRead) return false
          if (selectDirectoryOption) selection.isDirectory else {
            val endsWith = if (fileEndsWith != null) filename.toLowerCase.endsWith(fileEndsWith) else true
            endsWith || selection.isDirectory
          }
        }
      }

      val filteredFiles = path.listFiles(filenameFilter).sorted
      for (file <- filteredFiles) {
        files += file
      }
    }

    fileList = files
  }
}

class FileDialogAdapter(activity: Activity, fileList: ArrayBuffer[File], currentPath: File)
  extends ArrayAdapter[File](activity,
    R.layout.file_list_row,
    R.id.file_name,
    fileList.toArray) {

  override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    val (resultView, holder) =
      if (convertView == null) {
        val view = activity.getLayoutInflater.inflate(R.layout.file_list_row, parent, false)

        val viewHolder = new FileViewHolder(view)
        view.setTag(viewHolder)
        (view, viewHolder)
      } else {
        (convertView, convertView.getTag.asInstanceOf[FileViewHolder])
      }

    holder.update(fileList(position), currentPath)
    resultView
  }

  override def getViewTypeCount: Int = 1
}

class FileViewHolder(view: View) {
  val fileNameView = view.findViewById(R.id.file_name).asInstanceOf[TextView]
  val fileImageView = view.findViewById(R.id.file_icon).asInstanceOf[ImageView]

  def update(file: File, currentPath: File): Unit = {
    val fileName = if (file == currentPath.getParentFile) FileDialog.PARENT_DIR_TEXT else file.getName
    fileNameView.setText(fileName)
    fileImageView.setImageResource(
      if (file.isDirectory)
        R.drawable.ic_folder_blue_grey_500_48dp
      else
        R.drawable.ic_insert_drive_file_grey_800_48dp)
  }
}

object ListenerList {

  trait FireHandler[L] {

    def fireEvent(listener: L): Unit
  }
}

class ListenerList[L] {

  private val listenerList: util.List[L] = new util.ArrayList[L]()

  def add(listener: L) {
    listenerList.add(listener)
  }

  def fireEvent(fireHandler: FireHandler[L]) {
    val copy = new util.ArrayList[L](listenerList)
    for (l <- copy) {
      fireHandler.fireEvent(l)
    }
  }
}
