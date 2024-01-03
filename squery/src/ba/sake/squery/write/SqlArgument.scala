package ba.sake.squery.write

import ba.sake.squery.Query

case class SqlArgument[T](
    value: T
)(using val sqlWrite: SqlWrite[T])

object SqlArgument {

  given writeable2sqlArgument[T: SqlWrite]: Conversion[T, SqlArgument[T]] with {
    def apply(value: T): SqlArgument[T] = new SqlArgument(value)
  }
}
