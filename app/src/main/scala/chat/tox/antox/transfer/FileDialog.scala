
package chat.tox.antox.transfer

import java.io.{File, FilenameFilter}
import java.util

import android.app.{Activity, AlertDialog, Dialog}
import android.content.DialogInterface
import android.os.Environment
import android.util.Log
import chat.tox.antox.R
import chat.tox.antox.transfer.FileDialog.{DirectorySelectedListener, FileSelectedListener}
import chat.tox.antox.transfer.ListenerList.FireHandler

import scala.collection.JavaConversions._

object FileDialog {

  trait FileSelectedListener {

    def fileSelected(file: File): Unit
  }

  trait DirectorySelectedListener {

    def directorySelected(directory: File): Unit
  }
}

class FileDialog(private val activity: Activity, path: File, selectDirectoryOption: Boolean) {

  private val TAG = this.getClass.getSimpleName

  private val PARENT_DIR = ".."

  private var fileList: Array[String] = _

  private var currentPath: File = _

  private val fileListenerList: ListenerList[FileSelectedListener] = new ListenerList[FileDialog.FileSelectedListener]()

  private val dirListenerList: ListenerList[DirectorySelectedListener] = new ListenerList[FileDialog.DirectorySelectedListener]()

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
          Log.d(TAG, currentPath.getPath)
          fireDirectorySelectedEvent(currentPath)
        }
      })
    }
    builder.setItems(fileList.map(x => x: CharSequence), new DialogInterface.OnClickListener() {

      def onClick(dialog: DialogInterface, which: Int) {
        val fileChosen = fileList(which)
        val chosenFile = getChosenFile(fileChosen)
        if (chosenFile.isDirectory) {
          loadFileList(chosenFile)
          dialog.cancel()
          dialog.dismiss()
          showDialog()
        } else fireFileSelectedEvent(chosenFile)
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
    val r = new util.ArrayList[CharSequence]()
    if (path.exists()) {
      if (path.getParentFile != null) r.add(PARENT_DIR)
      val filter = new FilenameFilter() {

        def accept(dir: File, filename: String): Boolean = {
          var sel = new File(dir, filename)
          if (!sel.canRead) return false
          if (selectDirectoryOption) sel.isDirectory else {
            var endsWith = if (fileEndsWith != null) filename.toLowerCase.endsWith(fileEndsWith) else true
            endsWith || sel.isDirectory
          }
        }
      }
      val fileList1 = path.list(filter)
      for (file <- fileList1) {
        r.add(file)
      }
    }
    fileList = r.toArray(Array[String]())
    fileList.map(x => x: CharSequence)
  }

  private def getChosenFile(fileChosen: String): File = {
    if (fileChosen == PARENT_DIR) currentPath.getParentFile else new File(currentPath, fileChosen)
  }
}

object ListenerList {

  trait FireHandler[L] {

    def fireEvent(listener: L): Unit
  }
}

class ListenerList[L] {

  private var listenerList: util.List[L] = new util.ArrayList[L]()

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
