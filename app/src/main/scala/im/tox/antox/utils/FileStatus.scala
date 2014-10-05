package im.tox.antox.utils

object FileStatus extends Enumeration {
  type FileStatus = Value
  val REQUESTSENT, CANCELLED, INPROGRESS, FINISHED, PAUSED = Value
}
