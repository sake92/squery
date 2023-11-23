package ba.sake.squery.write

import java.{sql => jsql}
import java.time.Instant
import java.sql.Timestamp
import java.util.UUID
import java.sql.JDBCType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.OffsetDateTime

trait SqlWrite[T]:
  def write(ps: jsql.PreparedStatement, idx: Int, valueOpt: Option[T]): Unit

object SqlWrite:

  def apply[T](using sqlWrite: SqlWrite[T]): SqlWrite[T] = sqlWrite

  given SqlWrite[String] = new {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[String]
    ): Unit = valueOpt match
      case Some(value) => ps.setString(idx, value)
      case None        => ps.setString(idx, null)
  }

  given SqlWrite[Boolean] = new {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[Boolean]
    ): Unit = valueOpt match
      case Some(value) => ps.setBoolean(idx, value)
      case None        => ps.setNull(idx, jsql.Types.BOOLEAN)
  }

  given SqlWrite[Int] = new {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[Int]
    ): Unit = valueOpt match
      case Some(value) => ps.setInt(idx, value)
      case None        => ps.setNull(idx, jsql.Types.INTEGER)
  }

  given SqlWrite[Long] = new {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[Long]
    ): Unit = valueOpt match
      case Some(value) => ps.setLong(idx, value)
      case None        => ps.setNull(idx, jsql.Types.BIGINT)
  }

  given SqlWrite[Double] = new {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[Double]
    ): Unit = valueOpt match
      case Some(value) => ps.setDouble(idx, value)
      case None        => ps.setNull(idx, jsql.Types.DOUBLE)
  }

  given SqlWrite[Instant] = new {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[Instant]
    ): Unit = valueOpt match
      case Some(value) => ps.setTimestamp(idx, Timestamp.from(value))
      case None        => ps.setNull(idx, jsql.Types.TIMESTAMP_WITH_TIMEZONE)
  }

  given SqlWrite[OffsetDateTime] = new {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[OffsetDateTime]
    ): Unit = valueOpt match
      case Some(value) => ps.setObject(idx, value)
      case None        => ps.setNull(idx, jsql.Types.TIMESTAMP_WITH_TIMEZONE)
  }

  given SqlWrite[LocalDate] = new {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[LocalDate]
    ): Unit = valueOpt match
      case Some(value) => ps.setDate(idx, java.sql.Date.valueOf(value))
      case None        => ps.setNull(idx, jsql.Types.DATE)
  }

  given SqlWrite[LocalDateTime] = new {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[LocalDateTime]
    ): Unit = valueOpt match
      case Some(value) =>
        ps.setDate(
          idx,
          java.sql.Date(
            value
              .atZone(ZoneId.systemDefault())
              .toInstant()
              .toEpochMilli()
          )
        )
      case None => ps.setNull(idx, jsql.Types.TIMESTAMP)
  }

  given SqlWrite[UUID] = new {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        valueOpt: Option[UUID]
    ): Unit = valueOpt match
      case Some(value) => ps.setObject(idx, value)
      case None        => ps.setNull(idx, jsql.Types.OTHER)
  }

  given [T](using sw: SqlWrite[T]): SqlWrite[Option[T]] = new {
    def write(
        ps: jsql.PreparedStatement,
        idx: Int,
        value: Option[Option[T]]
    ): Unit =
      sw.write(ps, idx, value.flatten)
  }
