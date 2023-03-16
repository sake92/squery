package ba.sake.squery

import java.{sql => jsql}
import scala.util.Using

import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.JSQLParserException

import ba.sake.squery.write.SqlArgument

// TODO normal class, package private fields..
case class Query(
    sqlString: String,
    arguments: Seq[SqlArgument[?]]
) {

  def ++(other: Query): Query =
    Query(
      sqlString + " " + other.sqlString,
      arguments ++ other.arguments
    )

  private[squery] def newPreparedStatement(
      c: jsql.Connection
  ): jsql.PreparedStatement = {
    val enrichedQueryString = Query.enrichSqlQuery(sqlString)
    println("enriched: " + enrichedQueryString)
    val stat =
      c.prepareStatement(
        enrichedQueryString,
        jsql.Statement.RETURN_GENERATED_KEYS
      )
    arguments.zipWithIndex.foreach { (arg, i) =>
      arg.sqlWrite.write(stat, i + 1, arg.value)
    }
    stat
  }
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
