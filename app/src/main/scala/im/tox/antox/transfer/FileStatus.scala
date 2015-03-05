package im.tox.antox.transfer

object FileStatus extends Enumeration {
  type FileStatus = Value
  val REQUESTSENT, CANCELLED, INPROGRESS, FINISHED, PAUSED = Value
}
