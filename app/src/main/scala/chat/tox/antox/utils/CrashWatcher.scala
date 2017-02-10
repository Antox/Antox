package chat.tox.antox.utils

import java.io.{PrintWriter, StringWriter, _}
import java.text.SimpleDateFormat
import java.util
import java.util.{Calendar, Scanner}

import android.app.ActivityManager
import android.content.{ComponentName, Context, Intent}
import android.os.{Build, Environment}
import android.preference.PreferenceManager
import android.util.Log
import chat.tox.antox.CrashActivity
import chat.tox.antox.data.State


object CrashWatcher {

  private var last_stack_trace_as_string: String = ""
  private val i: Int = 0
  private var crashes: Int = 0
  private var last_crash_time: Long = 0L
  private var prevlast_crash_time: Long = 0L
  private var randnum: Int = -1


  def setup(ctx: Context): Unit ={
    randnum = (Math.random * 1000d).toInt
    crashes = PreferenceManager.getDefaultSharedPreferences(ctx).getInt("crashes", 0)

    if (crashes > 10000) {
      crashes = 0
      PreferenceManager.getDefaultSharedPreferences(ctx).edit.putInt("crashes", crashes).commit
    }

    last_crash_time = PreferenceManager.getDefaultSharedPreferences(ctx).getLong("last_crash_time", 0)
    prevlast_crash_time = PreferenceManager.getDefaultSharedPreferences(ctx).getLong("prevlast_crash_time", 0)
    Thread.setDefaultUncaughtExceptionHandler((thread: Thread, e: Throwable) => {
      handleUncaughtException(thread, e, ctx)
    })
  }

  @throws[IOException]
  private def saveErrorMessage(): Unit ={
    val log_detailed: String = grabLogcat
    try {
      val c: Calendar = Calendar.getInstance
      val df: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss")
      val formattedDate: String = df.format(c.getTime)
      val myDir: File = new File(Environment.getExternalStorageDirectory.getAbsolutePath + "/Antox")
      myDir.mkdirs
      val myFile: File = new File(myDir.getAbsolutePath + "/crash_" + formattedDate + ".txt")
      myFile.createNewFile
      val fOut: FileOutputStream = new FileOutputStream(myFile)
      val myOutWriter: OutputStreamWriter = new OutputStreamWriter(fOut)
      myOutWriter.append("Errormesage:\n" + last_stack_trace_as_string + "\n\n===================================\n\n" + log_detailed)
      myOutWriter.close()
      fOut.close()
    }
    catch {
      case e: Exception =>

    }
  }

  private def handleUncaughtException(thread: Thread, e: Throwable, ctx: Context): Unit ={
    last_stack_trace_as_string = e.getMessage
    var stack_trace_ok: Boolean = false
    try {
      val writer: Writer = new StringWriter
      val printWriter: PrintWriter = new PrintWriter(writer)
      e.printStackTrace(printWriter)
      last_stack_trace_as_string = writer.toString
      stack_trace_ok = true
    }
    catch {
      case ee: Exception =>

      case ex2: OutOfMemoryError =>

    }
    if (!stack_trace_ok) {
      try {
        last_stack_trace_as_string = Log.getStackTraceString(e)
        stack_trace_ok = true
      }
      catch {
        case ee: Exception =>

        case ex2: OutOfMemoryError =>

      }
    }
    crashes += 1
    PreferenceManager.getDefaultSharedPreferences(ctx).edit.putInt("crashes", crashes).commit
    try {
      saveErrorMessage()
    }
    catch {
      case ee: Exception =>

      case ex2: OutOfMemoryError =>

    }
    try {
      State.shutdown(ctx)
    }
    catch {
      case e2: Exception =>
        e2.printStackTrace()

    }
    val am: ActivityManager = ctx.getSystemService(Context.ACTIVITY_SERVICE).asInstanceOf[ActivityManager]
    val taskInfo: util.List[ActivityManager.RunningTaskInfo] = am.getRunningTasks(1)
    val componentInfo: ComponentName = taskInfo.get(0).topActivity
    try {
      State.MainToxService.onDestroy()
    }
    catch {
      case e3: Exception =>
        e3.printStackTrace()
    }
    val intent: Intent = new Intent(ctx, classOf[CrashActivity])
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
    ctx.startActivity(intent)
    android.os.Process.killProcess(android.os.Process.myPid)
    System.exit(2)
  }

  private def grabLogcat: String = {
    try {
      val process: Process = Runtime.getRuntime.exec("logcat -d -v threadtime")
      val scanner = new Scanner(process.getInputStream)
      val log: StringBuilder = new StringBuilder
      val separator: String = System.getProperty("line.separator")
      while (scanner.hasNextLine) {
        {
          log.append(scanner.nextLine())
          log.append(separator)
        }
      }
      scanner.close()
      if ((log.length < 100) || (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)) {
        val process2: Process = Runtime.getRuntime.exec("logcat -d")
        val scanner = new Scanner(process2.getInputStream)
        val log2: StringBuilder = new StringBuilder
        var line2: String = null
        while (scanner.hasNextLine) {
          {
            log2.append(scanner.nextLine())
            log2.append(separator)
          }
        }
        scanner.close()

        log2.toString
      }
      else {
        log.toString
      }
    }
    catch {
      case ioe: IOException =>
        ioe.printStackTrace()
        null

      case e: Exception =>
        e.printStackTrace()
        null

    }
  }
}
