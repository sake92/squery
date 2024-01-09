package ba.sake.squery.h2

import java.{sql => jsql}
import java.util.UUID
import ba.sake.squery.write.*

given SqlWrite[UUID] with {
  def write(
      ps: jsql.PreparedStatement,
      idx: Int,
      valueOpt: Option[UUID]
  ): Unit = valueOpt match
    case Some(value) => ps.setObject(idx, value)
    case None        => ps.setNull(idx, jsql.Types.OTHER)
}
