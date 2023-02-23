package ba.sake.squery

import java.{sql => jsql}
import scala.util.Using

// TODO string interpolator !
case class Query(
    sql: String
)

object Query {
  def newPreparedStatement(
      q: Query,
      c: jsql.Connection
  ): jsql.PreparedStatement = {
    val stat = c.prepareStatement(q.sql, jsql.Statement.RETURN_GENERATED_KEYS)
    stat
  }
}
