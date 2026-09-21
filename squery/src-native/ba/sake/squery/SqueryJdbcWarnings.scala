package ba.sake.squery

import java.{sql => jsql}

private[squery] object SqueryJdbcWarningsPlatform {
  def log(statement: jsql.Statement, logger: SqueryLogger): Unit = ()
}
