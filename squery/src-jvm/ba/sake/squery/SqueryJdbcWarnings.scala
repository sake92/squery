package ba.sake.squery

import java.{sql => jsql}

private[squery] object SqueryJdbcWarningsPlatform {
  def log(statement: jsql.Statement, logger: SqueryLogger): Unit = {
    var warning = statement.getWarnings
    while warning != null do
      logger.warn(warning.getMessage)
      warning = warning.getNextWarning
  }
}
