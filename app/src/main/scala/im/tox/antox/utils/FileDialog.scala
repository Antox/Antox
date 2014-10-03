
package im.tox.antox.utils

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FilenameFilter
import java.util.ArrayList
import java.util.List
import FileDialog._
import ListenerList._
//remove if not needed
import scala.collection.JavaConversions._

object FileDialog {

  private val PARENT_DIR = ".."

  trait FileSelectedListener {

    def fileSelected(file: File): Unit
  }

  trait DirectorySelectedListener {

    def directorySelected(directory: File): Unit
  }
}

class FileDialog(private val activity: Activity, path: File) {

  private val TAG = getClass.getName

  private var fileList: Array[String] = _

  private var currentPath: File = _

  private var fileListenerList: ListenerList[FileSelectedListener] = new ListenerList[FileDialog.FileSelectedListener]()

  private var dirListenerList: ListenerList[DirectorySelectedListener] = new ListenerList[FileDialog.DirectorySelectedListener]()

  private var selectDirectoryOption: Boolean = _

  private var fileEndsWith: String = _

  val newPath = if (!path.exists()) {
    Environment.getExternalStorageDirectory
  } else {
    path
  }

  loadFileList(newPath)

  def createFileDialog(): Dialog = {
    var dialog: Dialog = null
    val builder = new AlertDialog.Builder(activity)
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
    val r = new ArrayList[CharSequence]()
    if (path.exists()) {
      if (path.getParentFile != null) r.add(PARENT_DIR)
      val filter = new FilenameFilter() {

        def accept(dir: File, filename: String): Boolean = {
          var sel = new File(dir, filename)
          if (!sel.canRead()) return false
          if (selectDirectoryOption) return sel.isDirectory else {
            var endsWith = if (fileEndsWith != null) filename.toLowerCase().endsWith(fileEndsWith) else true
            return endsWith || sel.isDirectory
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

  private var listenerList: List[L] = new ArrayList[L]()

  def add(listener: L) {
    listenerList.add(listener)
  }

  def fireEvent(fireHandler: FireHandler[L]) {
    val copy = new ArrayList[L](listenerList)
    for (l <- copy) {
      fireHandler.fireEvent(l)
    }
  }
}
