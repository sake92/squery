package ba.sake.squery

import ba.sake.squery.write.SqlArgument
import scala.collection.mutable.ListBuffer

// TODO interpolate Seq[SqlArgument] ?

// arg can be a simple value
// or another query
type SqlInterpolatorArg = SqlArgument[?] | Query

/** Implementation of `sql""` interpolator. For a query sql"SELECT .. WHERE $a > 5 AND b = 'abc' ", there have to be
  * `SqlWrite` typeclass instances for types of $a and $b.
  */
extension (sc: StringContext) {

  def sql(args: SqlInterpolatorArg*): Query =
    val stringPartsIter = sc.parts.iterator
    val argsIter = args.iterator
    var sb = new StringBuilder(stringPartsIter.next())
    val allArgs = ListBuffer.empty[SqlArgument[?]]
    while stringPartsIter.hasNext do {
      argsIter.next() match
        case simple: SqlArgument[?] =>
          sb.append("?")
          allArgs += simple
        case nestedQuery: Query =>
          sb.append(nestedQuery.sqlString)
          allArgs ++= nestedQuery.arguments
      sb.append(stringPartsIter.next())
    }

    Query(sb.toString, allArgs.toSeq)
}
