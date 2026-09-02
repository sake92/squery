package ba.sake.squery

import javax.sql.DataSource
import scala.util.Using
import scala.util.control.NonFatal

final class SqueryContext(ds: DataSource, lintUpdates: Boolean = false) {

  def run[T](dbAction: SqueryConnection ?=> T): T =
    Using.resource(ds.getConnection()) { conn =>
      conn.setAutoCommit(true)
      dbAction(using SqueryConnection(conn, lintUpdates))
    }

  // default db isolation level..
  def runTransaction[T](dbAction: SqueryConnection ?=> T): T =
    Using.resource(ds.getConnection()) { conn =>
      conn.setAutoCommit(false)
      try {
        val res = dbAction(using SqueryConnection(conn, lintUpdates))
        conn.commit()
        res
      } catch {
        case NonFatal(error) =>
          conn.rollback()
          throw error
      }
    }

  def runTransactionWithIsolation[T](level: TransactionIsolation)(dbAction: SqueryConnection ?=> T): T =
    Using.resource(ds.getConnection()) { conn =>
      conn.setAutoCommit(false)
      conn.setTransactionIsolation(level.jdbcLevel)
      try {
        val res = dbAction(using SqueryConnection(conn, lintUpdates))
        conn.commit()
        res
      } catch {
        case NonFatal(error) =>
          conn.rollback()
          throw error
      }
    }

}
