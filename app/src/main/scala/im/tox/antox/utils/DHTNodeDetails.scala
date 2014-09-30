package im.tox.antox.utils

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.net.URL
import java.nio.charset.Charset
import DHTNodeDetails._
//remove if not needed
import scala.collection.JavaConversions._

object DHTNodeDetails {

  object JsonReader {

    private def readAll(rd: Reader): String = {
      val sb = new StringBuilder()
      var cp: Int = 0
      while ((cp = rd.read()) != -1) {
        sb.append(cp.toChar)
      }
      sb.toString
    }

    def readJsonFromUrl(url: String): JSONObject = {
      val is = new URL(url).openStream()
      try {
        val rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")))
        val jsonText = readAll(rd)
        val json = new JSONObject(jsonText)
        json
      } finally {
        is.close()
      }
    }
  }
}

class DHTNodeDetails(var ctx: Context) extends AsyncTask[Void, Void, Void] {

  val nodeDetails = new Array[String](7)

  val TAG = "DHTNODEDETAILS"

  protected override def doInBackground(params: Void*): Void = {
    try {
      val json = JsonReader.readJsonFromUrl("http://jfk.us.cdn.libtoxcore.so/elizabeth_remote/config/Nodefile.json")
      Log.d(TAG, json.toString)
      val serverArray = json.getJSONArray("servers")
      for (i <- 0 until serverArray.length) {
        val jsonObject = serverArray.getJSONObject(i)
        DhtNode.owner.add(jsonObject.getString("owner"))
        DhtNode.ipv6.add(jsonObject.getString("ipv6"))
        DhtNode.key.add(jsonObject.getString("pubkey"))
        DhtNode.ipv4.add(jsonObject.getString("ipv4"))
        DhtNode.port.add(String.valueOf(jsonObject.getInt("port")))
      }
      Log.d(TAG, "Nodes fetched from online")
    } catch {
      case exp: Exception => {
        Log.d(TAG, "Failed to connect to Tox CDN for nodes")
        DhtNode.ipv4.add("192.254.75.98")
        DhtNode.ipv6.add("2607:5600:284::2")
        DhtNode.owner.add("stqism")
        DhtNode.port.add("33445")
        DhtNode.key.add("951C88B7E75C867418ACDB5D273821372BB5BD652740BCDF623A4FA293E75D2F")
        DhtNode.ipv4.add("144.76.60.215")
        DhtNode.ipv6.add("2a01:4f8:191:64d6::1")
        DhtNode.owner.add("sonofra")
        DhtNode.port.add("33445")
        DhtNode.key.add("04119E835DF3E78BACF0F84235B300546AF8B936F035185E2A8E9E0A67C8924F")
        DhtNode.ipv4.add("37.187.46.132")
        DhtNode.ipv6.add("2001:41d0:0052:0300::0507")
        DhtNode.owner.add("mouseym")
        DhtNode.port.add("33445")
        DhtNode.key.add("A9D98212B3F972BD11DA52BEB0658C326FCCC1BFD49F347F9C2D3D8B61E1B927")
        DhtNode.ipv4.add("37.59.102.176")
        DhtNode.ipv6.add("2001:41d0:51:1:0:0:0:cc")
        DhtNode.owner.add("astonex")
        DhtNode.port.add("33445")
        DhtNode.key.add("B98A2CEAA6C6A2FADC2C3632D284318B60FE5375CCB41EFA081AB67F500C1B0B")
        DhtNode.ipv4.add("54.199.139.199")
        DhtNode.ipv6.add("")
        DhtNode.owner.add("aitjcize")
        DhtNode.port.add("33445")
        DhtNode.key.add("7F9C31FE850E97CEFD4C4591DF93FC757C7C12549DDD55F8EEAECC34FE76C029")
      }
    }
    Log.d(TAG, "DhtNode size: " + DhtNode.ipv4.size)
    null
  }
}
