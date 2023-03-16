package ba.sake.squery

import java.{sql => jsql}
import scala.util.Using

import ba.sake.squery.read.SqlRead
import ba.sake.squery.read.SqlReadRow

// ext methods coz overloadinggggggggg

extension (query: Query) {

  // simple INSERT/UPDATE/DELETE statements
  def update()(using c: SqueryConnection): Int =
    Using.resource(query.newPreparedStatement(c.underlying)) { stmt =>
      stmt.executeUpdate()
    }

  /* INSERT */
  // QOL insert function
  def insert()(using c: SqueryConnection): Unit =
    val affected = query.update()
    if affected == 0 then throw SqueryException("Insert failed")

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
  def insertReturningValues[A]()(using
      c: SqueryConnection,
      r: SqlRead[A]
  ): List[A] =
    Using.resource(query.newPreparedStatement(c.underlying)) { stmt =>
      stmt.executeUpdate()
      val keysRes = stmt.getGeneratedKeys()
      val elems = collection.mutable.ListBuffer.empty[A]
      while (keysRes.next()) {
        elems += r.readByIdx(keysRes, 1).get // TODO
      }
      elems.result()
    }

  def insertReturningRows[A]()(using
      c: SqueryConnection,
      r: SqlReadRow[A]
  ): List[A] =
    Using.resource(query.newPreparedStatement(c.underlying)) { stmt =>
      stmt.executeUpdate()
      val keysRes = stmt.getGeneratedKeys()
      val elems = collection.mutable.ListBuffer.empty[A]
      while (keysRes.next()) {
        elems += r.readRow(keysRes, None)
      }
      elems.result()
    }

  // TODO same for UPDATE..
  def insertReturningRow[A]()(using c: SqueryConnection, r: SqlReadRow[A]): A =
    Using.resource(query.newPreparedStatement(c.underlying)) { stmt =>
      stmt.executeUpdate()
      val keysRes = stmt.getGeneratedKeys()
      val elems = collection.mutable.ListBuffer.empty[A]
      while (keysRes.next()) {
        elems += r.readRow(keysRes, None)
      }
      elems.result.head // TODO
    }

  /* SELECT */
  // read single column (unnamed)
  def readValues[A]()(using c: SqueryConnection, r: SqlRead[A]): List[A] =
    val elems = collection.mutable.ListBuffer.empty[A]
    Using.resource(query.newPreparedStatement(c.underlying)) { stmt =>
      Using.resource(stmt.executeQuery()) { res =>
        while (res.next()) {
          elems += r.readByIdx(res, 1).get // TODO
        }
        elems.result()
      }
    }

  def readValueOpt[A]()(using c: SqueryConnection, r: SqlRead[A]): Option[A] =
    query.readValues().headOption

  def readValue[A]()(using c: SqueryConnection, r: SqlRead[A]): A =
    query.readValues().head // TODO

  // read case class (named columns)
  def readRows[A]()(using c: SqueryConnection, r: SqlReadRow[A]): List[A] =
    val elems = collection.mutable.ListBuffer.empty[A]
    Using.resource(query.newPreparedStatement(c.underlying)) { stmt =>
      Using.resource(stmt.executeQuery()) { res =>
        while (res.next()) {
          elems += r.readRow(res, None)
        }
        elems.result()
      }
    }

  def readRowOpt[A]()(using c: SqueryConnection, r: SqlReadRow[A]): Option[A] =
    query.readRows().headOption

  def readRow[A]()(using c: SqueryConnection, r: SqlReadRow[A]): A =
    query.readRows().head // TODO

}
