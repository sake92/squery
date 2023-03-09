package ba.sake.squery

import java.{sql => jsql}
import scala.util.Using

import ba.sake.squery.write.SqlArgument

case class Query(
    sqlString: String,
    arguments: Seq[SqlArgument[?]]
) {
  def newPreparedStatement(
      c: jsql.Connection
  ): jsql.PreparedStatement = {
    val stat =
      c.prepareStatement(sqlString, jsql.Statement.RETURN_GENERATED_KEYS)
    arguments.zipWithIndex.foreach { (arg, i) =>
      arg.sqlWrite.write(stat, i + 1, arg.value)
    }
    stat
  }
}
