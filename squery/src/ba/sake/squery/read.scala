package ba.sake.squery

import java.{sql => jsql}
import scala.util.Using

// reads a value from a column
trait SqlRead[T]:
  def readByName(jRes: jsql.ResultSet, colName: String): T
  def readByIdx(jRes: jsql.ResultSet, colIdx: Int): T

object SqlRead:
  def apply[T](using sqlRead: SqlRead[T]): SqlRead[T] = sqlRead

  given SqlRead[String] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): String =
      jRes.getString(colName)
    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): String =
      jRes.getString(colIdx)
  }

  given SqlRead[Int] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Int =
      jRes.getInt(colName)
    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Int =
      jRes.getInt(colIdx)
  }

// TODO typeclassss
// reads a row (or just part of it if used in composition)
trait SqlReadRow[T]:
  def readRow(jRes: jsql.ResultSet, prefix: Option[String]): T

object SqlReadRow:
  def apply[T](using sqlReadRow: SqlReadRow[T]): SqlReadRow[T] = sqlReadRow



object read {

  def apply[A](query: Query)(using c: Connection, r: SqlRead[A]): List[A] = {
    val elems = collection.mutable.ListBuffer.empty[A]
    Using.resource(Query.newPreparedStatement(query, c.underlying)) { stmt =>
      Using.resource(stmt.executeQuery()) { res =>
        while (res.next()) {
          elems += r.readByIdx(res, 1)
        }
        elems.result()
      }
    }
  }

  def rows[A](query: Query)(using c: Connection, r: SqlReadRow[A]): List[A] = {
    val elems = collection.mutable.ListBuffer.empty[A]
    Using.resource(Query.newPreparedStatement(query, c.underlying)) { stmt =>
      Using.resource(stmt.executeQuery()) { res =>
        while (res.next()) {
          elems += r.readRow(res, None)
        }
        elems.result()
      }
    }
  }
}




