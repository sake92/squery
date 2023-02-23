package ba.sake.squery

import java.{sql => jsql}
import scala.util.Using

@annotation.implicitNotFound(
  "No database connection found. Make sure to call this in a `run{ }` or `transaction{ }` block."
)
case class Connection(underlying: jsql.Connection)

object run {

  def apply[A](ds: javax.sql.DataSource)(fn: Connection ?=> A): A =
    Using.resource(ds.getConnection()) { conn =>
      conn.setAutoCommit(true)
      fn(using Connection(conn))
    }

  object transaction {

    def apply[A](ds: javax.sql.DataSource)(fn: Connection ?=> A): A =
      Using.resource(ds.getConnection()) { conn =>
        conn.setAutoCommit(false)
        val res = fn(using Connection(conn))
        conn.commit()
        res
      }

  }
}

