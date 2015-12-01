package chat.tox.antox.utils

import java.io.{BufferedReader, File, InputStreamReader, Reader}
import java.net.URL
import java.nio.charset.Charset

import org.json.JSONObject

import scala.io.Source

object JsonReader {

  private def readAll(rd: Reader): String = {
    val sb = new StringBuilder()
    var cp: Int = rd.read()
    while (cp != -1) {
      sb.append(cp.toChar)
      cp = rd.read()
    }
    sb.toString()
  }

  def readFromUrl(url: String): String = {
    val is = new URL(url).openStream()
    try {
      val rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")))
      val jsonText = readAll(rd)
      jsonText
    } catch {
      case e: Exception => {
        AntoxLog.errorException("JsonReader readJsonFromUrl error", e)
        ""
      }
    } finally {
      is.close()
    }
  }

  def readJsonFromFile(file: File): JSONObject = {
    try {
      val source = Source.fromFile(file)
      val jsonText = try source.mkString finally source.close()

      new JSONObject(jsonText)
    } catch {
      case e: Exception => {
        AntoxLog.errorException("JsonReader readJsonFromFile error", e)
        new JSONObject()
      }
    }
  }
}
