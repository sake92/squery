package ba.sake.squery

import java.{sql => jsql}
import scala.util.Using

import ba.sake.squery.read.SqlRead
import ba.sake.squery.read.SqlReadRow

// simple INSERT/UPDATE/DELETE statements
def update(
    query: Query
)(using c: SqueryConnection): Int = {
  Using.resource(query.newPreparedStatement(c.underlying)) { stmt =>
    stmt.executeUpdate()
  }
}

/* INSERT */
// QOL insert function
def insert(
    query: Query
)(using c: SqueryConnection): Unit =
  update(query)

/** Inserts values and returns generated keys.
  *
  * @param query
  *   SQL query to execute
  * @param c
  *   implicit Squery connection
  * @param r
  *   implicit Sql read for the return type (generated key types)
  * @return
  *   generated keys
  */
def insertReturningKeys[A](
    query: Query
)(using c: SqueryConnection, r: SqlRead[A]): List[A] = {
  Using.resource(query.newPreparedStatement(c.underlying)) { stmt =>
    stmt.executeUpdate()
    val keysRes = stmt.getGeneratedKeys()
    val elems = collection.mutable.ListBuffer.empty[A]
    while (keysRes.next()) {
      elems += r.readByIdx(keysRes, 1)
    }
    elems.result()
  }
}

/* SELECT */
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
