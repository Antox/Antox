package chat.tox.antox.activities

import android.app.Activity
import android.os.Bundle
import chat.tox.antox.utils.AntoxPermissionManager

class PermissionActivity extends Activity {

  override def onRequestPermissionsResult(requestCode: Int, permissions: Array[String], grantResults: Array[Int]): Unit ={
    AntoxPermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

}
