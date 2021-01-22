package chat.tox.antox.tox

/**
  * Trait for Tox interval times
  */
trait Intervals {
  /**
    * Returns how many milliseconds should be used as an interval between tox iterations
    */
  def interval: Int
}

/**
  * An enumeration to store the different possible levels the app can be 'working' at.
  * Enumeration makes it extensible
  */
// scalastyle:off
object IntervalLevels extends Enumeration {
  type IntervalLevels = Value
  //
  // try [0.5 secs / 1 secs]
  val WORKING = Value(500)
  // Orig: Value(50) // only in filetransfers it seems
  val AWAKE = Value(1000) // Orig: Value(1000) // everywhere else
  //
}
