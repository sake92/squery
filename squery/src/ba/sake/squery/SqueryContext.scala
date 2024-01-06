package ba.sake.squery

import javax.sql.DataSource
import scala.util.Using

final class SqueryContext(ds: DataSource) {

  def run[T](dbAction: SqueryConnection ?=> T): T =
    Using.resource(ds.getConnection()) { conn =>
      conn.setAutoCommit(true)
      dbAction(using SqueryConnection(conn))
    }

  // default db isolation level..
  def runTransaction[T](dbAction: SqueryConnection ?=> T): T =
    Using.resource(ds.getConnection()) { conn =>
      conn.setAutoCommit(false)
      val res = dbAction(using SqueryConnection(conn))
      conn.commit()
      res
    }

  def runTransactionWithIsolation[T](level: TransactionIsolation)(dbAction: SqueryConnection ?=> T): T =
    Using.resource(ds.getConnection()) { conn =>
      conn.setAutoCommit(false)
      conn.setTransactionIsolation(level.jdbcLevel)
      val res = dbAction(using SqueryConnection(conn))
      conn.commit()
      res
    }

}
