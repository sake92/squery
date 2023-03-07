package ba.sake.squery.read

import java.{sql => jsql}
import scala.util.Using

import ba.sake.squery.*

def values[A](query: Query)(using c: Connection, r: SqlRead[A]): List[A] = {
  val elems = collection.mutable.ListBuffer.empty[A]
  Using.resource(Query.newPreparedStatement(query, c.underlying)) { stmt =>
    Using.resource(stmt.executeQuery()) { res =>
      while (res.next()) {
        elems += r.readByIdx(res, 1)
      }
      elems.result()
    }
  }
}

def rows[A](query: Query)(using c: Connection, r: SqlReadRow[A]): List[A] = {
  val elems = collection.mutable.ListBuffer.empty[A]
  Using.resource(Query.newPreparedStatement(query, c.underlying)) { stmt =>
    Using.resource(stmt.executeQuery()) { res =>
      while (res.next()) {
        elems += r.readRow(res, None)
      }
      elems.result()
    }
  }
}
