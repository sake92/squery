package ba.sake.squery

import java.{sql => jsql}
import scala.util.Using

import ba.sake.squery.write.SqlArgument

case class Query(
    sql: String,
    arguments: Seq[SqlArgument[?]]
)

object Query {
  def newPreparedStatement(
      q: Query,
      c: jsql.Connection
  ): jsql.PreparedStatement = {
    val stat = c.prepareStatement(q.sql, jsql.Statement.RETURN_GENERATED_KEYS)
    q.arguments.zipWithIndex.foreach { (arg, i) =>
      arg.sqlWrite.write(stat, i + 1, arg.value)
    }
    stat
  }
}
