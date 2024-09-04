package ba.sake.squery.postgres.jawn

import java.{sql => jsql}
import org.typelevel.jawn.ast.*
import ba.sake.squery.write.*

given SqlWrite[JValue] with {
  def write(
      ps: jsql.PreparedStatement,
      idx: Int,
      valueOpt: Option[JValue]
  ): Unit = valueOpt match
    case Some(value) => ps.setObject(idx, FastRenderer.render(value), jsql.Types.OTHER)
    case None        => ps.setNull(idx, jsql.Types.OTHER)
}
