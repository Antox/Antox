package chat.tox.antox.av

abstract class AudioDevice(var _sampleRate: Int, var _channels: Int) {

  protected var active: Boolean

  // if the device is dirty it will be recreated on the next usage
  protected var dirty: Boolean

  //getters
  protected def sampleRate = _sampleRate

  protected def channels = _channels

  //setters
  protected def sampleRate_=(sampleRate: Int): Unit = {
    _sampleRate = sampleRate
    dirty = true
  }

  protected def channels_=(channels: Int): Unit = {
    _channels = channels
    dirty = true
  }
}
