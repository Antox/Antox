package chat.tox.antox.av

trait CallEnhancement {

  /**
    * Called when the call ends.
    */
  def onRemove(): Unit
}
