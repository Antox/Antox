package chat.tox.antox.utils

import java.lang.Iterable

import android.content.ContentValues
import android.database.Cursor
import com.squareup.sqlbrite.BriteDatabase
import rx.functions.Func1
import rx.lang.scala.JavaConversions._
import rx.lang.scala.Observable

//wrapper to make sqlbrite expose scala observables
class BriteScalaDatabase(db: BriteDatabase) {

  def newTransaction(): BriteDatabase.Transaction = db.newTransaction()

  def close(): Unit = db.close()

  def createQuery(table: String, sql: String, args: String*): Observable[Cursor] =
    db.createQuery(table, sql, args: _*).mapToOne(new Func1[Cursor, Cursor] {
      override def call(cursor: Cursor): Cursor = cursor
    })

  def createQuery(tables: Iterable[String], sql: String, args: String*): Observable[Cursor] =
    db.createQuery(tables, sql, args: _*).mapToOne(new Func1[Cursor, Cursor] {
      override def call(cursor: Cursor): Cursor = cursor
    })

  def delete(table: String, whereClause: String, whereArgs: String*): Int =
    db.delete(table, whereClause, whereArgs: _*)

  def insert(table: String, values: ContentValues): Long = db.insert(table, values)

  def insert(table: String, values: ContentValues, conflictAlgorithm: Int): Long =
    db.insert(table, values, conflictAlgorithm)

  def query(sql: String, args: String*): Cursor =
    db.query(sql, args: _*)

  def setLoggingEnabled(enabled: Boolean): Unit =
    db.setLoggingEnabled(enabled)

  def update(table: String, values: ContentValues, conflictAlgorithm: Int, whereClause: String, whereArgs: String*): Int =
    db.update(table, values, conflictAlgorithm, whereClause, whereArgs: _*)

  def update(table: String, values: ContentValues, whereClause: String, whereArgs: String*): Int =
    db.update(table, values, whereClause, whereArgs: _*)
}