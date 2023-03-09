package ba.sake.squery

import ba.sake.squery.write.SqlArgument
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.select.Select
import net.sf.jsqlparser.JSQLParserException

/** Implementation of `sql""` interpolator. For a query sql"SELECT .. WHERE $a >
  * 5 AND b = 'abc' ", there have to be ` SqlWrite` typeclass instances for
  * types of $a and $b.
  */
extension (sc: StringContext) {

  def sql(args: SqlArgument[?]*): Query =
    val strings = sc.parts.iterator
    var buf = new StringBuilder(strings.next())
    while strings.hasNext do
      buf.append("?")
      buf.append(strings.next())

    val enrichedQuery = enrichSqlQuery(buf.toString())
    Query(enrichedQuery, args.toSeq)

}

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
