package ba.sake.squery.read

import java.{sql => jsql}
import java.time.*
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

  given SqlRead[Boolean] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Boolean] =
      Option(jRes.getBoolean(colName)).filterNot(_ => jRes.wasNull())
    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Boolean] =
      Option(jRes.getBoolean(colIdx)).filterNot(_ => jRes.wasNull())
  }

  given SqlRead[Int] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Int] =
      Option(jRes.getInt(colName)).filterNot(_ => jRes.wasNull())
    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Int] =
      Option(jRes.getInt(colIdx)).filterNot(_ => jRes.wasNull())
  }

  given SqlRead[Long] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Long] =
      Option(jRes.getLong(colName)).filterNot(_ => jRes.wasNull())
    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Long] =
      Option(jRes.getLong(colIdx)).filterNot(_ => jRes.wasNull())
  }

  given SqlRead[Double] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Double] =
      Option(jRes.getDouble(colName)).filterNot(_ => jRes.wasNull())
    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Double] =
      Option(jRes.getDouble(colIdx)).filterNot(_ => jRes.wasNull())
  }

  given SqlRead[Instant] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Instant] =
      Option(jRes.getTimestamp(colName)).map(_.toInstant)

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Instant] =
      Option(jRes.getTimestamp(colIdx)).map(_.toInstant)
  }

  given SqlRead[LocalDate] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[LocalDate] =
      Option(jRes.getObject(colName, classOf[LocalDate]))

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[LocalDate] =
      Option(jRes.getObject(colIdx, classOf[LocalDate]))
  }

  given SqlRead[UUID] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[UUID] =
      Option(jRes.getObject(colName, classOf[UUID]))

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[UUID] =
      Option(jRes.getObject(colIdx, classOf[UUID]))

  }

  // this "cannot fail"
  given [T](using sr: SqlRead[T]): SqlRead[Option[T]] = new {
    def readByName(jRes: jsql.ResultSet, colName: String): Option[Option[T]] =
      Some(sr.readByName(jRes, colName))

    def readByIdx(jRes: jsql.ResultSet, colIdx: Int): Option[Option[T]] =
      Some(sr.readByIdx(jRes, colIdx))

  }
