package ba.sake.squery

import java.{sql => jsql}
import scala.util.Using

import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.statement.update.Update
import net.sf.jsqlparser.statement.delete.Delete
import net.sf.jsqlparser.JSQLParserException

import com.typesafe.scalalogging.Logger

import ba.sake.squery.write.SqlArgument

case class Query(
    private[squery] val sqlString: String,
    private[squery] val arguments: Seq[SqlArgument[?]]
) {

  private val logger = Logger(getClass.getName)

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
    // TODO reorder enriched issue..
    // println("enriched: " + enrichedQueryString)
    logger.debug(s"Executing statement: $enrichedQueryString")
    val stat =
      c.prepareStatement(
        enrichedQueryString,
        if retGenKeys then jsql.Statement.RETURN_GENERATED_KEYS
        else jsql.Statement.NO_GENERATED_KEYS
      )
    arguments.zipWithIndex.foreach { (arg, i) =>
      arg.sqlWrite.write(stat, i + 1, Option(arg.value))
    }
    // print warnings if any
    var warning = stat.getWarnings()
    while (warning != null) {
      logger.warn(warning.getMessage())
      warning = warning.getNextWarning()
    }
    stat
  }

  override def toString(): String = sqlString
}

object Query {
  private val logger = Logger(getClass.getName)

  private def enrichSqlQuery(query: String): String =
    logger.trace(s"""Enriching query: $query""")
    val res = doEnrichSqlQuery(query)
    logger.trace(s"""Enriched query: $res""")
    res

  private def doEnrichSqlQuery(query: String): String = try {
    val stmt = CCJSqlParserUtil.parse(query)
    stmt match
      case selectStmt: Select =>
        selectStmt.getSelectBody().accept(SqueryAddAliasesVisitor())
        selectStmt.getSelectBody().toString()
      case updateStmt: Update =>
        if updateStmt.getWhere() == null then
          logger.warn(
            s"""There is no WHERE clause in the UPDATE statement. This is a dangerous action.
              Statement: $query"""
          )
        query
      case deleteStmt: Delete =>
        if deleteStmt.getWhere() == null then
          logger.warn(
            s"""There is no WHERE clause in the DELETE statement. This is a dangerous action.
              Statement: $query"""
          )
        query
      case other => query
  } catch {
    // do nothing if can't parse, db will throw anyways
    case _: JSQLParserException =>
      logger.warn(s"""Could not parse query but will run it anyways: $query""")
      query
  }
}
