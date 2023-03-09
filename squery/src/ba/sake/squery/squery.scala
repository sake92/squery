package ba.sake.squery

import java.{sql => jsql}
import scala.util.Using

import ba.sake.squery.read.SqlRead
import ba.sake.squery.read.SqlReadRow

// simple INSERT/UPDATE/DELETE statements
def update[A](
    query: Query
)(using c: SqueryConnection): Int = {
  Using.resource(query.newPreparedStatement(c.underlying)) { stmt =>
    stmt.executeUpdate()
  }
}

// read single column (unnamed)
def readValues[A](
    query: Query
)(using c: SqueryConnection, r: SqlRead[A]): List[A] = {
  val elems = collection.mutable.ListBuffer.empty[A]
  Using.resource(query.newPreparedStatement(c.underlying)) { stmt =>
    Using.resource(stmt.executeQuery()) { res =>
      while (res.next()) {
        elems += r.readByIdx(res, 1)
      }
      elems.result()
    }
  }
}

// read case class (named columns)
def readRows[A](
    query: Query
)(using c: SqueryConnection, r: SqlReadRow[A]): List[A] = {
  val elems = collection.mutable.ListBuffer.empty[A]
  Using.resource(query.newPreparedStatement(c.underlying)) { stmt =>
    Using.resource(stmt.executeQuery()) { res =>
      while (res.next()) {
        elems += r.readRow(res, None)
      }
      elems.result()
    }
  }
}
