package ba.sake.squery.sqlite

import java.{sql => jsql}
import java.time.Instant
import java.util.UUID
import ba.sake.squery.write.*

given SqlWrite[UUID] with
  def write(ps: jsql.PreparedStatement, idx: Int, valueOpt: Option[UUID]): Unit =
    valueOpt match
      case Some(value) => ps.setString(idx, value.toString)
      case None        => ps.setNull(idx, jsql.Types.VARCHAR)

given SqlWrite[Boolean] with
  def write(ps: jsql.PreparedStatement, idx: Int, valueOpt: Option[Boolean]): Unit =
    valueOpt match
      case Some(value) => ps.setInt(idx, if value then 1 else 0)
      case None        => ps.setNull(idx, jsql.Types.INTEGER)

given SqlWrite[Instant] with
  def write(ps: jsql.PreparedStatement, idx: Int, valueOpt: Option[Instant]): Unit =
    valueOpt match
      case Some(value) => ps.setString(idx, value.toString)
      case None        => ps.setNull(idx, jsql.Types.VARCHAR)
