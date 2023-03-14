package ba.sake.squery.read

import java.{sql => jsql}
import java.time.Instant
import java.util.UUID

// reads a value from a column
trait SqlRead[T]:
  def readByName(jRes: jsql.ResultSet, colName: String): Option[T]
  def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[T]

object SqlRead:
  def apply[T](using sqlRead: SqlRead[T]): SqlRead[T] = sqlRead

  given SqlRead[String] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[String] =
      Option(jRes.getString(colName))
    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[String] =
      Option(jRes.getString(colIdx))
  }

  given SqlRead[Int] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Int] =
      Option(jRes.getInt(colName))
    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Int] =
      Option(jRes.getInt(colIdx))
  }

  given SqlRead[Instant] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Instant] =
      Option(jRes.getTimestamp(colName)).map(_.toInstant)

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Instant] =
      Option(jRes.getTimestamp(colIdx)).map(_.toInstant)
  }

  given SqlRead[UUID] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[UUID] =
      Option(jRes.getObject(colName, classOf[UUID]))

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[UUID] =
      Option(jRes.getObject(colIdx, classOf[UUID]))

  }

  given [T](using sr: SqlRead[T]): SqlRead[Option[T]] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Option[T]] =
      Some(sr.readByName(jRes, colName))

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Option[T]] =
      Some(sr.readByIdx(jRes, colIdx))

  }
