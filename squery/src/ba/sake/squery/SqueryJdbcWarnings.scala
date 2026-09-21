package ba.sake.squery

import java.{sql => jsql}

private[squery] object SqueryJdbcWarnings {
  def log(statement: jsql.Statement, logger: SqueryLogger): Unit =
    SqueryJdbcWarningsPlatform.log(statement, logger)
}
