package ba.sake.squery

import java.{sql => jsql}
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.duration.FiniteDuration
import ba.sake.squery.DynamicArg
import ba.sake.squery.parser.SqlSelectAliasParser
import ba.sake.squery.parser.SqlStatementLinter

case class Query(
    private[squery] val sqlString: String,
    private[squery] val arguments: Seq[DynamicArg[?]],
    statementOptions: StatementOptions = StatementOptions()
) {

  private val logger = SqueryLoggerFactory(getClass.getName)

  def ++(other: Query): Query =
    copy(sqlString = sqlString + " " + other.sqlString, arguments = arguments ++ other.arguments)

  def withStatementOptions(options: StatementOptions): Query =
    copy(statementOptions = options)

  def withFetchSize(fetchSize: Int): Query =
    copy(statementOptions = statementOptions.copy(fetchSize = Some(fetchSize)))

  def withTimeout(timeout: FiniteDuration): Query =
    copy(statementOptions = statementOptions.copy(timeout = Some(timeout)))

  def withMaxRows(maxRows: Int): Query =
    copy(statementOptions = statementOptions.copy(maxRows = Some(maxRows)))

  def withResultSet(resultSetType: ResultSetType, concurrency: ResultSetConcurrency): Query =
    copy(
      statementOptions = statementOptions.copy(
        resultSetType = Some(resultSetType),
        resultSetConcurrency = Some(concurrency),
        generatedKeys = None
      )
    )

  def withGeneratedKeys(generatedKeys: GeneratedKeys = GeneratedKeys.All): Query =
    copy(
      statementOptions = statementOptions.copy(
        resultSetType = None,
        resultSetConcurrency = None,
        generatedKeys = Some(generatedKeys)
      )
    )

  private[squery] def newPreparedStatement(
      dbActionType: DbActionType,
      c: SqueryConnection
  ): jsql.PreparedStatement = {
    val enrichedQueryString = Query.enrichSqlQuery(sqlString, dbActionType, c.lintUpdates)
    logger.debug(s"Executing statement: $enrichedQueryString")
    val jdbcConnection = c.underlying
    val stat = statementOptions.generatedKeys match
      case Some(GeneratedKeys.All) =>
        jdbcConnection.prepareStatement(enrichedQueryString, jsql.Statement.RETURN_GENERATED_KEYS)
      case Some(GeneratedKeys.ColumnNames(names)) =>
        jdbcConnection.prepareStatement(enrichedQueryString, names.toArray)
      case Some(GeneratedKeys.ColumnIndexes(indexes)) =>
        jdbcConnection.prepareStatement(enrichedQueryString, indexes.toArray)
      case None =>
        (statementOptions.resultSetType, statementOptions.resultSetConcurrency) match
          case (Some(resultSetType), Some(concurrency)) =>
            jdbcConnection.prepareStatement(enrichedQueryString, resultSetType.jdbcValue, concurrency.jdbcValue)
          case _ => jdbcConnection.prepareStatement(enrichedQueryString)

    statementOptions.fetchSize.foreach(stat.setFetchSize)
    statementOptions.timeout.foreach { timeout =>
      val wholeSeconds = timeout.toSeconds
      val jdbcSeconds =
        if timeout > FiniteDuration(wholeSeconds, java.util.concurrent.TimeUnit.SECONDS) then wholeSeconds + 1
        else wholeSeconds
      stat.setQueryTimeout(jdbcSeconds.toInt)
    }
    statementOptions.maxRows.foreach(stat.setMaxRows)

    arguments.zipWithIndex.foreach { (arg, i) =>
      arg.sqlWrite.write(stat, i + 1, Option(arg.value))
    }

    SqueryJdbcWarnings.log(stat, logger)
    stat
  }

  override def toString: String = sqlString
}

object Query {
  private val logger = SqueryLoggerFactory(getClass.getName)

  private val selectStmtsCache = new ConcurrentHashMap[String, String]()

  private def enrichSqlQuery(query: String, dbActionType: DbActionType, lintUpdates: Boolean): String = {
    logger.trace(s"""Enriching query: $query""")
    val res = getEnrichedSqlQuery(query, dbActionType, lintUpdates)
    logger.trace(s"""Enriched query: $res""")
    res
  }

  // try to avoid parsing, or at least cache the results
  private def getEnrichedSqlQuery(query: String, dbActionType: DbActionType, lintUpdates: Boolean): String =
    if dbActionType == DbActionType.Select || lintUpdates then
      try {
        val cached = selectStmtsCache.get(query)
        if cached != null then return cached
        if dbActionType == DbActionType.Select then
          SqlSelectAliasParser
            .addAliases(query)
            .map(enriched => selectStmtsCache.computeIfAbsent(query, _ => enriched))
            .getOrElse(query)
        else {
          if (lintUpdates && SqlStatementLinter.isUpdateOrDeleteWithoutWhere(query))
            logger.warn(
              s"""There is no WHERE clause in the UPDATE or DELETE statement. This is a dangerous action.
                  Statement: $query"""
            )
          query
        }
      } catch {
        // do nothing if can't parse, db will throw anyways
        case _: RuntimeException =>
          logger.warn(s"""Could not parse query but will run it anyways: $query""")
          query
      }
    else {
      query // no need to do anything
    }

}
