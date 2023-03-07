package ba.sake.squery.write

import java.{sql => jsql}

trait SqlWrite[T]:
  def write(ps: jsql.PreparedStatement, idx: Int, value: T): Unit

object SqlWrite:
  def apply[T](using sqlWrite: SqlWrite[T]): SqlWrite[T] = sqlWrite

  given SqlWrite[String] = new {
    def write(ps: jsql.PreparedStatement, idx: Int, value: String): Unit =
      ps.setString(idx, value)
  }

  given SqlWrite[Int] = new {
    def write(ps: jsql.PreparedStatement, idx: Int, value: Int): Unit =
      ps.setInt(idx, value)
  }
