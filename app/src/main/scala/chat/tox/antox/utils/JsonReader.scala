package chat.tox.antox.utils

import java.io.{BufferedReader, File, InputStreamReader, Reader}
import java.net.URL
import java.nio.charset.Charset

import org.json.JSONObject

import scala.io.Source
import scala.util.Try

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

  def readFromUrl(url: String): Option[String] = {
    Try(new URL(url).openStream()).map(is => {
      try {
        val rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")))
        val jsonText = readAll(rd)
        Some(jsonText)
      } catch {
        case e: Exception => {
          AntoxLog.errorException("JsonReader readJsonFromUrl error", e)
          None
        }
      } finally {
        is.close()
      }
    }).toOption.flatten
  }

  def readJsonFromFile(file: File): Option[JSONObject] = {
    try {
      val source = Source.fromFile(file)
      val jsonText = try source.mkString finally source.close()

      Some(new JSONObject(jsonText))
    } catch {
      case _: Exception =>
        AntoxLog.error("JSON file not found")
        None
    }
  }
}
