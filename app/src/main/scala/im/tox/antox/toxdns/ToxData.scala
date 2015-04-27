package im.tox.antox.toxdns

import scala.beans.BeanProperty

class ToxData {
  @BeanProperty
  var fileBytes: Array[Byte] = null
  @BeanProperty
  var ID: String = _
}