package chat.tox.antox.utils

import java.lang.Iterable

import android.content.ContentValues
import chat.tox.antox.data.ClosedCursor
import com.squareup.sqlbrite.BriteDatabase
import rx.lang.scala.JavaConversions._
import rx.lang.scala.Observable

//wrapper to make sqlbrite expose scala observables
class BriteScalaDatabase(db: BriteDatabase) {

  def newTransaction(): BriteDatabase.Transaction = db.newTransaction()

  def close(): Unit = db.close()

  def createQuery(table: String, sql: String, args: String*): Observable[ClosedCursor] = {
    val observable = db.createQuery(table, sql, args: _*).asObservable()
    toScalaObservable(observable).map(query => ClosedCursor(query.run()))
  }

  def createQuery(tables: Iterable[String], sql: String, args: String*): Observable[ClosedCursor] = {
    val observable = db.createQuery(tables, sql, args: _*).asObservable()
    toScalaObservable(observable).map(query => ClosedCursor(query.run()))
  }

  def delete(table: String, whereClause: String, whereArgs: String*): Int =
    db.delete(table, whereClause, whereArgs: _*)

  def insert(table: String, values: ContentValues): Long = db.insert(table, values)

  def insert(table: String, values: ContentValues, conflictAlgorithm: Int): Long =
    db.insert(table, values, conflictAlgorithm)

  def query(sql: String, args: String*): ClosedCursor = {
    ClosedCursor(db.query(sql, args: _*))
  }

  def setLoggingEnabled(enabled: Boolean): Unit =
    db.setLoggingEnabled(enabled)

  def update(table: String, values: ContentValues, conflictAlgorithm: Int, whereClause: String, whereArgs: String*): Int =
    db.update(table, values, conflictAlgorithm, whereClause, whereArgs: _*)

  def update(table: String, values: ContentValues, whereClause: String, whereArgs: String*): Int =
    db.update(table, values, whereClause, whereArgs: _*)
}