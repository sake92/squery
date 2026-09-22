package ba.sake.squery

import java.{sql => jsql}
import scala.util.Using

import ba.sake.squery.read.SqlRead
import ba.sake.squery.read.SqlReadRow

type SqlRead[T] = ba.sake.squery.read.SqlRead[T]
val SqlRead = ba.sake.squery.read.SqlRead

type SqlReadRow[T] = ba.sake.squery.read.SqlReadRow[T]
val SqlReadRow = ba.sake.squery.read.SqlReadRow

type SqlWrite[T] = ba.sake.squery.write.SqlWrite[T]
val SqlWrite = ba.sake.squery.write.SqlWrite

type DbAction[T] = SqueryConnection ?=> T

enum DbActionType:
  case Select, Update, General

// ext methods coz overloadinggggggggg
extension (query: Query) {

  // for generic statements like creating stored procedures
  def execute()(using c: SqueryConnection): Unit =
    Using.resource(query.newPreparedStatement(DbActionType.General, c)) { stmt =>
      stmt.execute()
    }

  // simple INSERT/UPDATE/DELETE statements
  def update()(using c: SqueryConnection): Int =
    Using.resource(query.newPreparedStatement(DbActionType.Update, c)) { stmt =>
      stmt.executeUpdate()
    }

  /* INSERT */
  // same as update..
  def insert()(using c: SqueryConnection): Int =
    update()

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
  def insertReturningGenKeys[A](colNames: Seq[String] = Seq.empty)(using
      c: SqueryConnection,
      r: SqlRead[A]
  ): Seq[A] =
    val queryWithGeneratedKeys =
      if colNames.nonEmpty then query.withGeneratedKeys(GeneratedKeys.ColumnNames(colNames))
      else if query.statementOptions.generatedKeys.nonEmpty then query
      else query.withGeneratedKeys()
    Using.resource(
      queryWithGeneratedKeys.newPreparedStatement(DbActionType.Update, c)
    ) { stmt =>
      stmt.executeUpdate()
      val keysRes = stmt.getGeneratedKeys
      val elems = collection.mutable.ListBuffer.empty[A]
      while (keysRes.next()) {
        elems += r
          .readByIdx(keysRes, 1)
          .getOrElse(throw SqueryException("No value returned from query"))
      }
      elems.result()
    }

  def insertReturningGenKeyOpt[A](colName: Option[String] = None)(using
      c: SqueryConnection,
      r: SqlRead[A]
  ): Option[A] =
    insertReturningGenKeys(colName.toSeq).headOption

  def insertReturningGenKey[A](colName: Option[String] = None)(using
      c: SqueryConnection,
      r: SqlRead[A]
  ): A =
    insertReturningGenKeyOpt(colName).getOrElse(
      throw SqueryException("No value returned from query")
    )

  def insertReturningRows[A]()(using
      c: SqueryConnection,
      r: SqlReadRow[A]
  ): Seq[A] =
    Using.resource(query.newPreparedStatement(DbActionType.Update, c)) { stmt =>
      Using.resource(stmt.executeQuery()) { res =>
        val elems = collection.mutable.ListBuffer.empty[A]
        while (res.next()) {
          elems += r
            .readRow(res, None)
            .getOrElse(
              throw SqueryException("No value returned from query")
            )
        }
        elems.result()
      }
    }

  def insertReturningRow[A]()(using c: SqueryConnection, r: SqlReadRow[A]): A =
    insertReturningRows().headOption.getOrElse(
      throw SqueryException("No value returned from query")
    )

  /** Updates rows and returns their values from a `RETURNING` clause. */
  def updateReturningRows[A]()(using c: SqueryConnection, r: SqlReadRow[A]): Seq[A] =
    insertReturningRows()

  def updateReturningRow[A]()(using c: SqueryConnection, r: SqlReadRow[A]): A =
    updateReturningRows().headOption.getOrElse(
      throw SqueryException("No value returned from query")
    )

  /** Deletes rows and returns their values from a `RETURNING` clause. */
  def deleteReturningRows[A]()(using c: SqueryConnection, r: SqlReadRow[A]): Seq[A] =
    insertReturningRows()

  def deleteReturningRow[A]()(using c: SqueryConnection, r: SqlReadRow[A]): A =
    deleteReturningRows().headOption.getOrElse(
      throw SqueryException("No value returned from query")
    )

  /* SELECT */
  // read single column (unnamed)
  def readValues[A]()(using c: SqueryConnection, r: SqlRead[A]): Seq[A] =
    val elems = collection.mutable.ListBuffer.empty[A]
    Using.resource(query.newPreparedStatement(DbActionType.Select, c)) { stmt =>
      Using.resource(stmt.executeQuery()) { res =>
        while (res.next()) {
          elems += r
            .readByIdx(res, 1)
            .getOrElse(throw SqueryException("No values returned from query"))
        }
        elems.result()
      }
    }

  def readValueOpt[A]()(using c: SqueryConnection, r: SqlRead[A]): Option[A] =
    query.readValues().headOption

  def readValue[A]()(using c: SqueryConnection, r: SqlRead[A]): A =
    readValueOpt().getOrElse(
      throw SqueryException("No value returned from query")
    )

  // read case class (named columns)
  def readRows[A]()(using c: SqueryConnection, r: SqlReadRow[A]): Seq[A] =
    val elems = collection.mutable.ListBuffer.empty[A]
    Using.resource(query.newPreparedStatement(DbActionType.Select, c)) { stmt =>
      Using.resource(stmt.executeQuery()) { res =>
        while (res.next()) {
          r.readRow(res, None).foreach(elems += _)
        }
        elems.result()
      }
    }

  def readRowOpt[A]()(using c: SqueryConnection, r: SqlReadRow[A]): Option[A] =
    query.readRows().headOption

  def readRow[A]()(using c: SqueryConnection, r: SqlReadRow[A]): A =
    readRowOpt().getOrElse(
      throw SqueryException("No value returned from query")
    )

}

extension (queries: IterableOnce[Query]) {

  /** Executes queries with the same SQL shape as a JDBC prepared statement batch, falling back to sequential
    * prepared-statement updates when the driver does not implement batching.
    */
  def batchUpdate()(using c: SqueryConnection): Seq[Int] =
    val batch = queries.iterator.toSeq
    batch.headOption match
      case None => Seq.empty
      case Some(first) =>
        val expectedSql = first.sqlString
        val expectedArgumentCount = first.arguments.size
        batch.zipWithIndex.foreach { (query, index) =>
          if query.sqlString != expectedSql || query.arguments.size != expectedArgumentCount then
            throw SqueryException(
              s"All queries in batchUpdate must have the same SQL and argument count; query at index $index differs"
            )
        }

        Using.resource(first.newPreparedStatement(DbActionType.Update, c)) { stmt =>
          try
            stmt.addBatch()
            batch.tail.foreach { query =>
              query.bindArguments(stmt)
              stmt.addBatch()
            }
            stmt.executeBatch().toSeq
          catch
            case _: NotImplementedError =>
              batch.map { query =>
                query.bindArguments(stmt)
                stmt.executeUpdate()
              }
        }
}
