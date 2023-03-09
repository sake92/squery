package ba.sake.squery

import javax.sql.DataSource
import scala.util.Using

class SqueryContext(ds: DataSource) {

  def run[A](fn: SqueryConnection ?=> A): A =
    Using.resource(ds.getConnection()) { conn =>
      conn.setAutoCommit(true)
      fn(using SqueryConnection(conn))
    }

  def runTransaction[A](fn: SqueryConnection ?=> A): A =
    Using.resource(ds.getConnection()) { conn =>
      conn.setAutoCommit(false)
      val res = fn(using SqueryConnection(conn))
      conn.commit()
      res
    }

}
