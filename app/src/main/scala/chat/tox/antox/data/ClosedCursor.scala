package chat.tox.antox.data

import android.database.Cursor

case class ClosedCursor(cursor: Cursor) {

  /**
   * Calls a function with a cursor, then closes it and returns the result of the function.
   * Used to ensure that a cursor is always closed after use.
   *
   * @param func the function that will be called
   * @tparam T the return type of function 'func'
   * @return the result of func
   */
  def use[T](func: Cursor => T): T = {
    try {
       func(cursor)
    } finally {
      if (cursor != null) cursor.close()
    }
  }
}
