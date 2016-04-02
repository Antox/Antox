package chat.tox.antox.utils

import android.Manifest
import android.app.Activity
import android.content.{Intent, Context}
import android.content.pm.PackageManager
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import chat.tox.antox.activities.PermissionActivity
import rx.android.schedulers.AndroidSchedulers
import rx.lang.scala.Observable
import rx.lang.scala.schedulers.{AndroidMainThreadScheduler, NewThreadScheduler}

import scala.util.Random

object AntoxPermissionManager {


  private var checks: Map[Int, Option[Boolean]] = Map()


  def requestPermissionStorage(context: Context, activity: Activity): Observable[Boolean] = {
    requestPermissions(context, activity, Array(Manifest.permission.READ_EXTERNAL_STORAGE,
      Manifest.permission.WRITE_EXTERNAL_STORAGE))
  }

  def requestPermissionCamera(context: Context, activity: Activity): Observable[Boolean] = {
    requestPermissions(context, activity, Array(Manifest.permission.CAMERA))
  }

  def requestPermissionMicrophone(context: Context, activity: Activity): Observable[Boolean] = {
    requestPermissions(context, activity, Array(Manifest.permission.RECORD_AUDIO))
  }

  private def requestPermissions(context: Context, activity: Activity, permissions: Array[String]): Observable[Boolean] ={
    val requestCode = Random.nextInt(10000)
    val obs = Observable[Boolean](subscriber => {
      if(hasPermission(context, permissions)) {
        subscriber.onNext(true)
        subscriber.onCompleted()
      }
      else{
        Looper.prepare()
        val intent = new Intent(context, classOf[PermissionActivity])

        ActivityCompat.requestPermissions(activity, permissions, requestCode)
        checks += (requestCode -> None)
        var count = 0
        while(checks(requestCode).isEmpty){
          //count += 50
          //if(count > 60000){
          //  subscriber.onNext(false)
          //  subscriber.onCompleted()
          //}
          Thread.sleep(200)
        }
        subscriber.onNext(checks(requestCode).get)
        subscriber.onCompleted()
      }
    })
    obs.subscribeOn(NewThreadScheduler())
  }

  def hasPermissionStorage(context: Context): Boolean = {
    hasPermission(context, Array(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE))
  }

  def hasPermissionCamera(context: Context): Boolean = {
    hasPermission(context, Manifest.permission.CAMERA)
  }

  def hasPermissionMicrophone(context: Context): Boolean = {
    hasPermission(context, Manifest.permission.RECORD_AUDIO)
  }

  private def hasPermission(context: Context, permission: String ): Boolean ={
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
  }

  private def hasPermission(context: Context, permissions: Array[String] ): Boolean ={
    for(permission <- permissions){
      if(ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
        return false
    }
    true
  }

  def onRequestPermissionsResult(requestCode: Int, permissions: Array[String], grantResults: Array[Int]): Unit = {
    if(!checks.contains(requestCode)) return

    for(result <- grantResults){
      if(result == PackageManager.PERMISSION_DENIED) {
        checks += (requestCode -> Some(false))
        checks(requestCode).notify()
      }
    }
    checks += (requestCode -> Some(true))
  }

  trait OnPermissionChangeListener extends Activity {
    abstract override def onRequestPermissionsResult(requestCode: Int, permissions: Array[String], grantResults: Array[Int]): Unit = {
      AntoxPermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
  }

}


