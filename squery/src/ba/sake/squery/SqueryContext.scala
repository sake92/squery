package ba.sake.squery

import javax.sql.DataSource
import scala.util.Using

class SqueryContext(ds: DataSource) {

  def run[A](dbAction: DbAction[A]): A =
    Using.resource(ds.getConnection()) { conn =>
      conn.setAutoCommit(true)
      dbAction(using SqueryConnection(conn))
    }

  def runTransaction[A](level: TransactionIsolation = TransactionIsolation.ReadCommited)(
      dbAction: DbAction[A]
  ): A =
    Using.resource(ds.getConnection()) { conn =>
      conn.setAutoCommit(false)
      conn.setTransactionIsolation(level.jdbcLevel)
      val res = dbAction(using SqueryConnection(conn))
      conn.commit()
      res
    }

}
