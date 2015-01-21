package im.tox.antox.utils

import scala.beans.BeanProperty
//remove if not needed

class DrawerItem(@BeanProperty var label: String, private var resid: Int) {

  def getResId: Int = resid

  def setResId(resid: Int) {
    this.resid = resid
  }

  override def toString: String = label + "\n" + resid
}
