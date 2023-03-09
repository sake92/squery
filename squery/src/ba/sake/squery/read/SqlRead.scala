package ba.sake.squery.read

import java.{sql => jsql}
import java.time.Instant
import java.util.UUID

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

  given SqlRead[Instant] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Instant =
      jRes.getTimestamp(colName).toInstant()

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Instant =
      jRes.getTimestamp(colIdx).toInstant()
  }

  given SqlRead[UUID] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): UUID =
      jRes.getObject(colName, classOf[UUID])

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): UUID =
      jRes.getObject(colIdx, classOf[UUID])

  }
