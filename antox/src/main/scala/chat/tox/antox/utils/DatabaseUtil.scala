package chat.tox.antox.utils

import android.database.sqlite.SQLiteDatabase

object DatabaseUtil {
  def isColumnInTable(mDb: SQLiteDatabase, table: String, column: String): Boolean = {
    try {
      val cursor = mDb.rawQuery("SELECT * FROM " + table + " LIMIT 0", null)
      val result = cursor.getColumnIndex(column) != -1
      cursor.close()
      result
    } catch {
      case e: Exception => false
    }
  }
}