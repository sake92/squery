package ba.sake.squery

import java.{sql => jsql}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

final case class StatementOptions(
    fetchSize: Option[Int] = None,
    timeout: Option[FiniteDuration] = None,
    maxRows: Option[Int] = None,
    resultSetType: Option[ResultSetType] = None,
    resultSetConcurrency: Option[ResultSetConcurrency] = None,
    generatedKeys: Option[GeneratedKeys] = None
) {
  require(fetchSize.forall(_ >= 0), "fetch size must not be negative")
  require(timeout.forall(_.length >= 0), "timeout must not be negative")
  require(
    timeout.forall(_ <= FiniteDuration(Int.MaxValue, TimeUnit.SECONDS)),
    "timeout is too large for JDBC"
  )
  require(maxRows.forall(_ >= 0), "maximum rows must not be negative")
  require(
    resultSetType.isDefined == resultSetConcurrency.isDefined,
    "result-set type and concurrency must be configured together"
  )
  require(
    generatedKeys.isEmpty || resultSetType.isEmpty,
    "generated keys cannot be combined with result-set type and concurrency"
  )
  require(
    generatedKeys.forall {
      case GeneratedKeys.All                    => true
      case GeneratedKeys.ColumnNames(names)     => names.nonEmpty && names.forall(_.nonEmpty)
      case GeneratedKeys.ColumnIndexes(indexes) => indexes.nonEmpty && indexes.forall(_ > 0)
    },
    "generated-key columns must be non-empty, and indexes must be positive"
  )
}

enum ResultSetType(private[squery] val jdbcValue: Int):
  case ForwardOnly extends ResultSetType(jsql.ResultSet.TYPE_FORWARD_ONLY)
  case ScrollInsensitive extends ResultSetType(jsql.ResultSet.TYPE_SCROLL_INSENSITIVE)
  case ScrollSensitive extends ResultSetType(jsql.ResultSet.TYPE_SCROLL_SENSITIVE)

enum ResultSetConcurrency(private[squery] val jdbcValue: Int):
  case ReadOnly extends ResultSetConcurrency(jsql.ResultSet.CONCUR_READ_ONLY)
  case Updatable extends ResultSetConcurrency(jsql.ResultSet.CONCUR_UPDATABLE)

enum GeneratedKeys:
  case All
  case ColumnNames(names: Seq[String])
  case ColumnIndexes(indexes: Seq[Int])
