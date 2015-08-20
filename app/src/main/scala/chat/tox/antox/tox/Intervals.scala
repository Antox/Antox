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
  val WORKING = Value(50)
  val AWAKE = Value(1000)
}
