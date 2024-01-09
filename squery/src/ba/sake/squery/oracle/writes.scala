package ba.sake.squery.oracle

import java.{sql => jsql}
import java.util.UUID
import ba.sake.squery.write.*

given SqlWrite[UUID] with {
  def write(
      ps: jsql.PreparedStatement,
      idx: Int,
      valueOpt: Option[UUID]
  ): Unit = valueOpt match
    case Some(value) => ps.setString(idx, value.toString)
    case None        => ps.setString(idx, null)
}
