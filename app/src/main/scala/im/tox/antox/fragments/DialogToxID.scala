package im.tox.antox.fragments

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import im.tox.QR.Contents
import im.tox.QR.QRCodeEncode
import im.tox.antox.R
import DialogToxID._
//remove if not needed
import scala.collection.JavaConversions._

object DialogToxID {

  trait DialogToxIDListener {

    def onDialogClick(fragment: DialogFragment): Unit
  }
}

class DialogToxID extends DialogFragment {

  var mListener: DialogToxIDListener = _

  override def onAttach(activity: Activity) {
    super.onAttach(activity)
    mListener = activity.asInstanceOf[DialogToxIDListener]
  }

  override def onCreateDialog(savedInstanceState: Bundle): Dialog = {
    val builder = new AlertDialog.Builder(getActivity)
    val inflater = getActivity.getLayoutInflater
    val view = inflater.inflate(R.layout.dialog_tox_id, null).asInstanceOf[View]
    builder.setView(view)
    builder.setNeutralButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {

      def onClick(dialogInterface: DialogInterface, ID: Int) {
        mListener.onDialogClick(DialogToxID.this)
      }
    })
    var file = new File(Environment.getExternalStorageDirectory.getPath + "/Antox/")
    if (!file.exists()) {
      file.mkdirs()
    }
    val noMedia = new File(Environment.getExternalStorageDirectory.getPath + "/Antox/", ".nomedia")
    if (!noMedia.exists()) {
      try {
        noMedia.createNewFile()
      } catch {
        case e: IOException => e.printStackTrace()
      }
    }
    file = new File(Environment.getExternalStorageDirectory.getPath + "/Antox/userkey_qr.png")
    val pref = PreferenceManager.getDefaultSharedPreferences(getActivity.getApplicationContext)
    generateQR(pref.getString("tox_id", ""))
    val bmp = BitmapFactory.decodeFile(file.getAbsolutePath)
    val qrCode = view.findViewById(R.id.qr_image).asInstanceOf[ImageButton]
    qrCode.setImageBitmap(bmp)
    qrCode.setOnClickListener(new View.OnClickListener() {

      override def onClick(v: View) {
        val shareIntent = new Intent()
        shareIntent.setAction(Intent.ACTION_SEND)
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(Environment.getExternalStorageDirectory.getPath + "/Antox/userkey_qr.png")))
        shareIntent.setType("image/jpeg")
        view.getContext.startActivity(Intent.createChooser(shareIntent, getResources.getString(R.string.share_with)))
      }
    })
    builder.create()
  }

  private def generateQR(userKey: String) {
    val qrData = "tox:" + userKey
    val qrCodeSize = 400
    val qrCodeEncoder = new QRCodeEncode(qrData, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString, 
      qrCodeSize)
    var out: FileOutputStream = null
    try {
      val bitmap = qrCodeEncoder.encodeAsBitmap()
      out = new FileOutputStream(Environment.getExternalStorageDirectory.getPath + "/Antox/userkey_qr.png")
      bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
      out.close()
    } catch {
      case e: WriterException => e.printStackTrace()
      case e: FileNotFoundException => e.printStackTrace()
      case e: IOException => e.printStackTrace()
    }
  }
}
