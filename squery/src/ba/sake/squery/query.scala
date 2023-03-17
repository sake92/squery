package ba.sake.squery

import java.{sql => jsql}
import scala.util.Using

import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.JSQLParserException

import ba.sake.squery.write.SqlArgument

class Query(
    private[squery] val sqlString: String,
    private[squery] val arguments: Seq[SqlArgument[?]]
) {

  def ++(other: Query): Query =
    Query(
      sqlString + " " + other.sqlString,
      arguments ++ other.arguments
    )

  private[squery] def newPreparedStatement(
      c: jsql.Connection,
      retGenKeys: Boolean = false
  ): jsql.PreparedStatement = {
    val enrichedQueryString = Query.enrichSqlQuery(sqlString)
    // TODO slf4j
    // TODO reorder enriched issue..
    // println("enriched: " + enrichedQueryString)
    val stat =
      c.prepareStatement(
        enrichedQueryString,
        if retGenKeys then jsql.Statement.RETURN_GENERATED_KEYS else jsql.Statement.NO_GENERATED_KEYS
      )
    arguments.zipWithIndex.foreach { (arg, i) =>
      arg.sqlWrite.write(stat, i + 1, Option(arg.value))
    }
    stat
  }

  override def toString(): String = sqlString
}

object Query {
  private def enrichSqlQuery(query: String): String = try {
    val stmt = CCJSqlParserUtil.parse(query)
    stmt match
      case selectStmt: Select =>
        selectStmt.getSelectBody().accept(new SqueryAddAliasesVisitor())
        selectStmt.getSelectBody().toString()
      case other => query
  } catch {
    // do nothing if can't parse, db will throw anyways
    case _: JSQLParserException => query
  }
}
