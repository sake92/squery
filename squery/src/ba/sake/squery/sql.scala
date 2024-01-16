package ba.sake.squery

import scala.compiletime.*
import scala.compiletime.ops.any.*
import scala.collection.mutable.ListBuffer
import ba.sake.squery.write.*
import ba.sake.squery.Query

case class LiteralString(value: String)

case class DynamicArg[T](value: T)(using val sqlWrite: SqlWrite[T])

type LiteralOrDynamicString = LiteralString | DynamicArg[String]

// strings are treated specially:
// - literal-type strings are just passed through
// - dynamic strings are interpolated with ?, of course
given string2LiteralString[T <: Singleton & String]: Conversion[T, LiteralOrDynamicString] with
  transparent inline def apply(value: T): LiteralOrDynamicString =
    inline constValue[IsConst[T]] match
      case true  => LiteralString(value)
      case false => DynamicArg(value)(using SqlWrite[String])

given sqlWrite2DynamicArg[T: SqlWrite]: Conversion[T, DynamicArg[T]] with
  def apply(value: T): DynamicArg[T] =
    DynamicArg(value)

/*
arg can be:
- a literal string https://scala-slick.org/doc/3.2.0/sql.html#splicing-literal-values
- a simple value
- or another query
 */
type SqlInterpolatorArgOrQuery = LiteralString | DynamicArg[?] | Query

/** Implementation of `sql""` interpolator. For a query sql"SELECT .. WHERE $a > 5 AND b = 'abc' ", there have to be
  * `SqlWrite` typeclass instances for types of $a and $b.
  */
extension (sc: StringContext) {

  def sql(args: SqlInterpolatorArgOrQuery*): Query =
    val stringPartsIter = sc.parts.iterator
    val argsIter = args.iterator
    val sb = StringBuilder(stringPartsIter.next())
    val allArgs = ListBuffer.empty[DynamicArg[?]]

    while stringPartsIter.hasNext do {
      argsIter.next() match
        case literalString: LiteralString =>
          sb.append(literalString.value)
        case sqlArg: DynamicArg[?] =>
          sb.append("?")
          allArgs += sqlArg
        case nestedQuery: Query =>
          sb.append(nestedQuery.sqlString)
          allArgs ++= nestedQuery.arguments
      sb.append(stringPartsIter.next())
    }

    Query(sb.toString, allArgs.toSeq)
}
