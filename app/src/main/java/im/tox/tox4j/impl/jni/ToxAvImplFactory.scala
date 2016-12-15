package im.tox.tox4j.impl.jni

import im.tox.tox4j.av.{ ToxAv, ToxAvFactory }
import im.tox.tox4j.core.ToxCore

object ToxAvImplFactory extends ToxAvFactory {

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  private def make(tox: ToxCore): ToxAv = {
    new ToxAvImpl(tox.asInstanceOf[ToxCoreImpl])
  }

  def withToxAv[R](tox: ToxCore)(f: ToxAv => R): R = {
    withToxAv(make(tox))(f)
  }

}
