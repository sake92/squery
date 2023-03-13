package ba.sake.squery.write

import java.{sql => jsql}
import java.time.Instant
import java.sql.Timestamp
import java.util.UUID

trait SqlWrite[T]:
  def write(ps: jsql.PreparedStatement, idx: Int, value: T): Unit

object SqlWrite:
  def apply[T](using sqlWrite: SqlWrite[T]): SqlWrite[T] = sqlWrite

  given SqlWrite[String] = new {
    def write(ps: jsql.PreparedStatement, idx: Int, value: String): Unit =
      ps.setString(idx, value)
  }

  given SqlWrite[Int] = new {
    def write(ps: jsql.PreparedStatement, idx: Int, value: Int): Unit =
      ps.setInt(idx, value)
  }

  given SqlWrite[Instant] = new {
    def write(ps: jsql.PreparedStatement, idx: Int, value: Instant): Unit =
      ps.setTimestamp(idx, Timestamp.from(value))
  }

  given SqlWrite[UUID] = new {
    def write(ps: jsql.PreparedStatement, idx: Int, value: UUID): Unit =
      ps.setObject(idx, value)
  }
